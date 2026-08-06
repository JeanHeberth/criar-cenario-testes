package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CodeReviewResponseParser - Testes Unitários")
class CodeReviewResponseParserTest {

    private CodeReviewResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new CodeReviewResponseParser(new ObjectMapper());
    }

    @Test
    @DisplayName("Deve parsear JSON válido para CodeReviewAiResponse")
    void deveParsearJsonValido() {
        CodeReviewAiResponse result = parser.parse(validJson());
        assertThat(result.files()).hasSize(1);
        assertThat(result.status()).isEqualTo(ReviewStatus.APPROVED_WITH_WARNINGS);
        assertThat(result.humanReviewRequired()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarNull() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(CodeReviewParseException.class)
                .hasMessageContaining("nula");
    }

    @Test
    @DisplayName("Deve rejeitar resposta vazia/blank")
    void deveRejeitarBlank() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(CodeReviewParseException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("Deve rejeitar JSON inválido")
    void deveRejeitarJsonInvalido() {
        assertThatThrownBy(() -> parser.parse("{invalid}"))
                .isInstanceOf(CodeReviewParseException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    @DisplayName("Deve remover wrapper Markdown conhecido")
    void deveRemoverWrapperConhecido() {
        CodeReviewAiResponse result = parser.parse("```json\n" + validJson() + "\n```");
        assertThat(result.files()).hasSize(1);
    }

    @Test
    @DisplayName("Deve rejeitar campo desconhecido")
    void deveRejeitarCampoDesconhecido() {
        String json = """
                {
                  "files": [],
                  "globalIssues": [],
                  "suggestions": [],
                  "passedRules": [],
                  "skippedRules": [],
                  "warnings": [],
                  "status": "APPROVED",
                  "confidence": "HIGH",
                  "humanReviewRequired": false,
                  "valid": true,
                  "unknownField": "x"
                }
                """;
        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(CodeReviewParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar enum desconhecido")
    void deveRejeitarEnumDesconhecido() {
        String json = """
                {
                  "files": [],
                  "globalIssues": [],
                  "suggestions": [],
                  "passedRules": [],
                  "skippedRules": [],
                  "warnings": [],
                  "status": "SOMETHING_ELSE",
                  "confidence": "HIGH",
                  "humanReviewRequired": false,
                  "valid": true
                }
                """;
        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(CodeReviewParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar resposta acima do limite")
    void deveRejeitarRespostaAcimaDoLimite() {
        String tooLong = "x".repeat(CodeReviewResponseParser.MAX_RESPONSE_LENGTH + 1);
        assertThatThrownBy(() -> parser.parse(tooLong))
                .isInstanceOf(CodeReviewParseException.class)
                .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("Não deve inventar campo (executionId não existe no DTO da IA)")
    void deveNaoInventarCampo() {
        CodeReviewAiResponse result = parser.parse(validJson());
        assertThat(result.getClass().getDeclaredFields()).extracting("name").doesNotContain("executionId");
    }

    @Test
    @DisplayName("Deve preservar textos em português")
    void devePreservarTextosEmPortugues() {
        String json = """
                {
                  "files": [],
                  "globalIssues": [],
                  "suggestions": [],
                  "passedRules": [],
                  "skippedRules": [],
                  "warnings": [{"code": "W1", "description": "Aviso em português com acentuação", "blocking": false}],
                  "status": "APPROVED",
                  "confidence": "HIGH",
                  "humanReviewRequired": false,
                  "valid": true
                }
                """;
        CodeReviewAiResponse result = parser.parse(json);
        assertThat(result.warnings().get(0).description()).isEqualTo("Aviso em português com acentuação");
    }

    private String validJson() {
        return """
                {
                  "files": [
                    {
                      "relativePath": "tests/login.spec.ts",
                      "status": "APPROVED_WITH_WARNINGS",
                      "issues": [],
                      "suggestions": [],
                      "passedRules": [],
                      "skippedRules": [],
                      "confidence": "HIGH",
                      "valid": true
                    }
                  ],
                  "globalIssues": [],
                  "suggestions": [],
                  "passedRules": [],
                  "skippedRules": [],
                  "warnings": [],
                  "status": "APPROVED_WITH_WARNINGS",
                  "confidence": "HIGH",
                  "humanReviewRequired": true,
                  "valid": true
                }
                """;
    }
}
