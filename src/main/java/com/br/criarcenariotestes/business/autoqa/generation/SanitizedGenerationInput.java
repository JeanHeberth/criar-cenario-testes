package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;

import java.util.List;

public record SanitizedGenerationInput(
        AutomationFramework framework,
        AutomationLanguage language,
        BuildTool buildTool,
        List<String> testingFrameworks,
        NamingConvention namingConvention,
        String scenarioTitle,
        String scenarioObjective,
        List<SanitizedStep> steps,
        String planTitle,
        String planStrategy,
        List<SanitizedFileAction> fileActions,
        List<SanitizedComponent> reusableComponents,
        List<String> planWarnings
) {
    public record SanitizedStep(int order, String action, String expectedResult) {}

    public record SanitizedFileAction(
            String relativePath,
            String operationName,
            String componentTypeName,
            String reason,
            boolean existingFile,
            List<String> dependencies
    ) {}

    public record SanitizedComponent(
            String relativePath,
            String typeName,
            String componentName,
            List<String> declaredMethods,
            List<String> imports
    ) {}
}
