package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

public record FailureAnalysisResult(
        String failureType,
        String summary,
        String probableCause,
        List<String> affectedFiles,
        List<String> suggestedChanges,
        boolean canRetryAutomatically,
        boolean requiresUserIntervention
) {
}
