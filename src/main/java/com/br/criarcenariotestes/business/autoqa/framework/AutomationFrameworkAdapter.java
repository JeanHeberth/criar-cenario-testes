package com.br.criarcenariotestes.business.autoqa.framework;

import com.br.criarcenariotestes.business.autoqa.model.context.AllowedCommand;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;

import java.util.List;
import java.util.Set;

/**
 * Contrato dos adapters de framework de automação.
 * Cada implementação encapsula regras específicas do framework,
 * mantendo os agentes genéricos.
 */
public interface AutomationFrameworkAdapter {

    AutomationFramework getFramework();

    Set<AutomationLanguage> supportedLanguages();

    default boolean supports(AutomationLanguage language) {
        return supportedLanguages().contains(language);
    }

    List<String> configurationFiles();

    List<String> importantDirectories();

    List<String> ignoredDirectories();

    String buildFrameworkInstructions(ProjectDiscoveryResult discovery);

    List<AllowedCommand> validationCommands(ProjectDiscoveryResult discovery);

    List<AllowedCommand> testCommands(ProjectDiscoveryResult discovery, String testFileRelativePath);

    String defaultTestFilePattern();

    String defaultTestDirectory();
}
