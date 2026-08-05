package com.br.criarcenariotestes.business.autoqa.model.review;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileReviewResult - Testes Unitários")
class FileReviewResultTest {

    @Test
    @DisplayName("Deve criar FileReviewResult válido")
    void deveCriarFileReviewValido() {
        FileReviewResult result = new FileReviewResult(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.APPROVED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true
        );

        assertThat(result.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(result.status()).isEqualTo(FileReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        FileReviewResult result = new FileReviewResult(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.APPROVED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true
        );

        assertThatThrownBy(() -> result.issues().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.suggestions().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve permitir status SKIPPED para REUSE")
    void devePermitirSkippedParaReuse() {
        FileReviewResult result = new FileReviewResult(
                "pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                FileReviewStatus.SKIPPED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.UNKNOWN, true
        );

        assertThat(result.status()).isEqualTo(FileReviewStatus.SKIPPED);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("Deve definir confidence UNKNOWN quando nula")
    void deveDefinirConfidenceUnknownQuandoNula() {
        FileReviewResult result = new FileReviewResult(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.APPROVED, List.of(), List.of(), List.of(), List.of(), null, true
        );

        assertThat(result.confidence()).isEqualTo(ReviewConfidence.UNKNOWN);
    }

    @Test
    @DisplayName("Deve remover espaços do relativePath")
    void deveTrimarRelativePath() {
        FileReviewResult result = new FileReviewResult(
                "  tests/login.spec.ts  ", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.APPROVED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true
        );

        assertThat(result.relativePath()).isEqualTo("tests/login.spec.ts");
    }
}
