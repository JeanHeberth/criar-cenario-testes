package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.*;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaContextSnapshotMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.*;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ponto único de controle do ciclo de vida de uma execução Auto QA:
 * criação, blocos (start/generate/apply/execute), aprovações, cancelamento,
 * lock de concorrência e optimistic locking. Nunca refatora nem instancia
 * AutoQaWorkflowService — delega a execução real dos agentes a
 * AutoQaStageExecutor. Nunca decide sozinho: transições vêm de
 * AutoQaTransitionValidator, ações disponíveis de
 * AutoQaAvailableActionResolver.
 */
@Service
public class AutoQaExecutionOrchestrator {

    private static final int TOTAL_STAGES = AutoQaStage.values().length;

    private final AutoQaExecutionRepository executionRepository;
    private final AutoQaExecutionSnapshotRepository snapshotRepository;
    private final AutoQaContextSnapshotMapper snapshotMapper;
    private final AutoQaStageExecutor stageExecutor;
    private final AutoQaTransitionValidator transitionValidator;
    private final AutoQaAvailableActionResolver actionResolver;
    private final AutoQaProperties properties;
    private final ProjectPathSecurityValidator projectPathSecurityValidator;

    public AutoQaExecutionOrchestrator(AutoQaExecutionRepository executionRepository,
                                        AutoQaExecutionSnapshotRepository snapshotRepository,
                                        AutoQaContextSnapshotMapper snapshotMapper,
                                        AutoQaStageExecutor stageExecutor,
                                        AutoQaTransitionValidator transitionValidator,
                                        AutoQaAvailableActionResolver actionResolver,
                                        AutoQaProperties properties,
                                        ProjectPathSecurityValidator projectPathSecurityValidator) {
        this.executionRepository = Objects.requireNonNull(executionRepository);
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.snapshotMapper = Objects.requireNonNull(snapshotMapper);
        this.stageExecutor = Objects.requireNonNull(stageExecutor);
        this.transitionValidator = Objects.requireNonNull(transitionValidator);
        this.actionResolver = Objects.requireNonNull(actionResolver);
        this.properties = Objects.requireNonNull(properties);
        this.projectPathSecurityValidator = Objects.requireNonNull(projectPathSecurityValidator);
    }

    public AutoQaExecutionDocument create(String scenario, String projectPath) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        if (!properties.isEnabled()) {
            throw new AutoQaExecutionDisabledException("Auto QA está desabilitado (auto-qa.enabled=false)");
        }
        // Rejeita cedo (antes de persistir) um projectPath fora de auto-qa.allowed-roots,
        // delegando à mesma política central usada de forma autoritativa em
        // ProjectDiscoveryService — evita criar uma execução fadada a falhar só no START.
        projectPathSecurityValidator.validate(Path.of(projectPath));

        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, scenario, projectPath, now);
        document.setAvailableActions(actionResolver.resolve(document, properties));
        document = saveWithLockHandling(document);

        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(executionId, now);
        saveSnapshotWithLockHandling(snapshot);
        return document;
    }

    public AutoQaExecutionDocument start(UUID executionId) {
        return runBlock(executionId, transitionValidator::validateStart,
                List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING),
                AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
    }

    public AutoQaExecutionDocument continueExecution(UUID executionId) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateContinue(document);
        List<AutoQaStage> block = blockOf(document.getLastStageStarted());
        return runBlockInternal(document, block, successStatusFor(block));
    }

    public AutoQaExecutionDocument generate(UUID executionId) {
        return runBlock(executionId, transitionValidator::validateGenerate,
                List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW), AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
    }

    public AutoQaExecutionDocument apply(UUID executionId) {
        requireSensitiveActionEnabled(properties.isAllowFileApplication(),
                "Aplicação de arquivo desabilitada por configuração (allow-file-application/sensitive-actions-enabled)");
        return runBlock(executionId, transitionValidator::validateApply,
                List.of(AutoQaStage.APPLY), AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
    }

    public AutoQaExecutionDocument execute(UUID executionId) {
        requireSensitiveActionEnabled(properties.isAllowCommandExecution(),
                "Execução de comando desabilitada por configuração (allow-command-execution/sensitive-actions-enabled)");
        return runBlock(executionId, transitionValidator::validateExecute,
                List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING), AutoQaWorkflowStatus.COMPLETED);
    }

    public AutoQaExecutionDocument registerApplyApproval(UUID executionId, ApplyApproval approval) {
        Objects.requireNonNull(approval, "approval must not be null");
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateRegisterApplyApproval(document);

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        snapshot.setApplyApproval(approval);
        snapshot.setUpdatedAt(Instant.now());
        saveSnapshotWithLockHandling(snapshot);

        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", approval.approvedBy(), toInstant(approval.approvedAt()), approval.approved()));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    public AutoQaExecutionDocument registerExecutionApproval(UUID executionId, ExecutionApproval approval) {
        Objects.requireNonNull(approval, "approval must not be null");
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateRegisterExecutionApproval(document);

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        snapshot.setExecutionApproval(approval);
        snapshot.setUpdatedAt(Instant.now());
        saveSnapshotWithLockHandling(snapshot);

        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", approval.approvedBy(), toInstant(approval.approvedAt()), approval.approved()));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    public AutoQaExecutionDocument cancel(UUID executionId, String reason) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateCancel(document);

        document.setWorkflowStatus(AutoQaWorkflowStatus.CANCELLED);
        document.setCancelledAt(Instant.now());
        document.setCancellationReason(reason);
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    // --- internals ---

    private AutoQaExecutionDocument runBlock(UUID executionId, Consumer<AutoQaExecutionDocument> validate,
                                              List<AutoQaStage> block, AutoQaWorkflowStatus targetStatus) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        validate.accept(document);
        return runBlockInternal(document, block, targetStatus);
    }

    private AutoQaExecutionDocument runBlockInternal(AutoQaExecutionDocument document, List<AutoQaStage> block,
                                                       AutoQaWorkflowStatus targetStatus) {
        UUID executionId = document.getExecutionId();

        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        document.setAttempt(document.getAttempt() + 1);
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        document = saveWithLockHandling(document);
        if (document.getStartedAt() == null) {
            document.setStartedAt(Instant.now());
        }

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        AutoQaContext context = snapshotMapper.toContext(snapshot, document.getScenarioSummary(), document.getProjectPath());

        AutoQaStageExecutor.AutoQaStageExecutionResult result;
        try {
            result = stageExecutor.executeStages(context, block);
        } catch (RuntimeException ex) {
            return finalizeFailure(document, null, null, "TECHNICAL_FAILURE", exceptionMessage(ex));
        }

        if (!result.success()) {
            return finalizeFailure(document, result.lastStageStarted(), result.lastStageCompleted(), "STAGE_FAILURE", result.message());
        }

        document.setLastStageStarted(result.lastStageStarted());
        document.setLastStageCompleted(result.lastStageCompleted());
        document.setCurrentStage(result.lastStageCompleted());

        AutoQaStage snapshotBoundary = boundaryStageOf(block);
        if (snapshotBoundary != null) {
            snapshotMapper.toSnapshot(context, snapshot, snapshotBoundary, Instant.now());
            saveSnapshotWithLockHandling(snapshot);
        }

        document.setWorkflowStatus(targetStatus);
        document.setOperationStatus(AutoQaOperationStatus.SUCCEEDED);
        document.setProgress(computeProgress(document.getLastStageCompleted()));
        if (targetStatus == AutoQaWorkflowStatus.COMPLETED) {
            document.setFinishedAt(Instant.now());
        }
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    private AutoQaExecutionDocument finalizeFailure(AutoQaExecutionDocument document, AutoQaStage lastStageStarted,
                                                      AutoQaStage lastStageCompleted, String code, String message) {
        if (lastStageStarted != null) {
            document.setLastStageStarted(lastStageStarted);
        }
        if (lastStageCompleted != null) {
            document.setLastStageCompleted(lastStageCompleted);
            document.setCurrentStage(lastStageCompleted);
        }
        document.setWorkflowStatus(AutoQaWorkflowStatus.FAILED);
        document.setOperationStatus(AutoQaOperationStatus.FAILED);
        document.getErrors().add(new AutoQaErrorRecord(code, message == null ? "erro desconhecido" : message));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    /**
     * O snapshot só avança até APPLY — o bloco final (Execution, FailureAnalysis,
     * Learning) nunca é snapshotado, pois sempre roda como uma unidade atômica:
     * se o processo cair no meio dele, a retomada (continueExecution) sempre
     * reidrata do snapshot pós-Apply e reexecuta o bloco final inteiro — nunca
     * repete ApplyAgent.
     */
    private AutoQaStage boundaryStageOf(List<AutoQaStage> block) {
        if (block.contains(AutoQaStage.EXECUTION)) {
            return null;
        }
        return block.isEmpty() ? null : block.get(block.size() - 1);
    }

    private List<AutoQaStage> blockOf(AutoQaStage stage) {
        if (stage == null) {
            throw new AutoQaInvalidTransitionException("Nenhuma etapa foi iniciada anteriormente para retomar");
        }
        return switch (stage) {
            case DISCOVERY, SCENARIO_ANALYSIS, PROJECT_KNOWLEDGE, PLANNING -> List.of(
                    AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING);
            case GENERATION, REVIEW -> List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW);
            case APPLY -> List.of(AutoQaStage.APPLY);
            case EXECUTION, FAILURE_ANALYSIS, LEARNING -> List.of(
                    AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING);
        };
    }

    private AutoQaWorkflowStatus successStatusFor(List<AutoQaStage> block) {
        if (block.contains(AutoQaStage.PLANNING)) {
            return AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL;
        }
        if (block.contains(AutoQaStage.REVIEW)) {
            return AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL;
        }
        if (block.contains(AutoQaStage.APPLY)) {
            return AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL;
        }
        return AutoQaWorkflowStatus.COMPLETED;
    }

    private void requireSensitiveActionEnabled(boolean specificFlag, String message) {
        if (!specificFlag || !properties.isSensitiveActionsEnabled()) {
            throw new AutoQaSensitiveActionDisabledException(message);
        }
    }

    private AutoQaExecutionDocument loadDocument(UUID executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        return executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new AutoQaExecutionNotFoundException("Execução não encontrada: " + executionId));
    }

    private AutoQaExecutionSnapshot loadSnapshot(UUID executionId) {
        return snapshotRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new AutoQaExecutionNotFoundException("Snapshot não encontrado: " + executionId));
    }

    private AutoQaExecutionDocument saveWithLockHandling(AutoQaExecutionDocument document) {
        try {
            return executionRepository.save(document);
        } catch (OptimisticLockingFailureException ex) {
            throw new AutoQaOptimisticLockException("Conflito de versão detectado ao salvar a execução", ex);
        }
    }

    private AutoQaExecutionSnapshot saveSnapshotWithLockHandling(AutoQaExecutionSnapshot snapshot) {
        try {
            return snapshotRepository.save(snapshot);
        } catch (OptimisticLockingFailureException ex) {
            throw new AutoQaOptimisticLockException("Conflito de versão detectado ao salvar o snapshot", ex);
        }
    }

    private int computeProgress(AutoQaStage lastStageCompleted) {
        if (lastStageCompleted == null) {
            return 0;
        }
        return (int) Math.round((lastStageCompleted.ordinal() + 1) * 100.0 / TOTAL_STAGES);
    }

    private String exceptionMessage(RuntimeException ex) {
        String detail = ex.getMessage();
        return detail == null || detail.isBlank() ? ex.getClass().getSimpleName() : detail;
    }

    private Instant toInstant(java.time.LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
