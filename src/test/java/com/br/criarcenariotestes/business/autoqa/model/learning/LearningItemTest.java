package com.br.criarcenariotestes.business.autoqa.model.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningItem - Testes Unitários")
class LearningItemTest {

    @Test
    @DisplayName("Deve criar item válido")
    void deveCriarItemValido() {
        LearningItem item = sample();
        assertThat(item.id()).isEqualTo("abc123");
        assertThat(item.type()).isEqualTo(LearningType.REUSABLE_COMPONENT);
        assertThat(item.scope()).isEqualTo(LearningScope.EXECUTION);
    }

    @Test
    @DisplayName("Deve rejeitar id nulo")
    void deveRejeitarIdNulo() {
        assertThatThrownBy(() -> new LearningItem(null, LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar id em branco")
    void deveRejeitarIdEmBranco() {
        assertThatThrownBy(() -> new LearningItem("  ", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar type nulo")
    void deveRejeitarTypeNulo() {
        assertThatThrownBy(() -> new LearningItem("id", null, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar scope nulo")
    void deveRejeitarScopeNulo() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, null,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar source nulo")
    void deveRejeitarSourceNulo() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                null, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar confidence nula")
    void deveRejeitarConfidenceNula() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", null, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar approvalStatus nulo")
    void deveRejeitarApprovalStatusNulo() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar title nulo")
    void deveRejeitarTitleNulo() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, null, "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar description nula")
    void deveRejeitarDescriptionNula() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", null, "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar recommendation nula")
    void deveRejeitarRecommendationNula() {
        assertThatThrownBy(() -> new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", null, LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve tratar listas nulas como vazias")
    void deveTratarListasNulasComoVazias() {
        LearningItem item = new LearningItem("id", LearningType.UNKNOWN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                null, null, null, null, null, true, false, false);
        assertThat(item.evidence()).isEmpty();
        assertThat(item.relatedComponents()).isEmpty();
        assertThat(item.relatedFiles()).isEmpty();
        assertThat(item.relatedFailureCodes()).isEmpty();
        assertThat(item.tags()).isEmpty();
    }

    @Test
    @DisplayName("Listas devem ser imutáveis")
    void listasDevemSerImutaveis() {
        LearningItem item = sample();
        assertThatThrownBy(() -> item.tags().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    private LearningItem sample() {
        return new LearningItem("abc123", LearningType.REUSABLE_COMPONENT, LearningScope.EXECUTION,
                LearningSource.GENERATION, "Componente reutilizado", "Descrição", "Recomendação",
                LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of("PageObject"), List.of("tests/login.spec.ts"), List.of(), List.of("reuso"),
                true, true, false);
    }
}
