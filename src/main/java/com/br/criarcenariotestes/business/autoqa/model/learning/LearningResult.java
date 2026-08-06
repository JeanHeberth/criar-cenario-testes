package com.br.criarcenariotestes.business.autoqa.model.learning;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado final da Fase 11, montado exclusivamente por LearningSummaryBuilder.
 * A coerência entre status/counts/items é responsabilidade de quem constrói o
 * resultado — este compact constructor não corrige nada, apenas valida
 * invariantes estruturais.
 */
public record LearningResult(
        UUID executionId,
        List<LearningItem> items,
        List<LearningEvidence> globalEvidence,
        List<LearningWarning> warnings,
        LearningStatus status,
        LearningConfidence confidence,
        int positiveLearnings,
        int negativeLearnings,
        boolean humanReviewRequired,
        boolean valid
) {
    public LearningResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        items = items == null ? List.of() : List.copyOf(items);
        globalEvidence = globalEvidence == null ? List.of() : List.copyOf(globalEvidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        if (positiveLearnings < 0) {
            throw new IllegalArgumentException("positiveLearnings must not be negative");
        }
        if (negativeLearnings < 0) {
            throw new IllegalArgumentException("negativeLearnings must not be negative");
        }
    }
}
