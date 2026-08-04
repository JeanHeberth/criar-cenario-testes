package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.BusinessRule;
import com.br.criarcenariotestes.business.autoqa.model.scenario.RiskLevel;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAmbiguity;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioRisk;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataRequirement;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScenarioAnalysisValidatorSafety - Testes Unitários")
class ScenarioAnalysisValidatorSafetyTest {

    private final ScenarioAnalysisValidator validator = new ScenarioAnalysisValidator();

    @Test
    @DisplayName("Deve rejeitar caminho Unix")
    void deveRejeitarCaminhoUnix() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(List.of("/Users/jean/projeto"))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho Windows")
    void deveRejeitarCaminhoWindows() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(List.of("C:\\Users\\Jean\\projeto"))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho Windows com barra normal")
    void deveRejeitarCaminhoWindowsComBarraNormal() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(List.of("D:/automacao/tests"))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho UNC")
    void deveRejeitarCaminhoUnc() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(List.of("\\\\servidor\\pasta"))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar file URI")
    void deveRejeitarFileUri() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(List.of("file:///Users/jean/projeto"))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar step nulo")
    void deveRejeitarStepNulo() {
        assertThatThrownBy(() -> validator.validate(ScenarioAnalysisTestData.analysisWithSteps(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar testData nulo")
    void deveRejeitarTestDataNulo() {
        assertThatThrownBy(() -> validator.validate(withTestData(null)))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar businessRule nula")
    void deveRejeitarBusinessRuleNula() {
        assertThatThrownBy(() -> validator.validate(withBusinessRules(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar risk nulo")
    void deveRejeitarRiskNulo() {
        assertThatThrownBy(() -> validator.validate(withRisks(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar ambiguity nula")
    void deveRejeitarAmbiguityNula() {
        assertThatThrownBy(() -> validator.validate(withAmbiguities(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar string nula em preconditions")
    void deveRejeitarStringNulaEmPreconditions() {
        assertThatThrownBy(() -> validator.validate(withPreconditions(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar string nula em dependencies")
    void deveRejeitarStringNulaEmDependencies() {
        assertThatThrownBy(() -> validator.validate(withDependencies(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar dependency interna nula")
    void deveRejeitarDependencyInternaNula() {
        assertThatThrownBy(() -> validator.validate(withStepDependencies(Collections.singletonList(null))))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar warnings nulos")
    void deveRejeitarWarningsNulos() {
        assertThatThrownBy(() -> validator.validate(withWarnings(null)))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar enum UNKNOWN")
    void deveRejeitarEnumUnknown() {
        ScenarioAnalysisResult result = withAutomationType(AutomationType.UNKNOWN);

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar testData UNKNOWN")
    void deveRejeitarTestDataUnknown() {
        ScenarioAnalysisResult result = withTestData(List.of(
                new TestDataRequirement("email", TestDataType.UNKNOWN, true, "E-mail", null)
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar risk level nulo")
    void deveRejeitarRiskLevelNulo() {
        ScenarioAnalysisResult result = withRisks(List.of(
                new ScenarioRisk("Instabilidade", null, "Reexecutar")
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar business rule com descrição nula")
    void deveRejeitarBusinessRuleDescricaoNula() {
        ScenarioAnalysisResult result = withBusinessRules(List.of(
                new BusinessRule("BR-001", null, true)
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar ambiguity com pergunta nula")
    void deveRejeitarAmbiguityPerguntaNula() {
        ScenarioAnalysisResult result = withAmbiguities(List.of(
                new ScenarioAmbiguity("Ambiguidade", null, false)
        ));

        assertThatThrownBy(() -> validator.validate(result))
                .isInstanceOf(ScenarioAnalysisValidationException.class);
    }

    private ScenarioAnalysisResult withPreconditions(List<String> preconditions) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                preconditions,
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withDependencies(List<String> dependencies) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                dependencies,
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withStepDependencies(List<String> dependencies) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                List.of(new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", dependencies)),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withTestData(List<TestDataRequirement> testData) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                testData,
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withBusinessRules(List<BusinessRule> businessRules) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                businessRules,
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withRisks(List<ScenarioRisk> risks) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                risks,
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withAmbiguities(List<ScenarioAmbiguity> ambiguities) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ambiguities,
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }

    private ScenarioAnalysisResult withWarnings(List<String> warnings) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID_WITH_WARNINGS,
                warnings,
                true
        );
    }

    private ScenarioAnalysisResult withAutomationType(AutomationType automationType) {
        return new ScenarioAnalysisResult(
                ScenarioAnalysisTestData.validAnalysis().title(),
                ScenarioAnalysisTestData.validAnalysis().objective(),
                ScenarioAnalysisTestData.validAnalysis().preconditions(),
                ScenarioAnalysisTestData.validAnalysis().steps(),
                ScenarioAnalysisTestData.validAnalysis().testData(),
                ScenarioAnalysisTestData.validAnalysis().businessRules(),
                ScenarioAnalysisTestData.validAnalysis().risks(),
                ScenarioAnalysisTestData.validAnalysis().ambiguities(),
                ScenarioAnalysisTestData.validAnalysis().entities(),
                ScenarioAnalysisTestData.validAnalysis().dependencies(),
                automationType,
                ScenarioAnalysisStatus.VALID,
                ScenarioAnalysisTestData.validAnalysis().warnings(),
                true
        );
    }
}
