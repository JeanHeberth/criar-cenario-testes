package com.br.criarcenariotestes.business.autoqa.model.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningResult - Testes Unitários")
class LearningResultTest {

    @Test
    @DisplayName("Deve criar resultado válido")
    void deveCriarResultadoValido() {
        UUID executionId = UUID.randomUUID();
        LearningResult result = new LearningResult(executionId, List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThat(result.executionId()).isEqualTo(executionId);
        assertThat(result.status()).isEqualTo(LearningStatus.SKIPPED);
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> new LearningResult(null, List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        assertThatThrownBy(() -> new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                null, LearningConfidence.UNKNOWN, 0, 0, false, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar confidence nula")
    void deveRejeitarConfidenceNula() {
        assertThatThrownBy(() -> new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, null, 0, 0, false, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar positiveLearnings negativo")
    void deveRejeitarPositiveLearningsNegativo() {
        assertThatThrownBy(() -> new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, -1, 0, false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar negativeLearnings negativo")
    void deveRejeitarNegativeLearningsNegativo() {
        assertThatThrownBy(() -> new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, -1, false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve tratar listas nulas como vazias")
    void deveTratarListasNulasComoVazias() {
        LearningResult result = new LearningResult(UUID.randomUUID(), null, null, null,
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThat(result.items()).isEmpty();
        assertThat(result.globalEvidence()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Não deve corrigir status inconsistente no compact constructor")
    void naoDeveCorrigirStatusInconsistente() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.COLLECTED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThat(result.status()).isEqualTo(LearningStatus.COLLECTED);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Listas devem ser imutáveis")
    void listasDevemSerImutaveis() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThatThrownBy(() -> result.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
