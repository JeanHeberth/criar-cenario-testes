package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class ScenarioAnalysisTestData {

    private ScenarioAnalysisTestData() {
    }

    static ScenarioAnalysisResult validAnalysis() {
        return new ScenarioAnalysisResult(
                "Login válido",
                "Validar acesso",
                List.of("Usuário cadastrado"),
                List.of(validStep()),
                List.of(validTestData()),
                List.of(validBusinessRule()),
                List.of(validRisk()),
                List.of(),
                List.of("Usuário"),
                List.of("Dependência externa"),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                List.of(),
                true
        );
    }

    static ScenarioAnalysisResult analysisWithTitle(String title) {
        return new ScenarioAnalysisResult(
                title,
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithObjective(String objective) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                objective,
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithSteps(List<ScenarioStep> steps) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                steps,
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithTestData(List<TestDataRequirement> testData) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                testData,
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithBusinessRules(List<BusinessRule> businessRules) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                businessRules,
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithRisks(List<ScenarioRisk> risks) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                risks,
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithAmbiguities(List<ScenarioAmbiguity> ambiguities) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                ambiguities,
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithPreconditions(List<String> preconditions) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                preconditions,
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithEntities(List<String> entities) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                entities,
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithDependencies(List<String> dependencies) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                dependencies,
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithWarnings(List<String> warnings) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                ScenarioAnalysisStatus.VALID_WITH_WARNINGS,
                warnings,
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithAutomationType(AutomationType automationType) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                automationType,
                validAnalysis().status(),
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithStatus(ScenarioAnalysisStatus status) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                status,
                validAnalysis().warnings(),
                validAnalysis().valid()
        );
    }

    static ScenarioAnalysisResult analysisWithValid(boolean valid) {
        return new ScenarioAnalysisResult(
                validAnalysis().title(),
                validAnalysis().objective(),
                validAnalysis().preconditions(),
                validAnalysis().steps(),
                validAnalysis().testData(),
                validAnalysis().businessRules(),
                validAnalysis().risks(),
                validAnalysis().ambiguities(),
                validAnalysis().entities(),
                validAnalysis().dependencies(),
                validAnalysis().automationType(),
                validAnalysis().status(),
                validAnalysis().warnings(),
                valid
        );
    }

    static ScenarioStep validStep() {
        return new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", List.of());
    }

    static ScenarioStep stepWithDependencies(List<String> dependencies) {
        return new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", dependencies);
    }

    static TestDataRequirement validTestData() {
        return new TestDataRequirement("email", TestDataType.ENVIRONMENT_VARIABLE, true, "E-mail", null);
    }

    static TestDataRequirement secretTestData() {
        return new TestDataRequirement("token", TestDataType.SECRET, true, "Token", null);
    }

    static BusinessRule validBusinessRule() {
        return new BusinessRule("BR-001", "Usuário deve estar ativo", true);
    }

    static ScenarioRisk validRisk() {
        return new ScenarioRisk("Instabilidade", RiskLevel.MEDIUM, "Reexecutar");
    }

    static ScenarioAmbiguity validAmbiguity(boolean blocking) {
        return new ScenarioAmbiguity("Ambiguidade", "Qual mensagem?", blocking);
    }

    static ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(AutomationFramework.PLAYWRIGHT),
                List.of("PLAYWRIGHT"),
                "playwright.config.ts",
                List.of("playwright.config.ts"),
                List.of("warning"),
                DiscoveryConfidence.HIGH,
                true
        );
    }

    static String validJson() {
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
                  "testData": [
                    {
                      "name": "email",
                      "type": "ENVIRONMENT_VARIABLE",
                      "required": true,
                      "description": "E-mail",
                      "example": null
                    }
                  ],
                  "businessRules": [
                    {
                      "identifier": "BR-001",
                      "description": "Usuário deve estar ativo",
                      "explicit": true
                    }
                  ],
                  "risks": [
                    {
                      "description": "Instabilidade",
                      "level": "MEDIUM",
                      "mitigation": "Reexecutar"
                    }
                  ],
                  "ambiguities": [],
                  "entities": ["Usuário"],
                  "dependencies": ["Dependência externa"],
                  "automationType": "WEB_UI",
                  "status": "VALID",
                  "warnings": [],
                  "valid": true
                }
                """;
    }

    static String secretScenario() {
        return "Usuário informou senha=MinhaSenha123 e token=abc123";
    }

    static String bearerScenario() {
        return "Cabeçalho Authorization: Bearer abc.def.ghi";
    }

    static String apiKeyScenario() {
        return "Chave da API api_key=super-secret";
    }

    static String urlCredentialScenario() {
        return "Conectar em https://usuario:minhaSenha@host.local/servico";
    }

    static String innocentScenario() {
        return "O texto apenas menciona senha sem valor aparente";
    }
}
