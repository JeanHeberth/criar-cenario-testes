package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;

import java.util.List;

public record SanitizedCodeReviewInput(
        AutomationFramework framework,
        AutomationLanguage language,
        String scenarioTitle,
        String scenarioObjective,
        String planTitle,
        String planStrategy,
        NamingConvention namingConvention,
        List<SanitizedReviewFile> files,
        List<SanitizedComponent> reusableComponents,
        List<SanitizedStaticIssue> staticIssues
) {
    public record SanitizedReviewFile(String relativePath, String operationName, String componentTypeName, String content) {}
    public record SanitizedComponent(String relativePath, String typeName, String componentName) {}
    public record SanitizedStaticIssue(String code, String categoryName, String severityName, String relativePath, String message) {}
}
