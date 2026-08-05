package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import java.util.List;

public record SanitizedPlanningInput(
    AutomationFramework framework,
    AutomationLanguage language,
    BuildTool buildTool,
    PackageManager packageManager,
    List<String> testingFrameworks,
    List<String> detectedFrameworks,
    DiscoveryConfidence discoveryConfidence,
    String scenarioTitle,
    String scenarioObjective,
    List<String> scenarioPreconditions,
    List<SanitizedStep> steps,
    List<String> entities,
    List<String> businessRuleDescriptions,
    List<String> riskDescriptions,
    List<String> ambiguityDescriptions,
    ScenarioAnalysisStatus scenarioStatus,
    KnowledgeStatus knowledgeStatus,
    NamingConvention namingConvention,
    List<SanitizedComponent> components,
    List<SanitizedCandidate> candidates,
    List<String> knowledgeWarnings,
    List<String> sourceDirectories,
    List<String> testDirectories
) {
    public record SanitizedStep(int order, String action, String expectedResult) {}
    public record SanitizedComponent(String relativePath, String typeName, String componentName) {}
    public record SanitizedCandidate(String componentPath, String typeName, String confidenceName, List<String> matchingTerms) {}
}
