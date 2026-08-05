package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationValidator - Testes Unitários")
class GenerationValidatorTest {

    private GenerationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GenerationValidator();
    }

    @Test
    @DisplayName("Deve validar Playwright com sucesso")
    void deveValidarPlaywright() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        GenerationResult validated = validate(result, GenerationTestData.playwrightDiscovery(), plan);
        assertThat(validated).isSameAs(result);
    }

    @Test
    @DisplayName("Deve validar Cypress com sucesso")
    void deveValidarCypress() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("cypress/e2e/login.cy.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("cypress/e2e/login.cy.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.CYPRESS_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThat(validate(result, GenerationTestData.cypressDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve validar Selenide com sucesso")
    void deveValidarSelenide() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("src/test/java/LoginTest.java", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("src/test/java/LoginTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.SELENIDE_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThat(validate(result, GenerationTestData.selenideDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve validar Selenium com sucesso")
    void deveValidarSelenium() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("src/test/java/LoginTest.java", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("src/test/java/LoginTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.SELENIUM_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThat(validate(result, GenerationTestData.seleniumDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve validar RestAssured com sucesso")
    void deveValidarRestAssured() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("src/test/java/LoginApiTest.java", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("src/test/java/LoginApiTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.REST_ASSURED_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThat(validate(result, GenerationTestData.restAssuredDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve validar Robot Framework com sucesso")
    void deveValidarRobot() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.robot", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.robot", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.ROBOT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThat(validate(result, GenerationTestData.robotDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve rejeitar framework UNKNOWN")
    void deveRejeitarFrameworkUnknown() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var result = GenerationTestData.aiResult(GenerationStatus.FAILED, GenerationConfidence.UNKNOWN, false);

        assertThatThrownBy(() -> validate(result, GenerationTestData.unknownDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar framework não suportado (ex.: Appium)")
    void deveRejeitarFrameworkNaoSuportado() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var result = GenerationTestData.aiResult(GenerationStatus.FAILED, GenerationConfidence.UNKNOWN, false);

        assertThatThrownBy(() -> validate(result, GenerationTestData.appiumDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo não planejado")
    void deveRejeitarArquivoNaoPlanejado() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/outro.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class)
                .hasMessageContaining("não planejado");
    }

    @Test
    @DisplayName("Deve rejeitar ação planejada omitida sem warning")
    void deveRejeitarArquivoPlanejadoOmitidoSemWarning() {
        var plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST),
                GenerationTestData.createAction("tests/logout.spec.ts", PlanComponentType.TEST)
        );
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve permitir omissão quando status é PARTIAL com warning")
    void devePermitirOmissaoComPartialEWarning() {
        var plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST),
                GenerationTestData.createAction("tests/logout.spec.ts", PlanComponentType.TEST)
        );
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = new GenerationResult(null, null, null, List.of(file), List.of(),
                List.of(new GenerationWarning("PARTIAL_GENERATION", "Não foi possível gerar logout", false)),
                null, null, GenerationStatus.PARTIAL, GenerationConfidence.MEDIUM, false);

        assertThat(validate(result, GenerationTestData.playwrightDiscovery(), plan)).isSameAs(result);
    }

    @Test
    @DisplayName("Deve rejeitar operação diferente do plano")
    void deveRejeitarOperacaoDiferenteDoPlano() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.updateAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path duplicado")
    void deveRejeitarPathDuplicado() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file1 = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var file2 = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file1, file2);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    @DisplayName("Deve rejeitar path traversal")
    void deveRejeitarPathTraversal() {
        assertRejeitaPath("../tests/login.spec.ts");
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto Unix")
    void deveRejeitarPathAbsolutoUnix() {
        assertRejeitaPath("/etc/passwd.ts");
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto Windows")
    void deveRejeitarPathAbsolutoWindows() {
        assertRejeitaPath("C:\\tests\\login.spec.ts");
    }

    @Test
    @DisplayName("Deve rejeitar UNC")
    void deveRejeitarUnc() {
        assertRejeitaPath("\\\\server\\share\\login.spec.ts");
    }

    @Test
    @DisplayName("Deve rejeitar file URI")
    void deveRejeitarFileUri() {
        assertRejeitaPath("file:///tests/login.spec.ts");
    }

    private void assertRejeitaPath(String path) {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction(path, PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile(path, GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar Markdown fence no content")
    void deveRejeitarMarkdownFence() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "```typescript\n" + GenerationTestData.PLAYWRIGHT_CONTENT + "\n```", null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar múltiplos arquivos Java concatenados (dois package)")
    void deveRejeitarMultiplosArquivosConcatenados() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("src/test/java/LoginTest.java", PlanComponentType.TEST));
        String concatenated = GenerationTestData.SELENIDE_CONTENT + "\n" + GenerationTestData.SELENIUM_CONTENT;
        var file = GenerationTestData.generatedFile("src/test/java/LoginTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                concatenated, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.selenideDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo vazio")
    void deveRejeitarConteudoVazio() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "   ", null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar extensão incompatível com o framework")
    void deveRejeitarExtensaoIncompativel() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.py", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.py", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar Playwright com evidência de Cypress")
    void deveRejeitarPlaywrightComCypress() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "import { test, expect } from '@playwright/test';\ncy.visit('/login');\n", null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar Cypress com evidência de Playwright")
    void deveRejeitarCypressComPlaywright() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("cypress/e2e/login.cy.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("cypress/e2e/login.cy.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "import { test } from '@playwright/test';\ndescribe('login', () => { it('ok', () => { cy.visit('/login'); }); });\n", null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.cypressDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar reusedComponents inexistente no knowledge")
    void deveRejeitarReuseInexistente() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, "UTF-8", "hash", null, false,
                List.of("pages/Inexistente.ts"), List.of(), List.of());
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status incoerente (FAILED com arquivos)")
    void deveRejeitarStatusIncoerente() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.FAILED, GenerationConfidence.LOW, false, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar elemento nulo em files")
    void deveRejeitarElementoNulo() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        List<GeneratedFile> files = new java.util.ArrayList<>();
        files.add(null);
        var result = new GenerationResult(null, null, null, files, List.of(), List.of(), null, null,
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve respeitar limite de tamanho por arquivo")
    void deveRespeitarLimitePorArquivo() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        String huge = "// import { test } from '@playwright/test';\n" + "x".repeat(GenerationValidator.MAX_CONTENT_LENGTH + 1);
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                huge, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        assertThatThrownBy(() -> validate(result, GenerationTestData.playwrightDiscovery(), plan))
                .isInstanceOf(GenerationValidationException.class);
    }

    @Test
    @DisplayName("Deve retornar a mesma instância recebida")
    void deveRetornarMesmaInstancia() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        GenerationResult validated = validate(result, GenerationTestData.playwrightDiscovery(), plan);

        assertThat(validated).isSameAs(result);
        assertThat(validated.status()).isEqualTo(result.status());
        assertThat(validated.confidence()).isEqualTo(result.confidence());
    }

    @Test
    @DisplayName("Deve não modificar o resultado (não preenche status GENERATED por arquivo)")
    void deveNaoModificarResultado() {
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var result = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);

        validate(result, GenerationTestData.playwrightDiscovery(), plan);

        assertThat(result.files().get(0).status()).isNull();
        assertThat(result.files().get(0).content()).isEqualTo(GenerationTestData.PLAYWRIGHT_CONTENT);
    }

    private GenerationResult validate(GenerationResult result, ProjectDiscoveryResult discovery, TechnicalPlanResult plan) {
        return validator.validate(result, discovery, GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), plan);
    }
}
