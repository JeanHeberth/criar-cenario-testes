package com.br.criarcenariotestes.business.autoqa.model.failure;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FailureAnalysisResult(
        UUID executionId,
        List<FailureFinding> findings,
        List<FailureSuggestion> suggestions,
        List<FailureEvidence> globalEvidence,
        List<FailureWarning> warnings,
        FailureAnalysisStatus status,
        FailureConfidence confidence,
        boolean humanReviewRequired,
        boolean retryRecommended,
        boolean regenerationRecommended,
        boolean valid
) {
    public FailureAnalysisResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        findings = findings == null ? List.of() : List.copyOf(findings);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        globalEvidence = globalEvidence == null ? List.of() : List.copyOf(globalEvidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        if (status == null) throw new NullPointerException("status must not be null");
        if (confidence == null) throw new NullPointerException("confidence must not be null");
    }
}