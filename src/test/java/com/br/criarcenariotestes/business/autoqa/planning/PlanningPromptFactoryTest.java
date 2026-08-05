package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.discovery.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanningPromptFactory - Testes Unitários")
class PlanningPromptFactoryTest {

    private PlanningPromptFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PlanningPromptFactory();
    }

    @Test
    @DisplayName("Deve incluir framework no prompt")
    void deveIncluirFrameworkNoPrompt() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.COMPLETE);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("PLAYWRIGHT");
    }

    @Test
    @DisplayName("Deve incluir linguagem no prompt")
    void deveIncluirLinguagemNoPrompt() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.COMPLETE);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("TYPESCRIPT");
    }

    @Test
    @DisplayName("Deve incluir título do cenário")
    void deveIncluirTituloDoScenario() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.COMPLETE);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("Login válido");
    }

    @Test
    @DisplayName("Deve incluir caminhos dos componentes")
    void deveIncluirCaminhosDosComponentes() {
        SanitizedPlanningInput input = buildInputWithComponents();
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("pages/LoginPage.ts");
    }

    @Test
    @DisplayName("Deve incluir candidatos de reutilização")
    void deveIncluirCandidatosDeReutilizacao() {
        SanitizedPlanningInput input = buildInputWithCandidates();
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("pages/LoginPage.ts");
        assertThat(prompt).contains("confiança=");
    }

    @Test
    @DisplayName("Deve incluir schema JSON no system prompt")
    void deveIncluirSchemaJsonNoSystemPrompt() {
        String systemPrompt = factory.createSystemPrompt();
        assertThat(systemPrompt).contains("fileActions");
        assertThat(systemPrompt).contains("reuseDecisions");
        assertThat(systemPrompt).contains("status");
        assertThat(systemPrompt).contains("confidence");
    }

    @Test
    @DisplayName("Deve proibir código no system prompt")
    void deveProibirCodigoNoSystemPrompt() {
        String systemPrompt = factory.createSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("código");
        assertThat(systemPrompt).containsIgnoringCase("imports");
    }

    @Test
    @DisplayName("Deve proibir DELETE no system prompt")
    void deveProibirDeleteNoSystemPrompt() {
        String systemPrompt = factory.createSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("DELETE");
    }

    @Test
    @DisplayName("Deve exigir approvalRequirement no system prompt")
    void deveExigirAprovacaoNoSystemPrompt() {
        String systemPrompt = factory.createSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("approvalRequirement");
    }

    @Test
    @DisplayName("Deve priorizar reutilização no system prompt")
    void devePriorizarReutilizacaoNoSystemPrompt() {
        String systemPrompt = factory.createSystemPrompt();
        assertThat(systemPrompt).containsIgnoringCase("REUSE");
    }

    @Test
    @DisplayName("Deve não incluir project path absoluto no prompt")
    void deveNaoIncluirProjectPath() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.COMPLETE);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).doesNotContain("/project");
        assertThat(prompt).doesNotContain("normalizedProjectPath");
    }

    @Test
    @DisplayName("Deve solicitar JSON sem Markdown")
    void deveSolicitarJsonSemMarkdown() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.COMPLETE);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).contains("JSON");
        assertThat(prompt).containsIgnoringCase("Markdown");
    }

    @Test
    @DisplayName("Deve instruir warnings para knowledge PARTIAL")
    void deveInstruirWarningsParaKnowledgePartial() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.PARTIAL);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).containsIgnoringCase("PARTIAL");
        assertThat(prompt).containsIgnoringCase("warnings");
    }

    @Test
    @DisplayName("Deve instruir warnings para knowledge EMPTY")
    void deveInstruirWarningsParaKnowledgeEmpty() {
        SanitizedPlanningInput input = buildInput(KnowledgeStatus.EMPTY);
        String prompt = factory.createUserPrompt(input);
        assertThat(prompt).containsIgnoringCase("EMPTY");
    }

    // --- helpers ---

    private SanitizedPlanningInput buildInput(KnowledgeStatus knowledgeStatus) {
        return new SanitizedPlanningInput(
            AutomationFramework.PLAYWRIGHT,
            AutomationLanguage.TYPESCRIPT,
            BuildTool.NPM,
            PackageManager.NPM,
            List.of("PLAYWRIGHT_TEST"),
            List.of("PLAYWRIGHT"),
            DiscoveryConfidence.HIGH,
            "Login válido",
            "Validar acesso",
            List.of("Usuário cadastrado"),
            List.of(new SanitizedPlanningInput.SanitizedStep(1, "Acessar login", "Tela exibida")),
            List.of("Usuário"),
            List.of(),
            List.of(),
            List.of(),
            ScenarioAnalysisStatus.VALID,
            knowledgeStatus,
            new NamingConvention("*.ts", "*.spec.ts", "camelCase", "camelCase", "tests", List.of(), ReuseConfidence.HIGH),
            List.of(),
            List.of(),
            List.of(),
            List.of("src"),
            List.of("tests")
        );
    }

    private SanitizedPlanningInput buildInputWithComponents() {
        return new SanitizedPlanningInput(
            AutomationFramework.PLAYWRIGHT,
            AutomationLanguage.TYPESCRIPT,
            BuildTool.NPM,
            PackageManager.NPM,
            List.of(),
            List.of(),
            DiscoveryConfidence.HIGH,
            "Login válido",
            "Validar acesso",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            ScenarioAnalysisStatus.VALID,
            KnowledgeStatus.COMPLETE,
            new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
            List.of(new SanitizedPlanningInput.SanitizedComponent("pages/LoginPage.ts", "PAGE_OBJECT", "LoginPage")),
            List.of(),
            List.of(),
            List.of("src"),
            List.of("tests")
        );
    }

    private SanitizedPlanningInput buildInputWithCandidates() {
        return new SanitizedPlanningInput(
            AutomationFramework.PLAYWRIGHT,
            AutomationLanguage.TYPESCRIPT,
            BuildTool.NPM,
            PackageManager.NPM,
            List.of(),
            List.of(),
            DiscoveryConfidence.HIGH,
            "Login válido",
            "Validar acesso",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            ScenarioAnalysisStatus.VALID,
            KnowledgeStatus.COMPLETE,
            new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
            List.of(),
            List.of(new SanitizedPlanningInput.SanitizedCandidate("pages/LoginPage.ts", "PAGE_OBJECT", "HIGH", List.of("login"))),
            List.of(),
            List.of("src"),
            List.of("tests")
        );
    }
}
