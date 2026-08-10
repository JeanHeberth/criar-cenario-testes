package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaExecutionConflictException;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaOptimisticLockException;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaContextSnapshotMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaApprovalRecord;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionRepository;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshot;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshotRepository;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testa concorrência/idempotência ponta a ponta: AutoQaExecutionOrchestrator
 * real + AutoQaTransitionValidator real (nunca mockado aqui), só
 * repositórios/StageExecutor/mapper mockados. Cobre exatamente a lista da
 * seção "CONCORRÊNCIA" do plano aprovado.
 */
@DisplayName("Concorrência e idempotência - Testes de Integração (repositories mockados)")
class AutoQaExecutionConcurrencyTest {

    private AutoQaExecutionRepository executionRepository;
    private AutoQaExecutionSnapshotRepository snapshotRepository;
    private AutoQaExecutionOrchestrator orchestrator;
    private UUID executionId;

    @BeforeEach
    void setUp() {
        executionRepository = mock(AutoQaExecutionRepository.class);
        snapshotRepository = mock(AutoQaExecutionSnapshotRepository.class);
        AutoQaContextSnapshotMapper snapshotMapper = mock(AutoQaContextSnapshotMapper.class);
        AutoQaStageExecutor stageExecutor = mock(AutoQaStageExecutor.class);
        AutoQaAvailableActionResolver actionResolver = mock(AutoQaAvailableActionResolver.class);
        AutoQaProperties properties = new AutoQaProperties();
        properties.setAllowFileApplication(true);
        properties.setAllowCommandExecution(true);
        properties.setSensitiveActionsEnabled(true);

        // Path de teste ("/projeto") nunca passa por create() neste arquivo (só start/apply/execute
        // sobre documentos já existentes stubados), então a política real de allowedRoots nunca é
        // exercitada aqui — a cobertura de ProjectPathSecurityValidator vive em
        // ProjectPathSecurityValidatorTest e nos novos testes de create() em AutoQaExecutionOrchestratorTest.
        orchestrator = new AutoQaExecutionOrchestrator(executionRepository, snapshotRepository, snapshotMapper,
                stageExecutor, new AutoQaTransitionValidator(), actionResolver, properties,
                new ProjectPathSecurityValidator(properties));

        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(actionResolver.resolve(any(), any())).thenReturn(Set.of());
        when(snapshotMapper.toContext(any(), any(), any())).thenReturn(AutoQaContext.create("cenário", "/projeto"));
        when(stageExecutor.executeStages(any(), any()))
                .thenReturn(AutoQaStageExecutor.AutoQaStageExecutionResult.success(AutoQaStage.PLANNING, AutoQaStage.PLANNING));

        executionId = UUID.randomUUID();
        stub(AutoQaExecutionDocument.createNew(executionId, "cenário", "/projeto", Instant.now()));
        stubSnapshot();
    }

    @Test
    @DisplayName("Dois start concorrentes: o segundo deve ser rejeitado enquanto o primeiro está IN_PROGRESS")
    void doisStartConcorrentes() {
        AutoQaExecutionDocument locked = document();
        locked.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        stub(locked);

        assertThatThrownBy(() -> orchestrator.start(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
        verifyNoInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("Dois continue concorrentes: o segundo deve ser rejeitado")
    void doisContinueConcorrentes() {
        AutoQaExecutionDocument locked = document();
        locked.setWorkflowStatus(AutoQaWorkflowStatus.FAILED);
        locked.setLastStageStarted(AutoQaStage.PLANNING);
        locked.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        stub(locked);

        assertThatThrownBy(() -> orchestrator.continueExecution(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Dois generate concorrentes: o segundo deve ser rejeitado")
    void doisGenerateConcorrentes() {
        AutoQaExecutionDocument locked = document();
        locked.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        locked.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        stub(locked);

        assertThatThrownBy(() -> orchestrator.generate(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Aprovação duplicada (dois apply-approval) deve ser rejeitada")
    void aprovacaoDuplicada() {
        AutoQaExecutionDocument doc = document();
        doc.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        doc.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        stub(doc);
        ApplyApproval approval = new ApplyApproval(true, "qa.lead2", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true);

        assertThatThrownBy(() -> orchestrator.registerApplyApproval(executionId, approval))
                .isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Dois apply concorrentes: o segundo deve ser rejeitado")
    void doisApplyConcorrentes() {
        AutoQaExecutionDocument locked = document();
        locked.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        locked.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        locked.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        stub(locked);

        assertThatThrownBy(() -> orchestrator.apply(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Dois execute concorrentes: o segundo deve ser rejeitado")
    void doisExecuteConcorrentes() {
        AutoQaExecutionDocument locked = document();
        locked.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        locked.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        locked.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        stub(locked);

        assertThatThrownBy(() -> orchestrator.execute(executionId)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Cancelamento concorrente (já cancelado) deve ser rejeitado")
    void cancelamentoConcorrente() {
        AutoQaExecutionDocument doc = document();
        doc.setWorkflowStatus(AutoQaWorkflowStatus.CANCELLED);
        stub(doc);

        assertThatThrownBy(() -> orchestrator.cancel(executionId, "motivo")).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Qualquer ação após execução em estado terminal (COMPLETED) deve ser rejeitada")
    void operacaoAposTerminalRejeitada() {
        AutoQaExecutionDocument doc = document();
        doc.setWorkflowStatus(AutoQaWorkflowStatus.COMPLETED);
        stub(doc);

        assertThatThrownBy(() -> orchestrator.cancel(executionId, "motivo")).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("Retomada duplicada (aplicação já concluída) não deve permitir novo apply")
    void retomadaDuplicadaNaoPermiteNovoApply() {
        AutoQaExecutionDocument doc = document();
        doc.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL); // já passou do apply
        stub(doc);

        assertThatThrownBy(() -> orchestrator.apply(executionId)).isInstanceOf(com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("Conflito de optimistic locking na primeira gravação (lock) deve interromper a operação sem sobrescrever silenciosamente")
    void optimisticLockingInterrompeOperacao() {
        when(executionRepository.save(any())).thenThrow(new OptimisticLockingFailureException("versão desatualizada"));

        assertThatThrownBy(() -> orchestrator.start(executionId)).isInstanceOf(AutoQaOptimisticLockException.class);
        verifyNoInteractions(snapshotRepository);
    }

    private AutoQaExecutionDocument document() {
        return AutoQaExecutionDocument.createNew(executionId, "cenário", "/projeto", Instant.now());
    }

    private void stub(AutoQaExecutionDocument document) {
        when(executionRepository.findByExecutionId(executionId)).thenReturn(Optional.of(document));
    }

    private void stubSnapshot() {
        when(snapshotRepository.findByExecutionId(executionId))
                .thenReturn(Optional.of(AutoQaExecutionSnapshot.createNew(executionId, Instant.now())));
    }
}
