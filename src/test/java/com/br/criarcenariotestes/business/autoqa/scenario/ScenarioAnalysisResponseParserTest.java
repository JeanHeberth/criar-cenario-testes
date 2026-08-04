package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScenarioAnalysisResponseParser - Testes Unitários")
class ScenarioAnalysisResponseParserTest {

    private final ScenarioAnalysisResponseParser parser = new ScenarioAnalysisResponseParser(new ObjectMapper());

    @Test
    @DisplayName("Deve parsear JSON válido")
    void deveParsearJsonValido() {
        ScenarioAnalysisResult result = parser.parse(validJson());

        assertThat(result.title()).isEqualTo("Login válido");
        assertThat(result.steps()).hasSize(1);
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarRespostaNula() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(ScenarioAnalysisParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar resposta vazia")
    void deveRejeitarRespostaVazia() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(ScenarioAnalysisParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar JSON inválido")
    void deveRejeitarJsonInvalido() {
        assertThatThrownBy(() -> parser.parse("{ invalid }"))
                .isInstanceOf(ScenarioAnalysisParseException.class);
    }

    @Test
    @DisplayName("Deve remover wrapper json conhecido")
    void deveRemoverWrapperJsonConhecido() {
        ScenarioAnalysisResult result = parser.parse("```json\n" + validJson() + "\n```");

        assertThat(result.title()).isEqualTo("Login válido");
    }

    @Test
    @DisplayName("Deve rejeitar resposta acima do limite")
    void deveRejeitarRespostaAcimaDoLimite() {
        StringBuilder builder = new StringBuilder("a");
        builder.append("x".repeat(80_001));

        assertThatThrownBy(() -> parser.parse(builder.toString()))
                .isInstanceOf(ScenarioAnalysisParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar campo desconhecido")
    void deveRejeitarCampoDesconhecido() {
        String json = validJson().replaceFirst("\\{", "{\"extra\":1,");

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(ScenarioAnalysisParseException.class);
    }

    @Test
    @DisplayName("Deve preservar textos em português")
    void devePreservarTextosEmPortugues() {
        ScenarioAnalysisResult result = parser.parse(validJson());

        assertThat(result.objective()).contains("Validar");
    }

    private String validJson() {
        return """
                {
                  "title": "Login válido",
                  "objective": "Validar acesso",
                  "preconditions": ["Usuário cadastrado"],
                  "steps": [
                    {
                      "order": 1,
                      "action": "Acessar a tela de login",
                      "expectedResult": "A tela é exibida",
                      "dependencies": []
                    }
                  ],
                  "testData": [],
                  "businessRules": [],
                  "risks": [],
                  "ambiguities": [],
                  "entities": ["Usuário"],
                  "dependencies": [],
                  "automationType": "WEB_UI",
                  "status": "VALID",
                  "warnings": [],
                  "valid": true
                }
                """;
    }
}
