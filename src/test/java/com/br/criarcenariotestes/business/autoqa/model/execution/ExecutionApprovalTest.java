package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionApproval - Testes Unitários")
class ExecutionApprovalTest {

    @Test
    @DisplayName("Deve criar ExecutionApproval válida")
    void deveCriarExecutionApprovalValida() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.GRADLE_WRAPPER_TEST), true, false, false);

        assertThat(approval.approved()).isTrue();
        assertThat(approval.approvedBy()).isEqualTo("qa.lead");
        assertThat(approval.allowedCommands()).containsExactly(ExecutionCommandId.GRADLE_WRAPPER_TEST);
    }

    @Test
    @DisplayName("Deve rejeitar approvedBy nulo")
    void deveRejeitarApprovedByNulo() {
        assertThatThrownBy(() -> new ExecutionApproval(true, null, LocalDateTime.now(), Set.of(), true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approvedBy");
    }

    @Test
    @DisplayName("Deve rejeitar approvedBy em branco")
    void deveRejeitarApprovedByEmBranco() {
        assertThatThrownBy(() -> new ExecutionApproval(true, "   ", LocalDateTime.now(), Set.of(), true, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar approvedAt nulo")
    void deveRejeitarApprovedAtNulo() {
        assertThatThrownBy(() -> new ExecutionApproval(true, "qa.lead", null, Set.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve tratar allowedCommands nulo como conjunto vazio")
    void deveTratarAllowedCommandsNuloComoVazio() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(), null, true, false, false);

        assertThat(approval.allowedCommands()).isEmpty();
    }

    @Test
    @DisplayName("allowedCommands deve usar o enum fechado, não texto livre")
    void allowedCommandsDeveUsarEnumFechado() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), true, false, false);

        assertThat(approval.allowedCommands()).allSatisfy(id -> assertThat(id).isInstanceOf(ExecutionCommandId.class));
    }

    @Test
    @DisplayName("permits deve retornar false quando approved=false")
    void devePermitsRetornarFalseQuandoNaoAprovado() {
        ExecutionApproval approval = new ExecutionApproval(false, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), true, false, false);

        assertThat(approval.permits(ExecutionCommandId.PYTEST)).isFalse();
    }

    @Test
    @DisplayName("permits deve retornar false quando allowTestExecution=false")
    void devePermitsRetornarFalseQuandoAllowTestExecutionFalso() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), false, false, false);

        assertThat(approval.permits(ExecutionCommandId.PYTEST)).isFalse();
    }

    @Test
    @DisplayName("permits deve retornar true quando aprovado, allowTestExecution e commandId presente")
    void devePermitsRetornarTrue() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), true, false, false);

        assertThat(approval.permits(ExecutionCommandId.PYTEST)).isTrue();
    }

    @Test
    @DisplayName("permits deve retornar false para commandId não listado")
    void devePermitsRetornarFalseParaCommandIdNaoListado() {
        ExecutionApproval approval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PYTEST), true, false, false);

        assertThat(approval.permits(ExecutionCommandId.MAVEN_TEST)).isFalse();
    }
}
