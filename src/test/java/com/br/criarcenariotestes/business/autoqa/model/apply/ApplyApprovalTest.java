package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApplyApproval - Testes Unitários")
class ApplyApprovalTest {

    @Test
    @DisplayName("Deve rejeitar approvedBy nulo")
    void deveRejeitarApprovedByNulo() {
        assertThatThrownBy(() -> new ApplyApproval(true, null, LocalDateTime.now(), List.of(ApplyOperation.CREATE), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approvedBy");
    }

    @Test
    @DisplayName("Deve rejeitar approvedBy em branco")
    void deveRejeitarApprovedByEmBranco() {
        assertThatThrownBy(() -> new ApplyApproval(true, "   ", LocalDateTime.now(), List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar approvedAt nulo")
    void deveRejeitarApprovedAtNulo() {
        assertThatThrownBy(() -> new ApplyApproval(true, "qa.lead", null, List.of(), false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve remover espaços de approvedBy")
    void deveRemoverEspacosDeApprovedBy() {
        ApplyApproval approval = new ApplyApproval(true, "  qa.lead  ", LocalDateTime.now(), List.of(), false, false);

        assertThat(approval.approvedBy()).isEqualTo("qa.lead");
    }

    @Test
    @DisplayName("Deve tratar approvedOperations nulo como lista vazia")
    void deveTratarApprovedOperationsNuloComoVazia() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(), null, false, false);

        assertThat(approval.approvedOperations()).isEmpty();
    }

    @Test
    @DisplayName("permits deve retornar false quando approved=false, mesmo com operação na lista")
    void devePermitsRetornarFalseQuandoNaoAprovado() {
        ApplyApproval approval = new ApplyApproval(false, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true);

        assertThat(approval.permits(ApplyOperation.CREATE)).isFalse();
    }

    @Test
    @DisplayName("permits deve retornar true para CREATE aprovado")
    void devePermitsRetornarTrueParaCreateAprovado() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), false, false);

        assertThat(approval.permits(ApplyOperation.CREATE)).isTrue();
    }

    @Test
    @DisplayName("permits deve retornar false para UPDATE sem allowFileUpdate")
    void devePermitsRetornarFalseParaUpdateSemAllowFileUpdate() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.UPDATE), false, false);

        assertThat(approval.permits(ApplyOperation.UPDATE)).isFalse();
    }

    @Test
    @DisplayName("permits deve retornar true para UPDATE com allowFileUpdate")
    void devePermitsRetornarTrueParaUpdateComAllowFileUpdate() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.UPDATE), true, false);

        assertThat(approval.permits(ApplyOperation.UPDATE)).isTrue();
    }

    @Test
    @DisplayName("permits deve retornar false quando operação não está na lista aprovada")
    void devePermitsRetornarFalseQuandoOperacaoNaoAprovada() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, false);

        assertThat(approval.permits(ApplyOperation.UPDATE)).isFalse();
    }

    @Test
    @DisplayName("permits deve retornar false para REUSE e NONE mesmo se listados")
    void devePermitsRetornarFalseParaReuseENone() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.REUSE, ApplyOperation.NONE), true, true);

        assertThat(approval.permits(ApplyOperation.REUSE)).isFalse();
        assertThat(approval.permits(ApplyOperation.NONE)).isFalse();
    }
}
