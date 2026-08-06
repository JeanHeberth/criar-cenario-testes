package com.br.criarcenariotestes.business.autoqa.model.failure;

import java.util.List;
import java.util.Objects;

public record FailureFinding(
        String code,
        FailureCategory category,
        FailureOrigin origin,
        FailureSeverity severity,
        FailureConfidence confidence,
        String title,
        String description,
        String probableCause,
        List<String> affectedTests,
        List<String> relatedFiles,
        List<FailureEvidence> evidence,
        boolean blocking,
        boolean likelyFlaky
) {
    public FailureFinding {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(probableCause, "probableCause must not be null");
        affectedTests = affectedTests == null ? List.of() : List.copyOf(affectedTests);
        relatedFiles = relatedFiles == null ? List.of() : List.copyOf(relatedFiles);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}