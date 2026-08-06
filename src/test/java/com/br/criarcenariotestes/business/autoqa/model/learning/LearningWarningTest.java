package com.br.criarcenariotestes.business.autoqa.model.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningWarning - Testes Unitários")
class LearningWarningTest {

    @Test
    @DisplayName("Deve criar warning válido")
    void deveCriarWarningValido() {
        LearningWarning warning = new LearningWarning("SINGLE_EXECUTION_ONLY", "Aprendizado baseado em uma única execução", false);
        assertThat(warning.code()).isEqualTo("SINGLE_EXECUTION_ONLY");
        assertThat(warning.blocking()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar code nulo")
    void deveRejeitarCodeNulo() {
        assertThatThrownBy(() -> new LearningWarning(null, "desc", false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar description nula")
    void deveRejeitarDescriptionNula() {
        assertThatThrownBy(() -> new LearningWarning("CODE", null, false))
                .isInstanceOf(NullPointerException.class);
    }
}
