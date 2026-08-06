package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApplyResult - Testes Unitários")
class ApplyResultTest {

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> new ApplyResult(null, List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/x", ApplyStatus.COMPLETED, false, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        assertThatThrownBy(() -> new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/x", null, false, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        ApplyResult result = new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/x", ApplyStatus.COMPLETED, false, true);

        assertThatThrownBy(() -> result.files().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.backups().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.conflicts().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.warnings().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve tratar listas nulas como vazias")
    void deveTratarListasNulasComoVazias() {
        ApplyResult result = new ApplyResult(UUID.randomUUID(), null, null, null, null,
                null, null, ApplyStatus.BLOCKED, false, true);

        assertThat(result.files()).isEmpty();
        assertThat(result.backups()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.projectRootReference()).isNull();
        assertThat(result.backupRoot()).isNull();
    }

    @Test
    @DisplayName("Não deve expor conteúdo de arquivo em nenhum campo do record")
    void naoDeveExporConteudoDeArquivo() {
        ApplyResult result = new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "criar-cenario-testes", ".auto-qa/backups/x", ApplyStatus.COMPLETED, false, true);

        assertThat(result.toString()).doesNotContain("content", "senha", "password");
    }
}
