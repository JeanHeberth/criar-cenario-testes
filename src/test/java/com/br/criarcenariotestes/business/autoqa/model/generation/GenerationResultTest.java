package com.br.criarcenariotestes.business.autoqa.model.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationResult - Testes Unitários")
class GenerationResultTest {

    @Test
    @DisplayName("Deve criar GenerationResult válido")
    void deveCriarGenerationResultValido() {
        UUID executionId = UUID.randomUUID();
        GenerationResult result = new GenerationResult(
                executionId, "PLAYWRIGHT", "TYPESCRIPT",
                List.of(), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );

        assertThat(result.executionId()).isEqualTo(executionId);
        assertThat(result.status()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        GenerationResult result = sample();

        assertThatThrownBy(() -> result.files().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.reusedFiles().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.warnings().add(new GenerationWarning("C", "d", false))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve definir confidence UNKNOWN quando nulo")
    void deveDefinirConfidenceUnknownQuandoNulo() {
        GenerationResult result = new GenerationResult(
                UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                List.of(), List.of(), List.of(), "root", "manifest.json",
                GenerationStatus.FAILED, null, false
        );

        assertThat(result.confidence()).isEqualTo(GenerationConfidence.UNKNOWN);
    }

    @Test
    @DisplayName("Deve remover espaços de framework e language")
    void deveTrimarFrameworkELanguage() {
        GenerationResult result = new GenerationResult(
                UUID.randomUUID(), "  PLAYWRIGHT  ", "  TYPESCRIPT  ",
                List.of(), List.of(), List.of(), "root", "manifest.json",
                GenerationStatus.FAILED, GenerationConfidence.UNKNOWN, false
        );

        assertThat(result.framework()).isEqualTo("PLAYWRIGHT");
        assertThat(result.language()).isEqualTo("TYPESCRIPT");
    }

    @Test
    @DisplayName("Deve normalizar coleções nulas para vazias")
    void deveNormalizarColecoesNulasParaVazias() {
        GenerationResult result = new GenerationResult(
                UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                null, null, null, "root", "manifest.json",
                GenerationStatus.FAILED, GenerationConfidence.UNKNOWN, false
        );

        assertThat(result.files()).isEmpty();
        assertThat(result.reusedFiles()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Não deve exigir executionId no compact constructor (DTO não confiável)")
    void naoDeveExigirExecutionIdNoConstructor() {
        GenerationResult result = new GenerationResult(
                null, null, null, List.of(), List.of(), List.of(), null, null,
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );

        assertThat(result.executionId()).isNull();
    }

    @Test
    @DisplayName("Deve manter contrato JSON da resposta da IA (sem executionId/generatedRoot)")
    void deveManterContratoJsonDaIa() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "files": [],
                  "warnings": [],
                  "status": "COMPLETED",
                  "confidence": "HIGH",
                  "valid": true
                }
                """;

        GenerationResult result = mapper.readValue(json, GenerationResult.class);

        assertThat(result.status()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(result.confidence()).isEqualTo(GenerationConfidence.HIGH);
        assertThat(result.valid()).isTrue();
        assertThat(result.executionId()).isNull();
    }

    private GenerationResult sample() {
        return new GenerationResult(
                UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                List.of(), List.of(), List.of(), "root", "manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );
    }
}
