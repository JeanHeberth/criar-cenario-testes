package com.br.criarcenariotestes.business.autoqa.framework.cypress;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.AllowedCommand;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CypressAdapter implements AutomationFrameworkAdapter {

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    @Override
    public AutomationFramework getFramework() {
        return AutomationFramework.CYPRESS;
    }

    @Override
    public Set<AutomationLanguage> supportedLanguages() {
        return Set.of(AutomationLanguage.TYPESCRIPT, AutomationLanguage.JAVASCRIPT);
    }

    @Override
    public List<String> configurationFiles() {
        return List.of("cypress.config.ts", "cypress.config.js");
    }

    @Override
    public List<String> importantDirectories() {
        return List.of(
                "cypress/e2e", "cypress/support", "cypress/fixtures",
                "cypress/commands", "cypress/pages"
        );
    }

    @Override
    public List<String> ignoredDirectories() {
        return List.of(
                "node_modules", ".git", "dist", "build", "coverage",
                "cypress/videos", "cypress/screenshots",
                "allure-results", "allure-report",
                ".idea", ".vscode", "logs"
        );
    }

    @Override
    public String buildFrameworkInstructions(ProjectDiscoveryResult discovery) {
        return """
                ## Cypress — Regras de Geração de Código
                - Reutilizar custom commands existentes em cypress/support
                - Reutilizar fixtures existentes
                - Preferir seletores data-cy, data-testid ou seletores estáveis
                - Nunca usar cy.wait com tempo fixo como sincronização
                - Usar interceptações cy.intercept quando necessário
                - Não inventar comandos customizados inexistentes
                - Respeitar baseUrl configurada no cypress.config
                - Não inserir credenciais hardcoded — usar variáveis de ambiente
                - Arquivos de teste TypeScript devem usar extensão .cy.ts
                - Usar describe e it conforme o padrão do projeto
                - Validar respostas e elementos de maneira determinística
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
                        "cypress-run",
                        npxExec,
                        List.of("cypress", "run", "--spec", testFileRelativePath),
                        "Execução de teste Cypress para o arquivo especificado"
                )
        );
    }

    @Override
    public String defaultTestFilePattern() {
        return "**/*.cy.ts";
    }

    @Override
    public String defaultTestDirectory() {
        return "cypress/e2e";
    }

    private String resolveExecutable(String base, ProjectDiscoveryResult discovery) {
        return IS_WINDOWS ? base + ".cmd" : base;
    }
}
