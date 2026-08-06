package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaExecutionConflictException;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaInvalidTransitionException;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaApprovalRecord;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutoQaTransitionValidator - Testes Unitários")
class AutoQaTransitionValidatorTest {

    private final AutoQaTransitionValidator validator = new AutoQaTransitionValidator();

    @Test
    @DisplayName("start deve ser permitido a partir de CREATED")
    void startPermitidoAPartirDeCreated() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        assertThatCode(() -> validator.validateStart(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("start deve ser rejeitado fora de CREATED")
    void startRejeitadoForaDeCreated() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        assertThatThrownBy(() -> validator.validateStart(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("start deve ser rejeitado quando há operação em andamento")
    void startRejeitadoQuandoBloqueado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        assertThatThrownBy(() -> validator.validateStart(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("continue deve ser permitido a partir de FAILED")
    void continuePermitidoAPartirDeFailed() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.FAILED);
        assertThatCode(() -> validator.validateContinue(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("continue deve ser rejeitado fora de FAILED")
    void continueRejeitadoForaDeFailed() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        assertThatThrownBy(() -> validator.validateContinue(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("continue deve ser rejeitado quando há operação em andamento")
    void continueRejeitadoQuandoBloqueado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.FAILED);
        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        assertThatThrownBy(() -> validator.validateContinue(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("generate deve ser permitido a partir de WAITING_GENERATION_APPROVAL")
    void generatePermitido() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        assertThatCode(() -> validator.validateGenerate(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("generate deve ser rejeitado fora de WAITING_GENERATION_APPROVAL")
    void generateRejeitadoForaDoEstado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        assertThatThrownBy(() -> validator.validateGenerate(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("registrar ApplyApproval deve ser permitido a partir de WAITING_APPLY_APPROVAL sem aprovação prévia")
    void registerApplyApprovalPermitido() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        assertThatCode(() -> validator.validateRegisterApplyApproval(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registrar ApplyApproval deve ser rejeitado se já houver aprovação registrada")
    void registerApplyApprovalRejeitaDuplicidade() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        assertThatThrownBy(() -> validator.validateRegisterApplyApproval(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("registrar ApplyApproval deve ser rejeitado fora de WAITING_APPLY_APPROVAL")
    void registerApplyApprovalRejeitadoForaDoEstado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        assertThatThrownBy(() -> validator.validateRegisterApplyApproval(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("apply deve ser permitido quando aprovação já registrada")
    void applyPermitidoComAprovacao() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        assertThatCode(() -> validator.validateApply(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("apply deve ser rejeitado sem aprovação registrada")
    void applyRejeitadoSemAprovacao() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        assertThatThrownBy(() -> validator.validateApply(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("apply deve ser rejeitado fora de WAITING_APPLY_APPROVAL")
    void applyRejeitadoForaDoEstado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        assertThatThrownBy(() -> validator.validateApply(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("apply deve ser rejeitado quando há operação em andamento (dois apply)")
    void applyRejeitadoQuandoBloqueado() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        assertThatThrownBy(() -> validator.validateApply(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("registrar ExecutionApproval deve ser permitido a partir de WAITING_EXECUTION_APPROVAL")
    void registerExecutionApprovalPermitido() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        assertThatCode(() -> validator.validateRegisterExecutionApproval(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registrar ExecutionApproval deve ser rejeitado se já registrada")
    void registerExecutionApprovalRejeitaDuplicidade() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        assertThatThrownBy(() -> validator.validateRegisterExecutionApproval(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("execute deve ser permitido quando aprovação já registrada")
    void executePermitidoComAprovacao() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        assertThatCode(() -> validator.validateExecute(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("execute deve ser rejeitado sem aprovação registrada")
    void executeRejeitadoSemAprovacao() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        assertThatThrownBy(() -> validator.validateExecute(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    @Test
    @DisplayName("execute duplicado (dois execute concorrentes) deve ser rejeitado pelo lock")
    void executeDuplicadoRejeitadoPeloLock() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        assertThatThrownBy(() -> validator.validateExecute(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("cancel deve ser permitido em qualquer estado não-terminal")
    void cancelPermitidoEmEstadoNaoTerminal() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.RUNNING);
        assertThatCode(() -> validator.validateCancel(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cancel deve ser rejeitado em COMPLETED")
    void cancelRejeitadoEmCompleted() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.COMPLETED);
        assertThatThrownBy(() -> validator.validateCancel(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("cancel deve ser rejeitado em CANCELLED (cancelamento duplicado)")
    void cancelRejeitadoEmCancelled() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CANCELLED);
        assertThatThrownBy(() -> validator.validateCancel(document)).isInstanceOf(AutoQaExecutionConflictException.class);
    }

    @Test
    @DisplayName("operação após execução cancelada deve ser rejeitada")
    void operacaoAposCancelamentoDeveSerRejeitada() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CANCELLED);
        assertThatThrownBy(() -> validator.validateContinue(document)).isInstanceOf(AutoQaInvalidTransitionException.class);
    }

    private AutoQaExecutionDocument documento(AutoQaWorkflowStatus status) {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        document.setWorkflowStatus(status);
        return document;
    }
}
