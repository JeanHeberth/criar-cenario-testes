package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StaticReviewRuleEngine - Testes Unitários")
class StaticReviewRuleEngineTest {

    private StaticReviewRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new StaticReviewRuleEngine();
    }

    @Test
    @DisplayName("Deve detectar arquivo planejado faltante")
    void deveDetectarArquivoFaltante() {
        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        GenerationResult generation = generationWith();

        List<ReviewIssue> issues = engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, plan, generation, List.of());

        assertThat(codesOf(issues)).contains(ReviewRule.PLAN_FILE_MISSING.name());
    }

    @Test
    @DisplayName("Deve detectar arquivo extra não planejado")
    void deveDetectarArquivoExtra() {
        TechnicalPlanResult plan = GenerationTestData.readyPlan();
        GenerationResult generation = generationWith(genFile("tests/extra.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST));

        List<ReviewIssue> issues = engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, plan, generation, List.of());

        assertThat(codesOf(issues)).contains(ReviewRule.PLAN_FILE_EXTRA.name());
    }

    @Test
    @DisplayName("Deve detectar operação divergente do plano")
    void deveDetectarOperacaoDivergente() {
        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.updateAction("tests/login.spec.ts", PlanComponentType.TEST));
        GenerationResult generation = generationWith(genFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST));

        List<ReviewIssue> issues = engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, plan, generation, List.of());

        assertThat(codesOf(issues)).contains(ReviewRule.PLAN_OPERATION_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve detectar componentType divergente do plano")
    void deveDetectarComponentTypeDivergente() {
        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        GenerationResult generation = generationWith(genFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.PAGE_OBJECT));

        List<ReviewIssue> issues = engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, plan, generation, List.of());

        assertThat(codesOf(issues)).contains(ReviewRule.PLAN_COMPONENT_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve detectar extensão inválida para o framework")
    void deveDetectarExtensaoInvalida() {
        List<ReviewIssue> issues = reviewOneFile("tests/login.py", PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT);
        assertThat(codesOf(issues)).contains(ReviewRule.INVALID_EXTENSION.name());
    }

    @Test
    @DisplayName("Deve detectar Markdown fence no conteúdo")
    void deveDetectarMarkdownFence() {
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST,
                "```typescript\n" + GenerationTestData.PLAYWRIGHT_CONTENT + "\n```");
        assertThat(codesOf(issues)).contains(ReviewRule.MARKDOWN_FENCE.name());
    }

    @Test
    @DisplayName("Deve detectar segredo hardcoded")
    void deveDetectarSegredo() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nconst password = \"SuperSecreta123\";\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.HARDCODED_SECRET.name());
        assertThat(issues.stream().filter(i -> i.code().equals(ReviewRule.HARDCODED_SECRET.name())))
                .allMatch(i -> i.severity() == ReviewSeverity.CRITICAL && i.blocking());
    }

    @Test
    @DisplayName("Deve detectar credencial hardcoded via Bearer token")
    void deveDetectarCredencial() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\n// Authorization: Bearer abcdef123456\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.HARDCODED_CREDENTIAL.name());
    }

    @Test
    @DisplayName("Deve detectar credencial hardcoded via URL com usuário/senha")
    void deveDetectarCredencialViaUrl() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nconst url = 'https://user:pass@example.com';\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.HARDCODED_CREDENTIAL.name());
    }

    @Test
    @DisplayName("Deve detectar URL hardcoded (sem credencial)")
    void deveDetectarUrlHardcoded() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nawait page.goto('https://meusite.com/login');\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.HARDCODED_URL.name());
    }

    @Test
    @DisplayName("Deve detectar path absoluto no relativePath")
    void deveDetectarPathAbsoluto() {
        List<ReviewIssue> issues = reviewOneFile("/etc/login.spec.ts", PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT);
        assertThat(codesOf(issues)).contains(ReviewRule.ABSOLUTE_PATH.name());
        assertThat(issues.stream().filter(i -> i.code().equals(ReviewRule.ABSOLUTE_PATH.name())))
                .allMatch(i -> i.severity() == ReviewSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Deve detectar path traversal")
    void deveDetectarPathTraversal() {
        List<ReviewIssue> issues = reviewOneFile("../login.spec.ts", PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT);
        assertThat(codesOf(issues)).contains(ReviewRule.PATH_TRAVERSAL.name());
    }

    @Test
    @DisplayName("Deve detectar framework incorreto (evidência ausente)")
    void deveDetectarFrameworkIncorreto() {
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, "const x = 1;");
        assertThat(codesOf(issues)).contains(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve detectar framework incompatível (evidência de outro framework)")
    void deveDetectarFrameworkIncompativelPorEvidenciaProibida() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\ncy.visit('/login');\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve detectar linguagem incorreta (extensão incompatível)")
    void deveDetectarLinguagemIncorreta() {
        List<ReviewIssue> issues = engine.review(AutomationFramework.SELENIDE, AutomationLanguage.JAVA,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/LoginTest.java", PlanComponentType.TEST)),
                generationWith(genFile("tests/LoginTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST)),
                List.of(artifact("tests/LoginTest.java", PlanComponentType.TEST, GenerationTestData.SELENIDE_CONTENT)));
        // Selenide/Java é compatível — não deve haver LANGUAGE_MISMATCH aqui.
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.LANGUAGE_MISMATCH.name());

        List<ReviewIssue> incompativel = engine.review(AutomationFramework.SELENIDE, AutomationLanguage.PYTHON,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/LoginTest.java", PlanComponentType.TEST)),
                generationWith(genFile("tests/LoginTest.java", GeneratedFileOperation.CREATE, PlanComponentType.TEST)),
                List.of(artifact("tests/LoginTest.java", PlanComponentType.TEST, GenerationTestData.SELENIDE_CONTENT)));
        assertThat(codesOf(incompativel)).contains(ReviewRule.LANGUAGE_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve detectar Thread.sleep")
    void deveDetectarThreadSleep() {
        String content = GenerationTestData.SELENIUM_CONTENT + "\nThread.sleep(2000);\n";
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.SELENIUM, AutomationLanguage.JAVA,
                "src/test/java/LoginTest.java", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.THREAD_SLEEP.name());
    }

    @Test
    @DisplayName("Deve detectar cy.wait com número fixo")
    void deveDetectarCyWaitNumerico() {
        String content = GenerationTestData.CYPRESS_CONTENT + "\ncy.wait(5000);\n";
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.CYPRESS, AutomationLanguage.TYPESCRIPT,
                "cypress/e2e/login.cy.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.FIXED_WAIT.name());
    }

    @Test
    @DisplayName("Deve detectar waitForTimeout")
    void deveDetectarWaitForTimeout() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nawait page.waitForTimeout(3000);\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.FIXED_WAIT.name());
    }

    @Test
    @DisplayName("Deve detectar Sleep fixo no Robot Framework")
    void deveDetectarSleepRobot() {
        String content = GenerationTestData.ROBOT_CONTENT + "\n    Sleep    5s\n";
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.ROBOT_FRAMEWORK, AutomationLanguage.ROBOT,
                "tests/login.robot", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.SLEEP_USAGE.name());
    }

    @Test
    @DisplayName("Deve detectar seletor CSS frágil")
    void deveDetectarSeletorFragil() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nawait page.click('.container > div > span > a');\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.FRAGILE_SELECTOR.name());
    }

    @Test
    @DisplayName("Deve detectar ausência de assertion em arquivo de teste")
    void deveDetectarAssertionAusente() {
        String content = "import { test } from '@playwright/test';\ntest('login', async ({ page }) => { await page.goto('/login'); });\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.MISSING_ASSERTION.name());
    }

    @Test
    @DisplayName("Não deve exigir assertion em arquivo que não é TEST")
    void naoDeveExigirAssertionEmPageObject() {
        String content = "import { Page } from '@playwright/test';\nexport class LoginPage { constructor(private page: Page) {} }\n";
        List<ReviewIssue> issues = reviewOneFile("pages/LoginPage.ts", PlanComponentType.PAGE_OBJECT, content);
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.MISSING_ASSERTION.name());
    }

    @Test
    @DisplayName("Deve detectar teste vazio")
    void deveDetectarTesteVazio() {
        String content = "import { test } from '@playwright/test';\ntest('login', async () => {});\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.EMPTY_TEST.name());
    }

    @Test
    @DisplayName("Deve detectar método vazio em Page Object")
    void deveDetectarMetodoVazio() {
        String content = "import { Page } from '@playwright/test';\nexport class LoginPage { login() {} }\n";
        List<ReviewIssue> issues = reviewOneFile("pages/LoginPage.ts", PlanComponentType.PAGE_OBJECT, content);
        assertThat(codesOf(issues)).contains(ReviewRule.EMPTY_METHOD.name());
    }

    @Test
    @DisplayName("Deve detectar duplicação simples de linhas")
    void deveDetectarDuplicacaoSimples() {
        String repeated = "await page.click('#botao-de-confirmacao');\n";
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + repeated.repeat(3);
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.DUPLICATED_CODE.name());
    }

    @Test
    @DisplayName("Deve detectar arquivo muito grande")
    void deveDetectarArquivoMuitoGrande() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "// comentario\n".repeat(1000);
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.FILE_TOO_LARGE.name());
    }

    @Test
    @DisplayName("Deve detectar import wildcard")
    void deveDetectarImportWildcard() {
        String content = "import org.openqa.selenium.*;\n" + GenerationTestData.SELENIUM_CONTENT;
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.SELENIUM, AutomationLanguage.JAVA,
                "src/test/java/LoginTest.java", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.WILDCARD_IMPORT.name());
    }

    @Test
    @DisplayName("Deve detectar captura de exceção genérica")
    void deveDetectarExcecaoGenerica() {
        String content = GenerationTestData.SELENIUM_CONTENT + "\ntry { } catch (Exception e) { }\n";
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.SELENIUM, AutomationLanguage.JAVA,
                "src/test/java/LoginTest.java", PlanComponentType.TEST, content);
        assertThat(codesOf(issues)).contains(ReviewRule.GENERIC_EXCEPTION.name());
    }

    @Test
    @DisplayName("Deve detectar convenção de nomenclatura divergente")
    void deveDetectarConvencaoDivergente() {
        List<ReviewIssue> issues = reviewOneFile("tests/arquivo123.ts", PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT);
        assertThat(codesOf(issues)).contains(ReviewRule.NAMING_CONVENTION_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve validar arquivo Playwright bem formado sem issues de framework")
    void deveValidarPlaywrightBemFormado() {
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT + "\nexpect(true).toBeTruthy();\n");
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name(), ReviewRule.MISSING_ASSERTION.name());
    }

    @Test
    @DisplayName("Deve validar arquivo Cypress bem formado sem issues de framework")
    void deveValidarCypressBemFormado() {
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.CYPRESS, AutomationLanguage.TYPESCRIPT,
                "cypress/e2e/login.cy.ts", PlanComponentType.TEST, GenerationTestData.CYPRESS_CONTENT + "\ncy.get('#ok').should('be.visible');\n");
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name(), ReviewRule.MISSING_ASSERTION.name());
    }

    @Test
    @DisplayName("Deve validar arquivo Selenide bem formado sem issues de framework")
    void deveValidarSelenideBemFormado() {
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.SELENIDE, AutomationLanguage.JAVA,
                "src/test/java/LoginTest.java", PlanComponentType.TEST, GenerationTestData.SELENIDE_CONTENT);
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve validar arquivo Selenium bem formado sem issues de framework")
    void deveValidarSeleniumBemFormado() {
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.SELENIUM, AutomationLanguage.JAVA,
                "src/test/java/LoginTest.java", PlanComponentType.TEST, GenerationTestData.SELENIUM_CONTENT);
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve validar arquivo RestAssured bem formado sem issues de framework")
    void deveValidarRestAssuredBemFormado() {
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.REST_ASSURED, AutomationLanguage.JAVA,
                "src/test/java/LoginApiTest.java", PlanComponentType.TEST, GenerationTestData.REST_ASSURED_CONTENT);
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Deve validar arquivo Robot Framework bem formado sem issues de framework")
    void deveValidarRobotBemFormado() {
        List<ReviewIssue> issues = reviewOneFileFramework(AutomationFramework.ROBOT_FRAMEWORK, AutomationLanguage.ROBOT,
                "tests/login.robot", PlanComponentType.TEST, GenerationTestData.ROBOT_CONTENT);
        assertThat(codesOf(issues)).doesNotContain(ReviewRule.FRAMEWORK_MISMATCH.name());
    }

    @Test
    @DisplayName("Toda issue CRITICAL deve possuir blocking=true")
    void issueCriticalDeveBloquear() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT + "\nconst password = \"outrasecreta\";\n";
        List<ReviewIssue> issues = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(issues.stream().filter(i -> i.severity() == ReviewSeverity.CRITICAL))
                .allMatch(ReviewIssue::blocking);
    }

    @Test
    @DisplayName("Não deve modificar o conteúdo do artefato")
    void naoDeveModificarConteudo() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT;
        GeneratedArtifactReader.ReadArtifact artifact = artifact("tests/login.spec.ts", PlanComponentType.TEST, content);

        engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)),
                generationWith(genFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST)),
                List.of(artifact));

        assertThat(artifact.content()).isEqualTo(content);
    }

    @Test
    @DisplayName("Engine é determinístico: mesma entrada produz mesmas issues")
    void deveSerDeterministico() {
        String content = GenerationTestData.PLAYWRIGHT_CONTENT;
        List<ReviewIssue> r1 = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        List<ReviewIssue> r2 = reviewOneFile("tests/login.spec.ts", PlanComponentType.TEST, content);
        assertThat(r1).isEqualTo(r2);
    }

    // --- helpers ---

    private List<String> codesOf(List<ReviewIssue> issues) {
        return issues.stream().map(ReviewIssue::code).toList();
    }

    private List<ReviewIssue> reviewOneFile(String path, PlanComponentType type, String content) {
        return reviewOneFileFramework(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, path, type, content);
    }

    private List<ReviewIssue> reviewOneFileFramework(AutomationFramework framework, AutomationLanguage language,
                                                       String path, PlanComponentType type, String content) {
        TechnicalPlanResult plan = GenerationTestData.readyPlan(GenerationTestData.createAction(path, type));
        GenerationResult generation = generationWith(genFile(path, GeneratedFileOperation.CREATE, type));
        return engine.review(framework, language, plan, generation, List.of(artifact(path, type, content)));
    }

    private GeneratedArtifactReader.ReadArtifact artifact(String path, PlanComponentType type, String content) {
        return new GeneratedArtifactReader.ReadArtifact(path, GeneratedFileOperation.CREATE, type, content, "hash", true);
    }

    private com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile genFile(
            String path, GeneratedFileOperation operation, PlanComponentType type) {
        return GenerationTestData.generatedFile(path, operation, type, "conteudo", null, false);
    }

    private GenerationResult generationWith(com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile... files) {
        return GenerationTestData.aiResult(
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true, files);
    }
}
