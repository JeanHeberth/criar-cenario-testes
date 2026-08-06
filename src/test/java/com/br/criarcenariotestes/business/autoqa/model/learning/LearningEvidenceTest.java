package com.br.criarcenariotestes.business.autoqa.model.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningEvidence - Testes Unitários")
class LearningEvidenceTest {

    @Test
    @DisplayName("Deve criar evidência válida")
    void deveCriarEvidenciaValida() {
        LearningEvidence evidence = new LearningEvidence(
                "generation", "reused-file", "tests/login.spec.ts", "tests/login.spec.ts",
                "Arquivo reaproveitado na geração", "excerto curto", false);

        assertThat(evidence.source()).isEqualTo("generation");
        assertThat(evidence.referenceType()).isEqualTo("reused-file");
        assertThat(evidence.description()).isEqualTo("Arquivo reaproveitado na geração");
    }

    @Test
    @DisplayName("Deve rejeitar source nulo")
    void deveRejeitarSourceNulo() {
        assertThatThrownBy(() -> new LearningEvidence(null, "type", "id", null, "desc", null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar referenceType nulo")
    void deveRejeitarReferenceTypeNulo() {
        assertThatThrownBy(() -> new LearningEvidence("source", null, "id", null, "desc", null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar description nula")
    void deveRejeitarDescriptionNula() {
        assertThatThrownBy(() -> new LearningEvidence("source", "type", "id", null, null, null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath absoluto")
    void deveRejeitarRelativePathAbsoluto() {
        assertThatThrownBy(() -> new LearningEvidence("source", "type", "id", "/etc/passwd", "desc", null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath com path traversal")
    void deveRejeitarRelativePathComTraversal() {
        assertThatThrownBy(() -> new LearningEvidence("source", "type", "id", "../../etc/passwd", "desc", null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve truncar excerpt maior que o limite")
    void deveTruncarExcerptMaiorQueLimite() {
        String longExcerpt = "a".repeat(1000);
        LearningEvidence evidence = new LearningEvidence("source", "type", "id", null, "desc", longExcerpt, false);
        assertThat(evidence.excerpt().length()).isLessThanOrEqualTo(300);
    }

    @Test
    @DisplayName("Deve aceitar excerpt nulo")
    void deveAceitarExcerptNulo() {
        LearningEvidence evidence = new LearningEvidence("source", "type", "id", null, "desc", null, false);
        assertThat(evidence.excerpt()).isNull();
    }

    @Test
    @DisplayName("Deve normalizar (trim) relativePath")
    void deveNormalizarRelativePath() {
        LearningEvidence evidence = new LearningEvidence("source", "type", "id", "  tests/a.ts  ", "desc", null, false);
        assertThat(evidence.relativePath()).isEqualTo("tests/a.ts");
    }
}
