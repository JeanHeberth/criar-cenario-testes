package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioAnalysisPromptFactory - Testes Unitários")
class ScenarioAnalysisPromptFactoryTest {

    private final ScenarioAnalysisPromptFactory factory = new ScenarioAnalysisPromptFactory();

    @Test
    @DisplayName("Deve incluir cenário")
    void deveIncluirCenario() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt).contains("Login válido");
    }

    @Test
    @DisplayName("Deve instruir a não perguntar o canal quando ele foi informado")
    void deveIncluirCanalInformado() {
        String prompt = factory.createUserPrompt("Login válido", discovery(),
                com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType.API);

        assertThat(prompt).contains("Canal de automação DEFINIDO pelo usuário: API");
        assertThat(prompt).contains("não registre ambiguidade");
    }

    @Test
    @DisplayName("Sem canal informado, o prompt não menciona canal definido")
    void deveOmitirCanalQuandoNaoInformado() {
        assertThat(factory.createUserPrompt("Login válido", discovery(), null))
                .doesNotContain("Canal de automação DEFINIDO");
        assertThat(factory.createUserPrompt("Login válido", discovery(),
                com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType.UNKNOWN))
                .doesNotContain("Canal de automação DEFINIDO");
    }

    @Test
    @DisplayName("Deve incluir framework descoberto")
    void deveIncluirFrameworkDescoberto() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt).contains("PLAYWRIGHT");
    }

    @Test
    @DisplayName("Deve incluir linguagem")
    void deveIncluirLinguagem() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt).contains("TYPESCRIPT");
    }

    @Test
    @DisplayName("Deve incluir testing frameworks")
    void deveIncluirTestingFrameworks() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt).contains("PLAYWRIGHT_TEST");
    }

    @Test
    @DisplayName("Deve incluir schema JSON")
    void deveIncluirSchemaJson() {
        String prompt = factory.createSystemPrompt();

        assertThat(prompt).contains("\"title\"");
        assertThat(prompt).contains("\"steps\"");
        assertThat(prompt).contains("\"automationType\"");
    }

    @Test
    @DisplayName("Deve proibir geração de código")
    void deveProibirGeracaoDeCodigo() {
        String prompt = factory.createSystemPrompt();

        assertThat(prompt.toLowerCase()).contains("não gerar código");
    }

    @Test
    @DisplayName("Deve orientar classificação de segredos")
    void deveOrientarClassificacaoDeSegredos() {
        String prompt = factory.createSystemPrompt();

        assertThat(prompt).contains("SECRET");
    }

    @Test
    @DisplayName("Deve não incluir projectPath")
    void deveNaoIncluirProjectPath() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt).doesNotContain("/projeto");
    }

    @Test
    @DisplayName("Deve não incluir credenciais")
    void deveNaoIncluirCredenciais() {
        String prompt = factory.createUserPrompt("Login válido", discovery());

        assertThat(prompt.toLowerCase()).doesNotContain("apikey");
    }

    @Test
    @DisplayName("Deve solicitar JSON sem Markdown")
    void deveSolicitarJsonSemMarkdown() {
        String prompt = factory.createSystemPrompt();

        assertThat(prompt).contains("não usar blocos");
        assertThat(prompt).contains("JSON válido");
    }

    private ProjectDiscoveryResult discovery() {
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
}
