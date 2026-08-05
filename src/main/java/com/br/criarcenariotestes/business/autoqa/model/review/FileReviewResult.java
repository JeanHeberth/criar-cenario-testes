package com.br.criarcenariotestes.business.autoqa.model.review;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de revisão de um arquivo, montado exclusivamente pelo CodeReviewService
 * a partir da combinação de issues estáticas e issues da IA.
 */
public record FileReviewResult(
        String relativePath,
        GeneratedFileOperation operation,
        PlanComponentType componentType,
        FileReviewStatus status,
        List<ReviewIssue> issues,
        List<ReviewSuggestion> suggestions,
        List<String> passedRules,
        List<String> skippedRules,
        ReviewConfidence confidence,
        boolean valid
) {
    public FileReviewResult {
        relativePath = relativePath == null ? null : relativePath.trim();
        issues = copyList(issues);
        suggestions = copyList(suggestions);
        passedRules = copyList(passedRules);
        skippedRules = copyList(skippedRules);
        confidence = confidence == null ? ReviewConfidence.UNKNOWN : confidence;
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
