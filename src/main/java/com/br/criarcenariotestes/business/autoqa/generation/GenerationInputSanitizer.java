package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class GenerationInputSanitizer {

    static final int MAX_FILE_ACTIONS = 30;
    static final int MAX_COMPONENTS = 20;
    static final int MAX_STEPS = 20;
    static final int MAX_METHODS_PER_COMPONENT = 10;
    static final int MAX_IMPORTS_PER_COMPONENT = 10;
    static final int MAX_WARNINGS = 10;
    static final int MAX_DEPENDENCIES_PER_ACTION = 10;

    public SanitizedGenerationInput sanitize(ProjectDiscoveryResult discovery,
                                              ScenarioAnalysisResult scenario,
                                              ProjectKnowledgeResult knowledge,
                                              TechnicalPlanResult plan) {
        return sanitize(discovery, scenario, knowledge, plan, List.of());
    }

    /**
     * @param correcoes erros da tentativa anterior a serem corrigidos nesta.
     */
    public SanitizedGenerationInput sanitize(ProjectDiscoveryResult discovery,
                                              ScenarioAnalysisResult scenario,
                                              ProjectKnowledgeResult knowledge,
                                              TechnicalPlanResult plan,
                                              List<String> correcoes) {
        return sanitize(discovery, scenario, knowledge, plan, correcoes, null);
    }

    public SanitizedGenerationInput sanitize(ProjectDiscoveryResult discovery,
                                              ScenarioAnalysisResult scenario,
                                              ProjectKnowledgeResult knowledge,
                                              TechnicalPlanResult plan,
                                              List<String> correcoes,
                                              String moduloDoCliente) {
        return sanitize(discovery, scenario, knowledge, plan, correcoes, moduloDoCliente, List.of());
    }

    public SanitizedGenerationInput sanitize(ProjectDiscoveryResult discovery,
                                              ScenarioAnalysisResult scenario,
                                              ProjectKnowledgeResult knowledge,
                                              TechnicalPlanResult plan,
                                              List<String> correcoes,
                                              String moduloDoCliente,
                                              List<String> camposDoContrato) {
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(plan, "plan must not be null");

        List<String> testingFrameworks = discovery.getTestingFrameworks().stream()
                .map(Enum::name).sorted().toList();

        List<SanitizedGenerationInput.SanitizedStep> steps = scenario.steps() == null ? List.of() :
                scenario.steps().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(ScenarioStep::order))
                        .limit(MAX_STEPS)
                        .map(s -> new SanitizedGenerationInput.SanitizedStep(s.order(), s.action(), s.expectedResult()))
                        .toList();

        List<SanitizedGenerationInput.SanitizedFileAction> fileActions = plan.fileActions() == null ? List.of() :
                plan.fileActions().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(a -> a.relativePath() != null ? a.relativePath() : ""))
                        .limit(MAX_FILE_ACTIONS)
                        .map(this::toSanitizedFileAction)
                        .toList();

        List<SanitizedGenerationInput.SanitizedComponent> reusableComponents = knowledge.components() == null ? List.of() :
                knowledge.components().stream()
                        .filter(Objects::nonNull)
                        .filter(ProjectComponent::reusable)
                        .sorted(Comparator.comparing(ProjectComponent::relativePath))
                        .limit(MAX_COMPONENTS)
                        .map(this::toSanitizedComponent)
                        .toList();

        List<String> planWarnings = plan.warnings() == null ? List.of() :
                plan.warnings().stream()
                        .filter(Objects::nonNull)
                        .map(w -> w.code() + ": " + w.description())
                        .sorted()
                        .limit(MAX_WARNINGS)
                        .toList();

        return new SanitizedGenerationInput(
                discovery.getAutomationFramework(),
                scenario.automationType(),
                discovery.getLanguage(),
                discovery.getBuildTool(),
                testingFrameworks,
                knowledge.namingConvention(),
                scenario.title(),
                scenario.objective(),
                steps,
                plan.title(),
                plan.strategy(),
                fileActions,
                reusableComponents,
                planWarnings,
                correcoes == null ? List.of() : List.copyOf(correcoes),
                moduloDoCliente,
                camposDoContrato == null ? List.of() : List.copyOf(camposDoContrato)
        );
    }

    private SanitizedGenerationInput.SanitizedFileAction toSanitizedFileAction(PlannedFileAction action) {
        List<String> dependencies = action.dependencies() == null ? List.of() :
                action.dependencies().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .limit(MAX_DEPENDENCIES_PER_ACTION)
                        .toList();
        return new SanitizedGenerationInput.SanitizedFileAction(
                action.relativePath(),
                action.operation() != null ? action.operation().name() : "UNKNOWN",
                action.componentType() != null ? action.componentType().name() : "UNKNOWN",
                action.reason(),
                action.existingFile(),
                dependencies
        );
    }

    private SanitizedGenerationInput.SanitizedComponent toSanitizedComponent(ProjectComponent component) {
        List<String> declaredMethods = component.declaredMethods() == null ? List.of() :
                component.declaredMethods().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .limit(MAX_METHODS_PER_COMPONENT)
                        .toList();
        List<String> imports = component.imports() == null ? List.of() :
                component.imports().stream()
                        .filter(Objects::nonNull)
                        .sorted()
                        .limit(MAX_IMPORTS_PER_COMPONENT)
                        .toList();
        return new SanitizedGenerationInput.SanitizedComponent(
                component.relativePath(),
                component.type() != null ? component.type().name() : "UNKNOWN",
                component.name(),
                declaredMethods,
                imports
        );
    }
}
