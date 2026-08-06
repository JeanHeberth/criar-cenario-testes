package com.br.criarcenariotestes.business.autoqa.failure;

import java.util.List;

public record FailureAnalysisAiResponse(
        List<Object> findings,
        List<Object> suggestions,
        List<Object> warnings,
        String confidence,
        Boolean humanReviewRequired,
        Boolean retryRecommended,
        Boolean regenerationRecommended,
        Boolean valid
) {
}