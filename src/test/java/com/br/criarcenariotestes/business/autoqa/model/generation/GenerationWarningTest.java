package com.br.criarcenariotestes.business.autoqa.model.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerationWarning - Testes Unitários")
class GenerationWarningTest {

    @Test
    @DisplayName("Deve criar warning válido")
    void deveCriarWarningValido() {
        GenerationWarning warning = new GenerationWarning("LOW_CONFIDENCE", "Confiança baixa", false);

        assertThat(warning.code()).isEqualTo("LOW_CONFIDENCE");
        assertThat(warning.blocking()).isFalse();
    }

    @Test
    @DisplayName("Deve remover espaços de code e description")
    void deveTrimarCodeEDescription() {
        GenerationWarning warning = new GenerationWarning("  LOW_CONFIDENCE  ", "  desc  ", true);

        assertThat(warning.code()).isEqualTo("LOW_CONFIDENCE");
        assertThat(warning.description()).isEqualTo("desc");
    }
}
