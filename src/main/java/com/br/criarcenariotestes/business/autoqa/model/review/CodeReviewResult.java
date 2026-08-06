package com.br.criarcenariotestes.business.autoqa.model.review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado final da Fase 7, montado exclusivamente por CodeReviewService.
 * Diferente das fases anteriores, este record nunca é o alvo de deserialização
 * direta da resposta da IA (esse papel é do CodeReviewAiResponse) — por isso
 * pode exigir executionId não nulo.
 */
public record CodeReviewResult(
        UUID executionId,
        List<FileReviewResult> files,
        List<ReviewIssue> globalIssues,
        List<ReviewSuggestion> suggestions,
        List<String> passedRules,
        List<String> skippedRules,
        List<ReviewWarning> warnings,
        ReviewStatus status,
        ReviewConfidence confidence,
        boolean humanReviewRequired,
        boolean valid
) {
    public CodeReviewResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        files = copyList(files);
        globalIssues = copyList(globalIssues);
        suggestions = copyList(suggestions);
        passedRules = copyList(passedRules);
        skippedRules = copyList(skippedRules);
        warnings = copyList(warnings);
        confidence = confidence == null ? ReviewConfidence.UNKNOWN : confidence;
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
