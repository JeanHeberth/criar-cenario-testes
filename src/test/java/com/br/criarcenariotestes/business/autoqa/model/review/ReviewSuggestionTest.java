package com.br.criarcenariotestes.business.autoqa.model.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReviewSuggestion - Testes Unitários")
class ReviewSuggestionTest {

    @Test
    @DisplayName("Deve criar sugestão válida")
    void deveCriarSugestaoValida() {
        ReviewSuggestion suggestion = new ReviewSuggestion(
                "tests/login.spec.ts", "Substituir seletor", ReviewSeverity.MEDIUM,
                "Reduz fragilidade", false, List.of("FRAGILE_SELECTOR")
        );

        assertThat(suggestion.description()).isEqualTo("Substituir seletor");
        assertThat(suggestion.relatedIssueCodes()).containsExactly("FRAGILE_SELECTOR");
    }

    @Test
    @DisplayName("Deve retornar coleção imutável")
    void deveRetornarColecaoImutavel() {
        ReviewSuggestion suggestion = new ReviewSuggestion(
                "tests/login.spec.ts", "desc", ReviewSeverity.LOW, "rationale", false, List.of()
        );

        assertThatThrownBy(() -> suggestion.relatedIssueCodes().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve remover espaços de relativePath, description e rationale")
    void deveTrimarCampos() {
        ReviewSuggestion suggestion = new ReviewSuggestion(
                "  tests/x.ts  ", "  desc  ", ReviewSeverity.LOW, "  rationale  ", true, List.of()
        );

        assertThat(suggestion.relativePath()).isEqualTo("tests/x.ts");
        assertThat(suggestion.description()).isEqualTo("desc");
        assertThat(suggestion.rationale()).isEqualTo("rationale");
    }
}
