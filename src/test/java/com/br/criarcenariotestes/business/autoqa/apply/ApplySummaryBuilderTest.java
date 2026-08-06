package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplySummaryBuilder - Testes Unitários")
class ApplySummaryBuilderTest {

    private final ApplySummaryBuilder builder = new ApplySummaryBuilder();

    @Test
    @DisplayName("Deve expor apenas o nome final do diretório do projeto, nunca o path absoluto completo")
    void deveExporApenasNomeFinalDoProjeto() {
        Path projectRoot = Path.of("/Users/alguem/Development/meu-projeto-secreto");

        ApplyResult result = builder.build(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                projectRoot, Path.of(".auto-qa/backups/x"), ApplyStatus.COMPLETED, false, true);

        assertThat(result.projectRootReference()).isEqualTo("meu-projeto-secreto");
        assertThat(result.projectRootReference()).doesNotContain("/Users/alguem");
    }

    @Test
    @DisplayName("Deve manter backupRoot como caminho relativo")
    void deveManterBackupRootRelativo() {
        UUID executionId = UUID.randomUUID();
        ApplyResult result = builder.build(executionId, List.of(), List.of(), List.of(), List.of(),
                Path.of("/projeto"), Path.of(".auto-qa/backups/" + executionId), ApplyStatus.COMPLETED, false, true);

        assertThat(result.backupRoot()).isEqualTo(".auto-qa/backups/" + executionId);
    }

    @Test
    @DisplayName("Deve normalizar separadores de path do backupRoot")
    void deveNormalizarSeparadoresDoBackupRoot() {
        ApplyResult result = builder.build(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                Path.of("/projeto"), Path.of(".auto-qa\\backups\\x"), ApplyStatus.COMPLETED, false, true);

        assertThat(result.backupRoot()).doesNotContain("\\");
    }

    @Test
    @DisplayName("Deve tratar projectRoot nulo retornando referência nula")
    void deveTratarProjectRootNulo() {
        ApplyResult result = builder.build(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                null, Path.of(".auto-qa/backups/x"), ApplyStatus.BLOCKED, false, true);

        assertThat(result.projectRootReference()).isNull();
    }

    @Test
    @DisplayName("Deve tratar backupRoot nulo retornando valor nulo")
    void deveTratarBackupRootNulo() {
        ApplyResult result = builder.build(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                Path.of("/projeto"), null, ApplyStatus.BLOCKED, false, true);

        assertThat(result.backupRoot()).isNull();
    }

    @Test
    @DisplayName("Deve propagar status, rollbackExecuted e valid exatamente como fornecidos")
    void devePropagarCamposDeStatus() {
        ApplyResult result = builder.build(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                Path.of("/projeto"), Path.of(".auto-qa/backups/x"), ApplyStatus.ROLLED_BACK, true, true);

        assertThat(result.status()).isEqualTo(ApplyStatus.ROLLED_BACK);
        assertThat(result.rollbackExecuted()).isTrue();
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> builder.build(null, List.of(), List.of(), List.of(), List.of(),
                        Path.of("/projeto"), Path.of(".auto-qa/backups/x"), ApplyStatus.COMPLETED, false, true))
                .isInstanceOf(NullPointerException.class);
    }
}
