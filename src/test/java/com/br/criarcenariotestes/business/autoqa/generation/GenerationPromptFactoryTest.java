package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerationPromptFactory - Testes Unitários")
class GenerationPromptFactoryTest {

    private GenerationPromptFactory factory;
    private GenerationInputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        factory = new GenerationPromptFactory();
        sanitizer = new GenerationInputSanitizer();
    }

    @Test
    @DisplayName("Deve proibir DELETE no system prompt")
    void deveProibirDelete() {
        assertThat(factory.createSystemPrompt()).contains("Não usar a operação DELETE");
    }

    @Test
    @DisplayName("Deve proibir arquivo extra fora do plano")
    void deveProibirArquivoExtra() {
        assertThat(factory.createSystemPrompt()).contains("Não inventar arquivos que não estejam no plano");
    }

    @Test
    @DisplayName("Deve proibir código fora do JSON (Markdown)")
    void deveProibirCodigoForaDoJson() {
        assertThat(factory.createSystemPrompt()).contains("SOMENTE JSON válido, sem Markdown");
    }

    @Test
    @DisplayName("Deve exigir um item por arquivo, proibindo concatenação")
    void deveExigirUmItemPorArquivo() {
        assertThat(factory.createSystemPrompt()).contains("nunca múltiplos arquivos concatenados");
    }

    @Test
    @DisplayName("Deve solicitar JSON sem Markdown explicitamente")
    void deveSolicitarJsonSemMarkdown() {
        assertThat(factory.createSystemPrompt()).contains("```json");
    }

    @Test
    @DisplayName("Deve incluir schema no system prompt")
    void deveIncluirSchema() {
        String prompt = factory.createSystemPrompt();
        assertThat(prompt).contains("\"files\"");
        assertThat(prompt).contains("\"status\"");
        assertThat(prompt).contains("\"confidence\"");
    }

    @Test
    @DisplayName("Deve incluir framework no user prompt")
    void deveIncluirFramework() {
        String prompt = userPrompt();
        assertThat(prompt).contains("PLAYWRIGHT");
    }

    @Test
    @DisplayName("Deve incluir linguagem no user prompt")
    void deveIncluirLinguagem() {
        String prompt = userPrompt();
        assertThat(prompt).contains("TYPESCRIPT");
    }

    @Test
    @DisplayName("Deve incluir plano no user prompt")
    void deveIncluirPlano() {
        String prompt = userPrompt();
        assertThat(prompt).contains("Plano técnico aprovado");
        assertThat(prompt).contains("tests/login.spec.ts");
    }

    @Test
    @DisplayName("Deve incluir componentes reutilizáveis no user prompt")
    void deveIncluirComponentesReutilizaveis() {
        String prompt = userPrompt();
        assertThat(prompt).contains("pages/LoginPage.ts");
    }

    @Test
    @DisplayName("Não deve incluir projectPath no user prompt")
    void deveNaoIncluirProjectPath() {
        String prompt = userPrompt();
        assertThat(prompt).doesNotContain("/project\"");
        assertThat(prompt).doesNotContain("normalizedProjectPath");
    }

    @Test
    @DisplayName("Não deve vazar toString bruto de objetos de domínio")
    void deveNaoVazarToStringBrutoDeObjetosDeDominio() {
        String prompt = userPrompt();
        assertThat(prompt).doesNotContain("ProjectComponent[");
        assertThat(prompt).doesNotContain("NamingConvention[");
    }

    private String userPrompt() {
        SanitizedGenerationInput input = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );
        return factory.createUserPrompt(input);
    }
}
