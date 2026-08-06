package com.br.criarcenariotestes.business.autoqa.model.failure;

import java.util.List;
import java.util.Objects;

public record FailureSuggestion(
        String description,
        String rationale,
        FailureSeverity priority,
        List<String> relatedFindingCodes,
        List<String> affectedFiles,
        boolean automaticCorrectionPossible,
        boolean requiresHumanApproval
) {
    public FailureSuggestion {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        relatedFindingCodes = relatedFindingCodes == null ? List.of() : List.copyOf(relatedFindingCodes);
        affectedFiles = affectedFiles == null ? List.of() : List.copyOf(affectedFiles);
    }
}