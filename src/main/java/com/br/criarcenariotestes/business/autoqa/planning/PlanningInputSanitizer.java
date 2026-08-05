package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.scenario.BusinessRule;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAmbiguity;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioRisk;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.*;

@Component
public class PlanningInputSanitizer {

    static final int MAX_COMPONENTS = 30;
    static final int MAX_CANDIDATES = 15;
    static final int MAX_STEPS = 20;
    static final int MAX_WARNINGS = 10;

    public SanitizedPlanningInput sanitize(ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario, ProjectKnowledgeResult knowledge) {
        List<String> testingFrameworks = discovery.getTestingFrameworks().stream()
            .map(Enum::name).sorted().toList();
        List<String> detectedFrameworks = discovery.getDetectedFrameworks().stream()
            .map(Enum::name).sorted().toList();

        List<SanitizedPlanningInput.SanitizedStep> steps = scenario.steps() == null ? List.of() :
            scenario.steps().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ScenarioStep::order))
                .limit(MAX_STEPS)
                .map(s -> new SanitizedPlanningInput.SanitizedStep(s.order(), s.action(), s.expectedResult()))
                .toList();

        List<String> businessRuleDescriptions = scenario.businessRules() == null ? List.of() :
            scenario.businessRules().stream()
                .filter(Objects::nonNull)
                .map(BusinessRule::description)
                .filter(Objects::nonNull)
                .toList();

        List<String> riskDescriptions = scenario.risks() == null ? List.of() :
            scenario.risks().stream()
                .filter(Objects::nonNull)
                .map(ScenarioRisk::description)
                .filter(Objects::nonNull)
                .toList();

        List<String> ambiguityDescriptions = scenario.ambiguities() == null ? List.of() :
            scenario.ambiguities().stream()
                .filter(Objects::nonNull)
                .map(ScenarioAmbiguity::description)
                .filter(Objects::nonNull)
                .toList();

        List<SanitizedPlanningInput.SanitizedComponent> components = knowledge.components() == null ? List.of() :
            knowledge.components().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProjectComponent::relativePath))
                .limit(MAX_COMPONENTS)
                .map(c -> new SanitizedPlanningInput.SanitizedComponent(
                    c.relativePath(),
                    c.type() != null ? c.type().name() : "UNKNOWN",
                    c.name()
                ))
                .toList();

        List<SanitizedPlanningInput.SanitizedCandidate> candidates = knowledge.reuseCandidates() == null ? List.of() :
            knowledge.reuseCandidates().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(c -> c.componentPath() != null ? c.componentPath() : ""))
                .limit(MAX_CANDIDATES)
                .map(c -> new SanitizedPlanningInput.SanitizedCandidate(
                    c.componentPath(),
                    c.type() != null ? c.type().name() : "UNKNOWN",
                    c.confidence() != null ? c.confidence().name() : "UNKNOWN",
                    c.matchingTerms()
                ))
                .toList();

        List<String> knowledgeWarnings = knowledge.warnings() == null ? List.of() :
            knowledge.warnings().stream()
                .filter(Objects::nonNull)
                .sorted()
                .limit(MAX_WARNINGS)
                .toList();

        return new SanitizedPlanningInput(
            discovery.getAutomationFramework(),
            discovery.getLanguage(),
            discovery.getBuildTool(),
            discovery.getPackageManager(),
            testingFrameworks,
            detectedFrameworks,
            discovery.getConfidence(),
            scenario.title(),
            scenario.objective(),
            scenario.preconditions() != null ? scenario.preconditions() : List.of(),
            steps,
            scenario.entities() != null ? scenario.entities() : List.of(),
            businessRuleDescriptions,
            riskDescriptions,
            ambiguityDescriptions,
            scenario.status(),
            knowledge.status(),
            knowledge.namingConvention(),
            components,
            candidates,
            knowledgeWarnings,
            knowledge.sourceDirectories() != null ? knowledge.sourceDirectories() : List.of(),
            knowledge.testDirectories() != null ? knowledge.testDirectories() : List.of()
        );
    }
}
