package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewCategory;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeReviewPromptFactory - Testes Unitários")
class CodeReviewPromptFactoryTest {

    private CodeReviewPromptFactory factory;
    private CodeReviewInputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        factory = new CodeReviewPromptFactory();
        sanitizer = new CodeReviewInputSanitizer();
    }

    @Test
    @DisplayName("Deve proibir código corrigido no system prompt")
    void deveProibirCodigoCorrigido() {
        assertThat(factory.createSystemPrompt()).contains("Não retornar código corrigido");
    }

    @Test
    @DisplayName("Deve proibir diff/patch no system prompt")
    void deveProibirDiff() {
        assertThat(factory.createSystemPrompt()).contains("diff, patch");
    }

    @Test
    @DisplayName("Deve proibir issue inventada (sem evidência)")
    void deveProibirIssueInventada() {
        assertThat(factory.createSystemPrompt()).contains("Não inventar linhas sem evidência");
    }

    @Test
    @DisplayName("Deve solicitar JSON sem Markdown")
    void deveSolicitarJsonSemMarkdown() {
        assertThat(factory.createSystemPrompt()).contains("SOMENTE JSON válido, sem Markdown");
        assertThat(factory.createSystemPrompt()).contains("```json");
    }

    @Test
    @DisplayName("Deve incluir schema no system prompt")
    void deveIncluirSchema() {
        String prompt = factory.createSystemPrompt();
        assertThat(prompt).contains("\"files\"");
        assertThat(prompt).contains("\"humanReviewRequired\"");
    }

    @Test
    @DisplayName("Deve proibir remoção/redução de severidade de issue estática")
    void deveProibirReducaoDeSeveridade() {
        assertThat(factory.createSystemPrompt()).contains("remover ou reduzir a severidade");
    }

    @Test
    @DisplayName("Deve incluir framework no user prompt")
    void deveIncluirFramework() {
        assertThat(userPrompt()).contains("PLAYWRIGHT");
    }

    @Test
    @DisplayName("Deve incluir linguagem no user prompt")
    void deveIncluirLinguagem() {
        assertThat(userPrompt()).contains("TYPESCRIPT");
    }

    @Test
    @DisplayName("Deve incluir plano no user prompt")
    void deveIncluirPlano() {
        assertThat(userPrompt()).contains("Plano técnico aprovado");
    }

    @Test
    @DisplayName("Deve incluir issues estáticas no user prompt")
    void deveIncluirIssuesEstaticas() {
        assertThat(userPrompt()).contains("FRAGILE_SELECTOR");
    }

    @Test
    @DisplayName("Deve incluir arquivos gerados no user prompt")
    void deveIncluirArquivosGerados() {
        assertThat(userPrompt()).contains("tests/login.spec.ts");
        assertThat(userPrompt()).contains("Arquivo: tests/login.spec.ts");
    }

    @Test
    @DisplayName("Não deve incluir projectPath no user prompt")
    void deveNaoIncluirProjectPath() {
        assertThat(userPrompt()).doesNotContain("/project\"");
        assertThat(userPrompt()).doesNotContain("normalizedProjectPath");
    }

    @Test
    @DisplayName("Não deve incluir credenciais em claro no user prompt")
    void deveNaoIncluirCredenciais() {
        String prompt = userPromptComSegredo();
        assertThat(prompt).doesNotContain("SenhaSuperSecreta123");
        assertThat(prompt).contains("[REDACTED]");
    }

    private String userPrompt() {
        var artifact = new GeneratedArtifactReader.ReadArtifact("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT, "hash", true);
        var issue = new ReviewIssue("FRAGILE_SELECTOR", ReviewCategory.MAINTAINABILITY, ReviewSeverity.MEDIUM,
                "tests/login.spec.ts", null, "Seletor frágil", null, "Usar seletor semântico", false);
        SanitizedCodeReviewInput input = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)),
                List.of(artifact), List.of(issue)
        );
        return factory.createUserPrompt(input);
    }

    private String userPromptComSegredo() {
        var artifact = new GeneratedArtifactReader.ReadArtifact("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, "const password = \"SenhaSuperSecreta123\";", "hash", true);
        SanitizedCodeReviewInput input = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge(),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)),
                List.of(artifact), List.of()
        );
        return factory.createUserPrompt(input);
    }
}
