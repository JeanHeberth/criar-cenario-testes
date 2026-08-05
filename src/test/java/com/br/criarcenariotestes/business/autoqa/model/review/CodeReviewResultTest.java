package com.br.criarcenariotestes.business.autoqa.model.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CodeReviewResult - Testes Unitários")
class CodeReviewResultTest {

    @Test
    @DisplayName("Deve criar CodeReviewResult válido")
    void deveCriarCodeReviewValido() {
        UUID executionId = UUID.randomUUID();
        CodeReviewResult result = new CodeReviewResult(
                executionId, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true
        );

        assertThat(result.executionId()).isEqualTo(executionId);
        assertThat(result.status()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve exigir executionId não nulo (nunca é alvo de parse direto da IA)")
    void deveExigirExecutionIdNaoNulo() {
        assertThatThrownBy(() -> new CodeReviewResult(
                null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        CodeReviewResult result = sample();

        assertThatThrownBy(() -> result.files().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.globalIssues().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.warnings().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve normalizar coleções nulas para vazias")
    void deveNormalizarColecoesNulasParaVazias() {
        CodeReviewResult result = new CodeReviewResult(
                UUID.randomUUID(), null, null, null, null, null, null,
                ReviewStatus.BLOCKED, null, true, true
        );

        assertThat(result.files()).isEmpty();
        assertThat(result.globalIssues()).isEmpty();
        assertThat(result.confidence()).isEqualTo(ReviewConfidence.UNKNOWN);
    }

    @Test
    @DisplayName("Deve representar status BLOCKED com humanReviewRequired verdadeiro")
    void deveRepresentarStatusBlocked() {
        CodeReviewResult result = new CodeReviewResult(
                UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.BLOCKED, ReviewConfidence.HIGH, true, true
        );

        assertThat(result.status()).isEqualTo(ReviewStatus.BLOCKED);
        assertThat(result.humanReviewRequired()).isTrue();
    }

    @Test
    @DisplayName("Não deve armazenar projectPath em nenhum campo textual")
    void deveNaoIncluirProjectPath() {
        CodeReviewResult result = sample();
        assertThat(result.toString()).doesNotContain("/project");
    }

    private CodeReviewResult sample() {
        return new CodeReviewResult(
                UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true
        );
    }
}
