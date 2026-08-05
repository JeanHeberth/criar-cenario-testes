package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.*;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.*;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class GenerationTestData {
    private GenerationTestData() {}

    // --- discovery ---

    public static ProjectDiscoveryResult discovery(AutomationFramework framework, AutomationLanguage language) {
        return new ProjectDiscoveryResult(
                Path.of("/project"), framework, language, PackageManager.NPM, BuildTool.NPM,
                Set.of(), Set.of(framework), List.of(), null, List.of(), List.of(), DiscoveryConfidence.HIGH, true
        );
    }

    public static ProjectDiscoveryResult playwrightDiscovery() { return discovery(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT); }
    public static ProjectDiscoveryResult cypressDiscovery() { return discovery(AutomationFramework.CYPRESS, AutomationLanguage.TYPESCRIPT); }
    public static ProjectDiscoveryResult selenideDiscovery() { return discovery(AutomationFramework.SELENIDE, AutomationLanguage.JAVA); }
    public static ProjectDiscoveryResult seleniumDiscovery() { return discovery(AutomationFramework.SELENIUM, AutomationLanguage.JAVA); }
    public static ProjectDiscoveryResult restAssuredDiscovery() { return discovery(AutomationFramework.REST_ASSURED, AutomationLanguage.JAVA); }
    public static ProjectDiscoveryResult robotDiscovery() { return discovery(AutomationFramework.ROBOT_FRAMEWORK, AutomationLanguage.ROBOT); }
    public static ProjectDiscoveryResult unknownDiscovery() { return discovery(AutomationFramework.UNKNOWN, AutomationLanguage.UNKNOWN); }
    public static ProjectDiscoveryResult appiumDiscovery() { return discovery(AutomationFramework.APPIUM, AutomationLanguage.JAVA); }

    // --- scenario ---

    public static ScenarioAnalysisResult validScenario() {
        return new ScenarioAnalysisResult(
                "Login válido", "Validar acesso ao sistema", List.of("Usuário cadastrado"),
                List.of(new ScenarioStep(1, "Acessar login", "Tela exibida", List.of())),
                List.of(), List.of(), List.of(), List.of(), List.of("Usuário"), List.of(),
                AutomationType.WEB_UI, ScenarioAnalysisStatus.VALID, List.of(), true
        );
    }

    public static ScenarioAnalysisResult invalidScenario() {
        return new ScenarioAnalysisResult(
                "Cenário inválido", "Não pode ser automatizado", List.of(),
                List.of(new ScenarioStep(1, "Ação", "Resultado", List.of())),
                List.of(), List.of(), List.of(), List.of(new ScenarioAmbiguity("Ambiguidade bloqueante", "Questão?", true)),
                List.of(), List.of(), AutomationType.WEB_UI, ScenarioAnalysisStatus.INVALID, List.of(), false
        );
    }

    // --- knowledge ---

    public static ProjectKnowledgeResult completeKnowledge(String... reusablePaths) {
        List<ProjectComponent> components = Arrays.stream(reusablePaths)
                .map(p -> new ProjectComponent(p, nameFromPath(p), ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT,
                        null, List.of(), List.of("open", "login"), List.of("import { Page } from '@playwright/test'"),
                        List.of(), List.of(), false, true, List.of()))
                .toList();
        return buildKnowledge(components, KnowledgeStatus.COMPLETE);
    }

    public static ProjectKnowledgeResult emptyKnowledge() {
        return buildKnowledge(List.of(), KnowledgeStatus.EMPTY);
    }

    public static ProjectKnowledgeResult failedKnowledge() {
        return buildKnowledge(List.of(), KnowledgeStatus.FAILED);
    }

    private static ProjectKnowledgeResult buildKnowledge(List<ProjectComponent> components, KnowledgeStatus status) {
        return new ProjectKnowledgeResult(
                Path.of("/project"), components, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(),
                new NamingConvention("*.spec.ts", "*Page.ts", "PascalCase", "camelCase", "tests", List.of(), ReuseConfidence.HIGH),
                List.of("tests"), List.of("src"), List.of("node_modules"), List.of(), status, status != KnowledgeStatus.FAILED
        );
    }

    private static String nameFromPath(String path) {
        String[] parts = path.split("[/\\\\]");
        String file = parts[parts.length - 1];
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    // --- plan ---

    public static PlannedFileAction createAction(String path, PlanComponentType type) {
        return new PlannedFileAction(path, FileOperation.CREATE, type, "Criar arquivo", false, true, ApprovalRequirement.NONE, List.of(), List.of());
    }

    public static PlannedFileAction updateAction(String path, PlanComponentType type) {
        return new PlannedFileAction(path, FileOperation.UPDATE, type, "Atualizar arquivo", true, true, ApprovalRequirement.FILE_UPDATE_REQUIRED, List.of(), List.of());
    }

    public static PlannedFileAction reuseAction(String path, PlanComponentType type) {
        return new PlannedFileAction(path, FileOperation.REUSE, type, "Reutilizar componente", true, true, ApprovalRequirement.NONE, List.of(), List.of());
    }

    public static PlannedFileAction noneAction(String path, PlanComponentType type) {
        return new PlannedFileAction(path, FileOperation.NONE, type, "Nenhuma ação necessária", true, false, ApprovalRequirement.NONE, List.of(), List.of());
    }

    public static TechnicalPlanResult readyPlan(PlannedFileAction... actions) {
        return new TechnicalPlanResult(
                "Plano de automação", "Gerar automação de login", List.of(actions), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
    }

    public static TechnicalPlanResult blockedPlan() {
        return new TechnicalPlanResult(
                "Plano bloqueado", "Decisão necessária", List.of(), List.of(), List.of(), List.of(),
                List.of(new PlanningWarning("W001", "Decisão humana necessária", true)),
                List.of(), List.of(), List.of(), PlanningStatus.BLOCKED, PlanningConfidence.LOW, false
        );
    }

    public static TechnicalPlanResult invalidPlan() {
        return new TechnicalPlanResult(
                "Plano inválido", "Cenário não pode ser planejado", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), PlanningStatus.INVALID, PlanningConfidence.UNKNOWN, false
        );
    }

    // --- generated files / content samples ---

    public static final String PLAYWRIGHT_CONTENT = """
            import { test, expect } from '@playwright/test';

            test('login deve funcionar', async ({ page }) => {
              await page.goto('/login');
              await expect(page).toHaveTitle(/Login/);
            });
            """;

    public static final String CYPRESS_CONTENT = """
            describe('login', () => {
              it('deve logar', () => {
                cy.visit('/login');
              });
            });
            """;

    public static final String SELENIDE_CONTENT = """
            package com.example.tests;

            import com.codeborne.selenide.Selenide;

            public class LoginTest {
                void login() {
                    Selenide.open("/login");
                }
            }
            """;

    public static final String SELENIUM_CONTENT = """
            package com.example.tests;

            import org.openqa.selenium.WebDriver;

            public class LoginTest {
                void login(WebDriver driver) {
                    driver.get("/login");
                }
            }
            """;

    public static final String REST_ASSURED_CONTENT = """
            package com.example.tests;

            import io.restassured.RestAssured;
            import io.restassured.response.Response;

            public class LoginApiTest {
                void login() {
                    Response response = RestAssured.given().when().get("/login");
                }
            }
            """;

    public static final String ROBOT_CONTENT = """
            *** Settings ***
            Library    SeleniumLibrary

            *** Test Cases ***
            Login Deve Funcionar
                Open Browser    /login    chrome
            """;

    public static GeneratedFile generatedFile(String path, GeneratedFileOperation operation, PlanComponentType type,
                                               String content, GeneratedFileStatus status, boolean existingFile) {
        return new GeneratedFile(path, operation, type, content, "UTF-8", content == null ? null : "hash", status, existingFile, List.of(), List.of(), List.of());
    }

    public static GenerationResult aiResult(GenerationStatus status, GenerationConfidence confidence, boolean valid, GeneratedFile... files) {
        return new GenerationResult(null, null, null, List.of(files), List.of(), List.of(), null, null, status, confidence, valid);
    }

    public static String playwrightAiResponseJson(String path) {
        return """
                {
                  "files": [
                    {
                      "relativePath": "%s",
                      "operation": "CREATE",
                      "componentType": "TEST",
                      "content": "%s",
                      "encoding": "UTF-8",
                      "existingFile": false,
                      "reusedComponents": [],
                      "dependencies": [],
                      "warnings": []
                    }
                  ],
                  "warnings": [],
                  "status": "COMPLETED",
                  "confidence": "HIGH",
                  "valid": true
                }
                """.formatted(path, PLAYWRIGHT_CONTENT.replace("\n", "\\n").replace("\"", "\\\""));
    }
}
