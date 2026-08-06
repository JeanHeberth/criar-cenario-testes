package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningResponseParser - Testes Unitários")
class LearningResponseParserTest {

    private final LearningResponseParser parser = new LearningResponseParser();

    @Test
    @DisplayName("Deve parsear JSON válido")
    void deveParsearJsonValido() {
        String json = """
                {"items":[],"warnings":[],"confidence":"MEDIUM","humanReviewRequired":true,"valid":true}
                """;
        LearningAiResponse response = parser.parse(json);
        assertThat(response.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Deve remover wrapper markdown conhecido")
    void deveRemoverWrapperMarkdown() {
        String json = "```json\n{\"items\":[],\"warnings\":[],\"confidence\":\"LOW\",\"humanReviewRequired\":true,\"valid\":true}\n```";
        LearningAiResponse response = parser.parse(json);
        assertThat(response.confidence()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Deve parsear item com type e scope válidos")
    void deveParsearItemComTypeEScopeValidos() {
        String json = """
                {"items":[{"type":"REUSABLE_COMPONENT","scope":"PROJECT","title":"t","description":"d","recommendation":"r","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"MEDIUM","humanReviewRequired":true,"valid":true}
                """;
        LearningAiResponse response = parser.parse(json);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).type()).isEqualTo("REUSABLE_COMPONENT");
    }

    @Test
    @DisplayName("Deve rejeitar JSON nulo")
    void deveRejeitarJsonNulo() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar JSON em branco")
    void deveRejeitarJsonEmBranco() {
        assertThatThrownBy(() -> parser.parse("   ")).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar JSON maior que o limite")
    void deveRejeitarJsonMuitoGrande() {
        String big = "a".repeat(60_000);
        assertThatThrownBy(() -> parser.parse(big)).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar JSON malformado")
    void deveRejeitarJsonMalformado() {
        assertThatThrownBy(() -> parser.parse("{isso não é json")).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar propriedade desconhecida")
    void deveRejeitarPropriedadeDesconhecida() {
        String json = """
                {"items":[],"warnings":[],"confidence":"MEDIUM","humanReviewRequired":true,"valid":true,"propriedadeInventada":"x"}
                """;
        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar type de item com valor de enum desconhecido")
    void deveRejeitarTypeDesconhecido() {
        String json = """
                {"items":[{"type":"TIPO_INVENTADO","scope":"PROJECT","title":"t","description":"d","recommendation":"r","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"MEDIUM","humanReviewRequired":true,"valid":true}
                """;
        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar scope de item com valor de enum desconhecido")
    void deveRejeitarScopeDesconhecido() {
        String json = """
                {"items":[{"type":"REUSABLE_COMPONENT","scope":"ESCOPO_INVENTADO","title":"t","description":"d","recommendation":"r","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"MEDIUM","humanReviewRequired":true,"valid":true}
                """;
        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(LearningParseException.class);
    }

    @Test
    @DisplayName("LearningAiResponse não deve possuir campo de JSON bruto")
    void naoDeveArmazenarJsonBruto() {
        assertThat(LearningAiResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("raw") || c.getName().toLowerCase().contains("json"));
    }
}
