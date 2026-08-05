package com.br.criarcenariotestes.business.autoqa.model.review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO não confiável (recebido da IA).
 * A validação semântica completa ocorre em CodeReviewValidator.
 */
public record ReviewSuggestion(
        String relativePath,
        String description,
        ReviewSeverity priority,
        String rationale,
        boolean automaticFixPossible,
        List<String> relatedIssueCodes
) {
    public ReviewSuggestion {
        relativePath = relativePath == null ? null : relativePath.trim();
        description = description == null ? null : description.trim();
        rationale = rationale == null ? null : rationale.trim();
        relatedIssueCodes = copyList(relatedIssueCodes);
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
