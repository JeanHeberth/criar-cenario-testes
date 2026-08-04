package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScenarioAnalysisValidator - Testes Unitários")
class ScenarioAnalysisValidatorTest {

    private final ScenarioAnalysisValidator validator = new ScenarioAnalysisValidator();

    @Test
    @DisplayName("Deve validar resultado completo")
    void deveValidarResultadoCompleto() {
        ScenarioAnalysisResult result = validator.validate(ScenarioAnalysisTestData.validAnalysis());

        assertThat(result.status()).isEqualTo(ScenarioAnalysisStatus.VALID);
    }

    @Test
    @DisplayName("Deve rejeitar título vazio")
    void deveRejeitarTituloVazio() {
        assertThatThrownBy(() -> validator.validate(ScenarioAnalysisTestData.analysisWithTitle(" ")))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar objetivo vazio")
    void deveRejeitarObjetivoVazio() {
        assertThatThrownBy(() -> validator.validate(ScenarioAnalysisTestData.analysisWithObjective(null)))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar lista de passos vazia")
    void deveRejeitarListaDePassosVazia() {
        assertThatThrownBy(() -> validator.validate(ScenarioAnalysisTestData.analysisWithSteps(List.of())))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar ordem duplicada")
    void deveRejeitarOrdemDuplicada() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithSteps(List.of(
                new ScenarioStep(1, "A", "B", List.of()),
                new ScenarioStep(1, "C", "D", List.of())
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar passos fora de ordem")
    void deveRejeitarPassosForaDeOrdem() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithSteps(List.of(
                new ScenarioStep(2, "A", "B", List.of()),
                new ScenarioStep(1, "C", "D", List.of())
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar código no resultado")
    void deveRejeitarCodigoNoResultado() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithSteps(List.of(
                new ScenarioStep(1, "public void test() {}", "B", List.of())
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar comando no resultado")
    void deveRejeitarComandoNoResultado() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithSteps(List.of(
                new ScenarioStep(1, "Executar curl http://exemplo", "B", List.of())
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar segredo aparente")
    void deveRejeitarSegredoAparente() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithTestData(List.of(
                new com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataRequirement(
                        "token",
                        com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataType.SECRET,
                        true,
                        "Token",
                        "123456"
                )
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status incoerente")
    void deveRejeitarStatusIncoerente() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithStatus(ScenarioAnalysisStatus.INVALID);

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve marcar inválido com ambiguidade bloqueante")
    void deveMarcarInvalidoComAmbiguidadeBloqueante() {
        ScenarioAnalysisResult result = ScenarioAnalysisTestData.analysisWithAmbiguities(List.of(
                ScenarioAnalysisTestData.validAmbiguity(true)
        ));
        result = ScenarioAnalysisTestData.analysisWithStatus(ScenarioAnalysisStatus.INVALID);
        result = new ScenarioAnalysisResult(
                result.title(),
                result.objective(),
                result.preconditions(),
                result.steps(),
                result.testData(),
                result.businessRules(),
                result.risks(),
                List.of(ScenarioAnalysisTestData.validAmbiguity(true)),
                result.entities(),
                result.dependencies(),
                result.automationType(),
                ScenarioAnalysisStatus.INVALID,
                result.warnings(),
                false
        );

        assertThat(validator.validate(result).status()).isEqualTo(ScenarioAnalysisStatus.INVALID);
    }

    @Test
    @DisplayName("Deve aceitar ambiguidade não bloqueante com warning")
    void deveAceitarAmbiguidadeNaoBloqueanteComWarning() {
        ScenarioAnalysisResult result = new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                List.of(ScenarioAnalysisTestData.validAmbiguity(false)),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID_WITH_WARNINGS,
                List.of("Ambiguidade"),
                true
        );

        assertThat(validator.validate(result).status()).isEqualTo(ScenarioAnalysisStatus.VALID_WITH_WARNINGS);
    }
}
