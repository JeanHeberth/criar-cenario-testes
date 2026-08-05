package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PlanningResponseParser - Testes Unitários")
class PlanningResponseParserTest {

    private PlanningResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new PlanningResponseParser(new ObjectMapper());
    }

    @Test
    @DisplayName("Deve parsear JSON válido")
    void deveParsearJsonValido() {
        TechnicalPlanResult result = parser.parse(validJson());
        assertThat(result.title()).isEqualTo("Plano de login");
        assertThat(result.strategy()).isEqualTo("Criar teste de login");
        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isNotNull();
        assertThat(result.confidence()).isNotNull();
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarRespostaNula() {
        assertThatThrownBy(() -> parser.parse(null))
            .isInstanceOf(PlanningParseException.class)
            .hasMessageContaining("nula");
    }

    @Test
    @DisplayName("Deve rejeitar resposta vazia")
    void deveRejeitarRespostaVazia() {
        assertThatThrownBy(() -> parser.parse(""))
            .isInstanceOf(PlanningParseException.class)
            .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("Deve rejeitar resposta somente com espaços")
    void deveRejeitarRespostaSomenteEspacos() {
        assertThatThrownBy(() -> parser.parse("   "))
            .isInstanceOf(PlanningParseException.class)
            .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("Deve rejeitar JSON inválido")
    void deveRejeitarJsonInvalido() {
        assertThatThrownBy(() -> parser.parse("{invalid json}"))
            .isInstanceOf(PlanningParseException.class)
            .hasMessageContaining("JSON");
    }

    @Test
    @DisplayName("Deve remover wrapper Markdown json")
    void deveRemoverWrapperMarkdownJson() {
        String wrapped = "```json\n" + validJson() + "\n```";
        TechnicalPlanResult result = parser.parse(wrapped);
        assertThat(result.title()).isEqualTo("Plano de login");
    }

    @Test
    @DisplayName("Deve remover wrapper Markdown simples")
    void deveRemoverWrapperMarkdownSimples() {
        String wrapped = "```\n" + validJson() + "\n```";
        TechnicalPlanResult result = parser.parse(wrapped);
        assertThat(result.title()).isEqualTo("Plano de login");
    }

    @Test
    @DisplayName("Deve rejeitar resposta acima do limite")
    void deveRejeitarRespostaAcimaDoLimite() {
        String tooLong = "x".repeat(PlanningResponseParser.MAX_RESPONSE_LENGTH + 1);
        assertThatThrownBy(() -> parser.parse(tooLong))
            .isInstanceOf(PlanningParseException.class)
            .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("Deve rejeitar campo desconhecido")
    void deveRejeitarCampoDesconhecido() {
        String json = """
            {
              "title": "Plano",
              "strategy": "Estratégia",
              "fileActions": [],
              "components": [],
              "reuseDecisions": [],
              "risks": [],
              "warnings": [],
              "assumptions": [],
              "constraints": [],
              "requiredApprovals": [],
              "status": "READY",
              "confidence": "HIGH",
              "valid": true,
              "unknownField": "should fail"
            }
            """;
        assertThatThrownBy(() -> parser.parse(json))
            .isInstanceOf(PlanningParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar enum desconhecido (DELETE não existe em FileOperation)")
    void deveRejeitarEnumDesconhecido() {
        String json = """
            {
              "title": "Plano",
              "strategy": "Estratégia",
              "fileActions": [
                {
                  "relativePath": "tests/x.ts",
                  "operation": "DELETE",
                  "componentType": "TEST",
                  "reason": "Razão",
                  "existingFile": false,
                  "required": true,
                  "approvalRequirement": "NONE",
                  "dependencies": [],
                  "warnings": []
                }
              ],
              "components": [],
              "reuseDecisions": [],
              "risks": [],
              "warnings": [],
              "assumptions": [],
              "constraints": [],
              "requiredApprovals": [],
              "status": "READY",
              "confidence": "HIGH",
              "valid": true
            }
            """;
        assertThatThrownBy(() -> parser.parse(json))
            .isInstanceOf(PlanningParseException.class);
    }

    @Test
    @DisplayName("Deve preservar textos em português")
    void devePreservarTextosEmPortugues() {
        String json = """
            {
              "title": "Plano de autenticação",
              "strategy": "Criar teste de validação de acesso",
              "fileActions": [],
              "components": [],
              "reuseDecisions": [],
              "risks": [],
              "warnings": [],
              "assumptions": ["Usuário cadastrado no sistema"],
              "constraints": [],
              "requiredApprovals": [],
              "status": "READY",
              "confidence": "HIGH",
              "valid": true
            }
            """;
        TechnicalPlanResult result = parser.parse(json);
        assertThat(result.title()).isEqualTo("Plano de autenticação");
        assertThat(result.assumptions()).containsExactly("Usuário cadastrado no sistema");
    }

    @Test
    @DisplayName("Deve não armazenar JSON bruto")
    void deveNaoArmazenarJsonBruto() {
        TechnicalPlanResult result = parser.parse(validJson());
        // The result is a TechnicalPlanResult record - no raw JSON field
        assertThat(result).isNotNull();
        assertThat(result.title()).isNotNull();
        // The class doesn't have a rawJson field
        assertThat(result.getClass().getDeclaredFields())
            .extracting("name")
            .doesNotContain("rawJson", "json", "raw");
    }

    @Test
    @DisplayName("Deve parsear fileActions completo")
    void deveParsearFileActionsCompleto() {
        String json = """
            {
              "title": "Plano",
              "strategy": "Estratégia",
              "fileActions": [
                {
                  "relativePath": "tests/login.spec.ts",
                  "operation": "CREATE",
                  "componentType": "TEST",
                  "reason": "Criar teste",
                  "existingFile": false,
                  "required": true,
                  "approvalRequirement": "NONE",
                  "dependencies": [],
                  "warnings": []
                }
              ],
              "components": [],
              "reuseDecisions": [],
              "risks": [],
              "warnings": [],
              "assumptions": [],
              "constraints": [],
              "requiredApprovals": [],
              "status": "READY",
              "confidence": "HIGH",
              "valid": true
            }
            """;
        TechnicalPlanResult result = parser.parse(json);
        assertThat(result.fileActions()).hasSize(1);
        assertThat(result.fileActions().get(0).relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(result.fileActions().get(0).operation()).isEqualTo(
            com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation.CREATE);
    }

    private String validJson() {
        return """
            {
              "title": "Plano de login",
              "strategy": "Criar teste de login",
              "fileActions": [],
              "components": [],
              "reuseDecisions": [],
              "risks": [],
              "warnings": [],
              "assumptions": [],
              "constraints": [],
              "requiredApprovals": [],
              "status": "READY",
              "confidence": "HIGH",
              "valid": true
            }
            """;
    }
}
