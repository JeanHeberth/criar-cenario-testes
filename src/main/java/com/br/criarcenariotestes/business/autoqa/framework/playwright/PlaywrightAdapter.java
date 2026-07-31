package com.br.criarcenariotestes.business.autoqa.framework.playwright;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.AllowedCommand;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PlaywrightAdapter implements AutomationFrameworkAdapter {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    @Override
    public AutomationFramework getFramework() {
        return AutomationFramework.PLAYWRIGHT;
    }

    @Override
    public Set<AutomationLanguage> supportedLanguages() {
        return Set.of(AutomationLanguage.TYPESCRIPT, AutomationLanguage.JAVASCRIPT);
    }

    @Override
    public List<String> configurationFiles() {
        return List.of(
                "playwright.config.ts",
                "playwright.config.js",
                "playwright.config.mts",
                "playwright.config.mjs"
        );
    }

    @Override
    public List<String> importantDirectories() {
        return List.of("tests", "e2e", "pages", "fixtures", "helpers", "utils", "data");
    }

    @Override
    public List<String> ignoredDirectories() {
        return List.of(
                "node_modules", ".git", "dist", "build", "coverage",
                "playwright-report", "test-results", "blob-report",
                "allure-results", "allure-report",
                ".idea", ".vscode", "logs"
        );
    }

    @Override
    public String buildFrameworkInstructions(ProjectDiscoveryResult discovery) {
        return """
                ## Playwright — Regras de Geração de Código
                - Utilizar @playwright/test como framework de testes
                - Preferir getByRole, getByTestId, getByLabel e locators estáveis
                - Nunca usar waitForTimeout como sincronização
                - Usar expect do Playwright para assertions
                - Não inventar métodos inexistentes nos Page Objects
                - Não duplicar métodos já existentes
                - Respeitar baseURL configurada
                - Não inserir credenciais hardcoded — usar variáveis de ambiente
                - Arquivos de teste TypeScript devem usar extensão .spec.ts
                - Usar test.describe para agrupar cenários relacionados
                - Considerar setup, teardown, screenshots e traces já configurados
                """;
    }

    @Override
    public List<AllowedCommand> validationCommands(ProjectDiscoveryResult discovery) {
        String npxExec = resolveExecutable("npx", discovery);
        List<AllowedCommand> commands = new ArrayList<>();
        if (discovery != null
                && discovery.getDetectedLanguage() == AutomationLanguage.TYPESCRIPT) {
            commands.add(AllowedCommand.of(
                    "typescript-check",
                    npxExec,
                    List.of("tsc", "--noEmit"),
                    "Verificação de tipos TypeScript sem emissão de arquivos"
            ));
        }
        return commands;
    }

    @Override
    public List<AllowedCommand> testCommands(ProjectDiscoveryResult discovery, String testFileRelativePath) {
        String npxExec = resolveExecutable("npx", discovery);
        return List.of(
                AllowedCommand.of(
                        "playwright-test",
                        npxExec,
                        List.of("playwright", "test", testFileRelativePath),
                        "Execução de teste Playwright para o arquivo especificado"
                )
        );
    }

    @Override
    public String defaultTestFilePattern() {
        return "**/*.spec.ts";
    }

    @Override
    public String defaultTestDirectory() {
        return "tests";
    }

    private String resolveExecutable(String base, ProjectDiscoveryResult discovery) {
        return IS_WINDOWS ? base + ".cmd" : base;
    }
}
