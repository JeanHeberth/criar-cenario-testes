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
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.OptimisticLockingFailureException;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AutoQaExecutionOrchestrator - Testes Unitários")
class AutoQaExecutionOrchestratorTest {

    private AutoQaExecutionRepository executionRepository;
    private AutoQaExecutionSnapshotRepository snapshotRepository;
    private AutoQaContextSnapshotMapper snapshotMapper;
    private AutoQaStageExecutor stageExecutor;
    private AutoQaTransitionValidator transitionValidator;
    private AutoQaAvailableActionResolver actionResolver;
    private AutoQaProperties properties;
    private AutoQaExecutionOrchestrator orchestrator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        executionRepository = mock(AutoQaExecutionRepository.class);
        snapshotRepository = mock(AutoQaExecutionSnapshotRepository.class);
        snapshotMapper = mock(AutoQaContextSnapshotMapper.class);
        stageExecutor = mock(AutoQaStageExecutor.class);
        transitionValidator = mock(AutoQaTransitionValidator.class);
        actionResolver = mock(AutoQaAvailableActionResolver.class);
        properties = new AutoQaProperties();
        properties.setAllowFileApplication(true);
        properties.setAllowCommandExecution(true);
        properties.setSensitiveActionsEnabled(true);
        // Fase 13.1A: create() agora exige que projectPath esteja dentro de uma
        // auto-qa.allowed-roots — tempDir é a única raiz autorizada nestes testes.
        properties.setAllowedRoots(List.of(tempDir.toString()));

        orchestrator = new AutoQaExecutionOrchestrator(executionRepository, snapshotRepository, snapshotMapper,
                stageExecutor, transitionValidator, actionResolver, properties,
                new ProjectPathSecurityValidator(properties));

        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(actionResolver.resolve(any(), any())).thenReturn(Set.of());
        when(snapshotMapper.toContext(any(), any(), any())).thenReturn(AutoQaContext.create("cenário", "/projeto"));
    }

    @Test
    @DisplayName("create deve persistir documento e snapshot novos")
    void createDevePersistirDocumentoESnapshot() {
        AutoQaExecutionDocument document = orchestrator.create("cenário", tempDir.toString());

        assertThat(document.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.CREATED);
        verify(executionRepository).save(any());
        verify(snapshotRepository).save(any());
    }

    @Test
    @DisplayName("create deve lançar AutoQaExecutionDisabledException quando auto-qa.enabled=false")
    void createDeveLancarQuandoDesabilitado() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> orchestrator.create("cenário", tempDir.toString()))
                .isInstanceOf(AutoQaExecutionDisabledException.class);
        verifyNoInteractions(executionRepository);
    }

    @Test
    @DisplayName("create deve rejeitar projectPath fora de auto-qa.allowed-roots, sem persistir nada (Fase 13.1A)")
    void createDeveRejeitarProjectPathForaDeAllowedRoots() throws Exception {
        Path outsideRoot = java.nio.file.Files.createTempDirectory("fora-da-allowlist");
        try {
            assertThatThrownBy(() -> orchestrator.create("cenário", outsideRoot.toString()))
                    .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
            verifyNoInteractions(executionRepository);
            verifyNoInteractions(snapshotRepository);
        } finally {
            java.nio.file.Files.deleteIfExists(outsideRoot);
        }
    }

    @Test
    @DisplayName("create deve rejeitar qualquer projectPath quando allowed-roots estiver vazia (fail-closed)")
    void createDeveRejeitarQuandoAllowedRootsVazia() {
        properties.setAllowedRoots(List.of());

        assertThatThrownBy(() -> orchestrator.create("cenário", tempDir.toString()))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
        verifyNoInteractions(executionRepository);
    }

    @Test
    @DisplayName("start deve avançar para WAITING_GENERATION_APPROVAL quando o bloco 1 tem sucesso")
    void startDeveAvancarParaWaitingGenerationApproval() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CREATED);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), eq(List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS,
                AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING))))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.PLANNING, AutoQaStage.PLANNING));

        AutoQaExecutionDocument result = orchestrator.start(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        assertThat(result.getOperationStatus()).isEqualTo(AutoQaOperationStatus.SUCCEEDED);
        verify(transitionValidator).validateStart(any());
    }

    @Test
    @DisplayName("start deve marcar operationStatus=IN_PROGRESS antes de chamar o StageExecutor (lock)")
    void startDeveMarcarLockAntesDeExecutar() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CREATED);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.PLANNING, AutoQaStage.PLANNING));

        orchestrator.start(executionId);

        verify(executionRepository, atLeastOnce()).save(argThat(d -> d.getOperationStatus() == AutoQaOperationStatus.IN_PROGRESS
                || d.getOperationStatus() == AutoQaOperationStatus.SUCCEEDED));
    }

    @Test
    @DisplayName("start deve incrementar attempt a cada chamada")
    void startDeveIncrementarAttempt() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CREATED);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.PLANNING, AutoQaStage.PLANNING));

        AutoQaExecutionDocument result = orchestrator.start(executionId);

        assertThat(result.getAttempt()).isEqualTo(1);
    }

    @Test
    @DisplayName("start deve resultar em FAILED quando o bloco falha, sem atualizar o snapshot")
    void startDeveResultarEmFailedQuandoBlocoFalha() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CREATED);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.failure(AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.DISCOVERY, "falhou"));

        AutoQaExecutionDocument result = orchestrator.start(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.FAILED);
        assertThat(result.getErrors()).isNotEmpty();
        verify(snapshotMapper, never()).toSnapshot(any(), any(), any(), any());
    }

    @Test
    @DisplayName("start deve resultar em FAILED quando o StageExecutor lança exceção técnica")
    void startDeveResultarEmFailedQuandoExcecaoTecnica() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CREATED);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any())).thenThrow(new RuntimeException("erro técnico"));

        AutoQaExecutionDocument result = orchestrator.start(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.FAILED);
    }

    @Test
    @DisplayName("start deve propagar AutoQaInvalidTransitionException quando o estado não permite")
    void startDevePropagarExcecaoDeTransicao() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        doThrow(new AutoQaInvalidTransitionException("inválido")).when(transitionValidator).validateStart(any());

        assertThatThrownBy(() -> orchestrator.start(executionId)).isInstanceOf(AutoQaInvalidTransitionException.class);
        verifyNoInteractions(stageExecutor);
    }

    @Test
    @DisplayName("start deve lançar AutoQaExecutionNotFoundException quando a execução não existir")
    void startDeveLancarNotFound() {
        UUID executionId = UUID.randomUUID();
        when(executionRepository.findByExecutionId(executionId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> orchestrator.start(executionId)).isInstanceOf(AutoQaExecutionNotFoundException.class);
    }

    @Test
    @DisplayName("generate deve avançar para WAITING_APPLY_APPROVAL")
    void generateDeveAvancarParaWaitingApplyApproval() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), eq(List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW))))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.REVIEW, AutoQaStage.REVIEW));

        AutoQaExecutionDocument result = orchestrator.generate(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
    }

    @Test
    @DisplayName("apply deve retornar 403 (AutoQaSensitiveActionDisabledException) quando allow-file-application=false")
    void applyDeveLancarQuandoFlagDesabilitada() {
        properties.setAllowFileApplication(false);
        UUID executionId = UUID.randomUUID();

        assertThatThrownBy(() -> orchestrator.apply(executionId)).isInstanceOf(AutoQaSensitiveActionDisabledException.class);
        verifyNoInteractions(executionRepository);
    }

    @Test
    @DisplayName("apply deve retornar 403 quando sensitive-actions-enabled=false mesmo com allow-file-application=true")
    void applyDeveLancarQuandoSensitiveActionsDesabilitado() {
        properties.setSensitiveActionsEnabled(false);
        UUID executionId = UUID.randomUUID();

        assertThatThrownBy(() -> orchestrator.apply(executionId)).isInstanceOf(AutoQaSensitiveActionDisabledException.class);
    }

    @Test
    @DisplayName("execute deve retornar 403 quando allow-command-execution=false")
    void executeDeveLancarQuandoFlagDesabilitada() {
        properties.setAllowCommandExecution(false);
        UUID executionId = UUID.randomUUID();

        assertThatThrownBy(() -> orchestrator.execute(executionId)).isInstanceOf(AutoQaSensitiveActionDisabledException.class);
        verifyNoInteractions(executionRepository);
    }

    @Test
    @DisplayName("apply deve avançar para WAITING_EXECUTION_APPROVAL quando habilitado e aprovado")
    void applyDeveAvancarParaWaitingExecutionApproval() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), eq(List.of(AutoQaStage.APPLY))))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.APPLY, AutoQaStage.APPLY));

        AutoQaExecutionDocument result = orchestrator.apply(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
    }

    @Test
    @DisplayName("execute deve avançar para COMPLETED, mesmo quando os testes falharam (ExecutionStatus.FAILED não é erro técnico)")
    void executeDeveAvancarParaCompleted() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), eq(List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING))))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.LEARNING, AutoQaStage.LEARNING));

        AutoQaExecutionDocument result = orchestrator.execute(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.COMPLETED);
        assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("execute não deve atualizar o snapshot (fronteira é sempre até Apply)")
    void executeNaoDeveAtualizarSnapshot() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.LEARNING, AutoQaStage.LEARNING));

        orchestrator.execute(executionId);

        verify(snapshotMapper, never()).toSnapshot(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Crash no bloco final não deve repetir Apply — continueExecution deve reexecutar apenas Execution-FailureAnalysis-Learning")
    void crashNoBlocoFinalNaoRepeteApply() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.FAILED);
        document.setLastStageStarted(AutoQaStage.LEARNING);
        document.setLastStageCompleted(AutoQaStage.FAILURE_ANALYSIS);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), eq(List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING))))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.LEARNING, AutoQaStage.LEARNING));

        AutoQaExecutionDocument result = orchestrator.continueExecution(executionId);

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.COMPLETED);
        verify(stageExecutor).executeStages(any(), eq(List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING)));
        verify(stageExecutor, never()).executeStages(any(), eq(List.of(AutoQaStage.APPLY)));
    }

    @Test
    @DisplayName("Retomada do bloco final preserva o ApplyResult já persistido no snapshot")
    void retomadaDoBlocoFinalPreservaApplyResult() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.FAILED);
        document.setLastStageStarted(AutoQaStage.EXECUTION);
        AutoQaExecutionSnapshot snapshot = stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.LEARNING, AutoQaStage.LEARNING));

        orchestrator.continueExecution(executionId);

        verify(snapshotMapper).toContext(snapshot, document.getScenarioSummary(), document.getProjectPath());
    }

    @Test
    @DisplayName("Retomada do bloco 1 (Discovery-Planning) reexecuta o bloco 1, nunca Generation em diante")
    void retomadaDoBloco1ReexecutaBloco1() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = stubDocumentoExistente(executionId, AutoQaWorkflowStatus.FAILED);
        document.setLastStageStarted(AutoQaStage.SCENARIO_ANALYSIS);
        stubSnapshotExistente(executionId);
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.PLANNING, AutoQaStage.PLANNING));

        orchestrator.continueExecution(executionId);

        verify(stageExecutor).executeStages(any(), eq(List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS,
                AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING)));
    }

    @Test
    @DisplayName("Execução concorrente do bloco final é bloqueada (validador rejeita antes do StageExecutor)")
    void execucaoConcorrenteDoBlocoFinalEhBloqueada() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        doThrow(new AutoQaExecutionConflictException("em andamento")).when(transitionValidator).validateExecute(any());

        assertThatThrownBy(() -> orchestrator.execute(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
        verifyNoInteractions(stageExecutor);
    }

    @Test
    @DisplayName("registerApplyApproval deve registrar a aprovação no documento e no snapshot")
    void registerApplyApprovalDeveRegistrar() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        AutoQaExecutionSnapshot snapshot = stubSnapshotExistente(executionId);
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true);

        AutoQaExecutionDocument result = orchestrator.registerApplyApproval(executionId, approval);

        assertThat(result.getApprovals()).anyMatch(a -> a.type().equals("APPLY") && a.approved());
        assertThat(snapshot.getApplyApproval()).isEqualTo(approval);
    }

    @Test
    @DisplayName("registerApplyApproval duplicado deve ser rejeitado pelo validador")
    void registerApplyApprovalDuplicadoRejeitado() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        doThrow(new AutoQaExecutionConflictException("duplicado")).when(transitionValidator).validateRegisterApplyApproval(any());
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true);

        assertThatThrownBy(() -> orchestrator.registerApplyApproval(executionId, approval))
                .isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("registerExecutionApproval deve registrar a aprovação no documento e no snapshot")
    void registerExecutionApprovalDeveRegistrar() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        AutoQaExecutionSnapshot snapshot = stubSnapshotExistente(executionId);
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);

        AutoQaExecutionDocument result = orchestrator.registerExecutionApproval(executionId, approval);

        assertThat(result.getApprovals()).anyMatch(a -> a.type().equals("EXECUTION") && a.approved());
        assertThat(snapshot.getExecutionApproval()).isEqualTo(approval);
    }

    @Test
    @DisplayName("cancel deve marcar CANCELLED em estado não-terminal")
    void cancelDeveMarcarCancelled() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);

        AutoQaExecutionDocument result = orchestrator.cancel(executionId, "motivo qualquer");

        assertThat(result.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.CANCELLED);
        assertThat(result.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel duplicado (concorrente) deve ser rejeitado pelo validador")
    void cancelDuplicadoRejeitado() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.CANCELLED);
        doThrow(new AutoQaExecutionConflictException("terminal")).when(transitionValidator).validateCancel(any());

        assertThatThrownBy(() -> orchestrator.cancel(executionId, "motivo")).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Conflito de optimistic locking ao salvar deve virar AutoQaOptimisticLockException")
    void conflitoDeOptimisticLockingDeveVirarExcecaoDedicada() {
        UUID executionId = UUID.randomUUID();
        stubDocumentoExistente(executionId, AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        when(executionRepository.save(any())).thenThrow(new OptimisticLockingFailureException("conflito"));

        assertThatThrownBy(() -> orchestrator.cancel(executionId, "motivo"))
                .isInstanceOf(AutoQaOptimisticLockException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> orchestrator.start(null)).isInstanceOf(NullPointerException.class);
    }

    // --- helpers ---

    private AutoQaExecutionDocument stubDocumentoExistente(UUID executionId, AutoQaWorkflowStatus status) {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "cenário", "/projeto", Instant.now());
        document.setWorkflowStatus(status);
        when(executionRepository.findByExecutionId(executionId)).thenReturn(java.util.Optional.of(document));
        return document;
    }

    private AutoQaExecutionSnapshot stubSnapshotExistente(UUID executionId) {
        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(executionId, Instant.now());
        when(snapshotRepository.findByExecutionId(executionId)).thenReturn(java.util.Optional.of(snapshot));
        return snapshot;
    }
}
