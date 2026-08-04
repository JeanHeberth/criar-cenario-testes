package com.br.criarcenariotestes.business.autoqa.model.scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record ScenarioStep(
        int order,
        String action,
        String expectedResult,
        List<String> dependencies
) {
    public ScenarioStep {
        action = action == null ? null : action.trim();
        expectedResult = expectedResult == null ? null : expectedResult.trim();
        dependencies = copyList(dependencies);
    }

    private static <T> List<T> copyList(List<T> values) {
        if (values == null) {
            return null;
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
