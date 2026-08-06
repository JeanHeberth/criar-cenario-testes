package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ExecutionValidationException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionPreconditionValidator - Testes Unitários")
class ExecutionPreconditionValidatorTest {

    private final ExecutionPreconditionValidator validator = new ExecutionPreconditionValidator();

    private ApplyResult applyResult(ApplyStatus status) {
        return new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/x", status, false, true);
    }

    private ExecutionApproval approval(boolean approved, boolean allowTestExecution) {
        return new ExecutionApproval(approved, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), allowTestExecution, false, false);
    }

    @Test
    @DisplayName("Deve aceitar ApplyStatus COMPLETED e aprovação válida")
    void deveAceitarApplyCompletedEAprovacaoValida() {
        assertThatCode(() -> validator.validate(applyResult(ApplyStatus.COMPLETED), approval(true, true)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar ApplyStatus COMPLETED_WITH_WARNINGS")
    void deveAceitarApplyCompletedWithWarnings() {
        assertThatCode(() -> validator.validate(applyResult(ApplyStatus.COMPLETED_WITH_WARNINGS), approval(true, true)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus BLOCKED")
    void deveRejeitarApplyBlocked() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.BLOCKED), approval(true, true)))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus ROLLED_BACK")
    void deveRejeitarApplyRolledBack() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.ROLLED_BACK), approval(true, true)))
                .isInstanceOf(ExecutionValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus FAILED")
    void deveRejeitarApplyFailed() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.FAILED), approval(true, true)))
                .isInstanceOf(ExecutionValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar aprovação nula")
    void deveRejeitarApprovalNula() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.COMPLETED), null))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("ExecutionApproval");
    }

    @Test
    @DisplayName("Deve rejeitar aprovação com approved=false")
    void deveRejeitarApprovalNaoAprovada() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.COMPLETED), approval(false, true)))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("approved");
    }

    @Test
    @DisplayName("Deve rejeitar allowTestExecution=false")
    void deveRejeitarAllowTestExecutionFalso() {
        assertThatThrownBy(() -> validator.validate(applyResult(ApplyStatus.COMPLETED), approval(true, false)))
                .isInstanceOf(ExecutionValidationException.class)
                .hasMessageContaining("allowTestExecution");
    }

    @Test
    @DisplayName("Deve rejeitar applyResult nulo")
    void deveRejeitarApplyResultNulo() {
        assertThatThrownBy(() -> validator.validate(null, approval(true, true)))
                .isInstanceOf(NullPointerException.class);
    }
}
