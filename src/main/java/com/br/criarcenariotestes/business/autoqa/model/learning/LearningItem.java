package com.br.criarcenariotestes.business.autoqa.model.learning;

import java.util.List;
import java.util.Objects;

/**
 * Item de aprendizado imutável. Este record valida apenas invariantes
 * estruturais (nulls, imutabilidade de coleções). Regras semânticas
 * cruzadas entre campos (approvalStatus por scope, reusable exigindo
 * evidência, humanReviewRequired por confidence, escopo proibido) são
 * responsabilidade de LearningValidator — não deste compact constructor.
 */
public record LearningItem(
        String id,
        LearningType type,
        LearningScope scope,
        LearningSource source,
        String title,
        String description,
        String recommendation,
        LearningConfidence confidence,
        LearningApprovalStatus approvalStatus,
        List<LearningEvidence> evidence,
        List<String> relatedComponents,
        List<String> relatedFiles,
        List<String> relatedFailureCodes,
        List<String> tags,
        boolean positive,
        boolean reusable,
        boolean humanReviewRequired
) {
    public LearningItem {
        id = requireText(id, "id");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(source, "source must not be null");
        title = requireText(title, "title");
        description = requireText(description, "description");
        recommendation = requireText(recommendation, "recommendation");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(approvalStatus, "approvalStatus must not be null");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        relatedComponents = relatedComponents == null ? List.of() : List.copyOf(relatedComponents);
        relatedFiles = relatedFiles == null ? List.of() : List.copyOf(relatedFiles);
        relatedFailureCodes = relatedFailureCodes == null ? List.of() : List.copyOf(relatedFailureCodes);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
