package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.ClassInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.MethodInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalogEntry;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProjectAnalysisAgent")
class ProjectAnalysisAgentTest {

    private ProjectAnalysisAgent agent;

    @BeforeEach
    void setUp() {
        agent = new ProjectAnalysisAgent();
    }

    // ─── Catálogo vazio ───────────────────────────────────────────────────────

    @Test
    @DisplayName("catálogo vazio deve produzir resultado não nulo com listas vazias")
    void emptyCatalogProducesEmptyResult() {
        ProjectCatalog catalog = emptyCatalog();
        ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
        assertThat(result).isNotNull();
        assertThat(result.getClasses()).isEmpty();
        assertThat(result.getPageObjects()).isEmpty();
        assertThat(result.getTestFiles()).isEmpty();
    }

    // ─── Detecção de classes ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de classes")
    class ClassDetection {

        @Test
        @DisplayName("deve detectar declaração simples de classe")
        void detectsSimpleClass() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts",
                    "export class LoginPage {\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getClasses())
                    .extracting(ClassInfo::getName)
                    .contains("LoginPage");
        }

        @Test
        @DisplayName("deve detectar classe não exportada")
        void detectsNonExportedClass() {
            ProjectCatalog catalog = catalogWith("helpers/Utils.ts",
                    "class Utils {\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getClasses())
                    .extracting(ClassInfo::getName)
                    .contains("Utils");
        }

        @Test
        @DisplayName("deve detectar classe abstract")
        void detectsAbstractClass() {
            ProjectCatalog catalog = catalogWith("pages/BasePage.ts",
                    "export abstract class BasePage {\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getClasses())
                    .anyMatch(c -> "BasePage".equals(c.getName()));
        }

        @Test
        @DisplayName("deve detectar interface")
        void detectsInterface() {
            ProjectCatalog catalog = catalogWith("models/IUser.ts",
                    "export interface IUser {\n  name: string;\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getClasses())
                    .anyMatch(c -> "IUser".equals(c.getName()) && c.isInterface());
        }

        @Test
        @DisplayName("não deve inventar classe que não existe no conteúdo")
        void doesNotInventClass() {
            ProjectCatalog catalog = catalogWith("tests/login.spec.ts",
                    "test('login', async ({ page }) => { await page.goto('/'); });");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getClasses())
                    .noneMatch(c -> c.getName() != null && c.getName().contains("Invented"));
        }
    }

    // ─── Detecção de Page Objects ─────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de Page Objects")
    class PageObjectDetection {

        @Test
        @DisplayName("classe terminando em Page deve estar em pageObjects")
        void detectsPageObject() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts",
                    "export class LoginPage {\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getPageObjects())
                    .extracting(ClassInfo::getName)
                    .contains("LoginPage");
        }

        @Test
        @DisplayName("classe utilitária não deve estar em pageObjects")
        void nonPageObjectNotInList() {
            ProjectCatalog catalog = catalogWith("utils/Helper.ts",
                    "export class Helper {\n}");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getPageObjects())
                    .noneMatch(c -> "Helper".equals(c.getName()));
        }
    }

    // ─── Detecção de métodos ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de métodos")
    class MethodDetection {

        private static final String LOGIN_PAGE = """
                export class LoginPage {
                  async login(email: string, password: string): Promise<void> {
                    await this.page.fill('#email', email);
                  }
                  async logout(): Promise<void> {
                    await this.page.click('#logout');
                  }
                }
                """;

        @Test
        @DisplayName("deve detectar método async com parâmetros")
        void detectsAsyncMethodWithParams() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = result.getClasses().stream()
                    .filter(c -> "LoginPage".equals(c.getName()))
                    .findFirst().orElseThrow();
            assertThat(loginPage.getMethods())
                    .extracting(MethodInfo::name)
                    .contains("login");
        }

        @Test
        @DisplayName("método login deve ser marcado como async")
        void methodIsAsync() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = findClass(result, "LoginPage");
            MethodInfo login = findMethod(loginPage, "login");
            assertThat(login.async()).isTrue();
        }

        @Test
        @DisplayName("deve detectar parâmetros email e password com tipos")
        void detectsParametersWithTypes() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = findClass(result, "LoginPage");
            MethodInfo login = findMethod(loginPage, "login");
            assertThat(login.parameters())
                    .extracting(p -> p.name())
                    .containsExactlyInAnyOrder("email", "password");
        }

        @Test
        @DisplayName("deve detectar return type Promise<void>")
        void detectsReturnType() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = findClass(result, "LoginPage");
            MethodInfo login = findMethod(loginPage, "login");
            assertThat(login.returnType()).contains("Promise");
        }

        @Test
        @DisplayName("método logout sem parâmetros deve ter lista vazia de parâmetros")
        void methodWithoutParams() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = findClass(result, "LoginPage");
            MethodInfo logout = findMethod(loginPage, "logout");
            assertThat(logout.parameters()).isEmpty();
        }

        @Test
        @DisplayName("deve detectar arquivo fonte no MethodInfo")
        void detectsSourceFile() {
            ProjectCatalog catalog = catalogWith("pages/LoginPage.ts", LOGIN_PAGE);
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            ClassInfo loginPage = findClass(result, "LoginPage");
            MethodInfo login = findMethod(loginPage, "login");
            assertThat(login.sourceFile()).contains("LoginPage.ts");
        }
    }

    // ─── Detecção de testes ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de testes")
    class TestDetection {

        @Test
        @DisplayName("deve identificar arquivo .spec.ts como arquivo de teste")
        void detectsSpecFile() throws Exception {
            Path tmp = java.nio.file.Files.createTempDirectory("autoqa");
            java.nio.file.Files.createFile(tmp.resolve("login.spec.ts"));
            ProjectCatalog catalog = catalogWith("tests/login.spec.ts",
                    "test('should login', async () => {});");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getTestFiles())
                    .anyMatch(f -> f.contains("login.spec.ts"));
        }

        @Test
        @DisplayName("deve detectar describe block")
        void detectsDescribeBlock() {
            ProjectCatalog catalog = catalogWith("tests/login.spec.ts",
                    "describe('Login', () => { it('should login', () => {}); });");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.PLAYWRIGHT);
            assertThat(result.getDescribeBlocks())
                    .anyMatch(d -> d.contains("Login"));
        }

        @Test
        @DisplayName("deve detectar arquivo .cy.ts como teste Cypress")
        void detectsCypressTestFile() {
            ProjectCatalog catalog = catalogWith("cypress/e2e/login.cy.ts",
                    "describe('Login', () => { it('logs in', () => {}); });");
            ProjectAnalysisResult result = agent.analyze(catalog, AutomationFramework.CYPRESS);
            assertThat(result.getTestFiles())
                    .anyMatch(f -> f.contains("login.cy.ts"));
        }
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private ProjectCatalog emptyCatalog() {
        return ProjectCatalog.builder()
                .projectRoot(Path.of("/tmp/project"))
                .entries(List.of())
                .totalFilesScanned(0)
                .totalContentBytes(0)
                .ignoredPaths(List.of())
                .warnings(List.of())
                .scannedAt(LocalDateTime.now())
                .build();
    }

    private ProjectCatalog catalogWith(String relativePath, String content) {
        ProjectCatalogEntry entry = new ProjectCatalogEntry(
                relativePath, content.length(), true, content, false
        );
        return ProjectCatalog.builder()
                .projectRoot(Path.of("/tmp/project"))
                .entries(List.of(entry))
                .totalFilesScanned(1)
                .totalContentBytes(content.length())
                .ignoredPaths(List.of())
                .warnings(List.of())
                .scannedAt(LocalDateTime.now())
                .build();
    }

    private ClassInfo findClass(ProjectAnalysisResult result, String name) {
        return result.getClasses().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Classe não encontrada: " + name));
    }

    private MethodInfo findMethod(ClassInfo classInfo, String name) {
        return classInfo.getMethods().stream()
                .filter(m -> name.equals(m.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Método não encontrado: " + name));
    }
}
