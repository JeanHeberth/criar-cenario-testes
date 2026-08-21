package com.br.criarcenariotestes.business.autoqa.model.scenario;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record ScenarioAnalysisResult(
        String title,
        String objective,
        @JsonDeserialize(contentUsing = TextoFlexivelDeserializer.class)
        List<String> preconditions,
        List<ScenarioStep> steps,
        List<TestDataRequirement> testData,
        List<BusinessRule> businessRules,
        List<ScenarioRisk> risks,
        List<ScenarioAmbiguity> ambiguities,
        @JsonDeserialize(contentUsing = TextoFlexivelDeserializer.class)
        List<String> entities,
        @JsonDeserialize(contentUsing = TextoFlexivelDeserializer.class)
        List<String> dependencies,
        AutomationType automationType,
        ScenarioAnalysisStatus status,
        @JsonDeserialize(contentUsing = TextoFlexivelDeserializer.class)
        List<String> warnings,
        boolean valid
) {
    public ScenarioAnalysisResult {
        title = title == null ? null : title.trim();
        objective = objective == null ? null : objective.trim();
        preconditions = copyList(preconditions);
        steps = copyList(steps);
        testData = copyList(testData);
        businessRules = copyList(businessRules);
        risks = copyList(risks);
        ambiguities = copyList(ambiguities);
        entities = copyList(entities);
        dependencies = copyList(dependencies);
        warnings = copyList(warnings);
    }

    private static <T> List<T> copyList(List<T> values) {
        if (values == null) {
            return null;
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
