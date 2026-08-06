package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.*;

import java.util.List;
import java.util.UUID;

public class FailureSummaryBuilder {

    public FailureAnalysisResult summarize(UUID executionId, ExecutionResult execution, List<FailureFinding> deterministicFindings, List<FailureFinding> aiFindings, List<FailureEvidence> evidence) {
        // derive status deterministically
        FailureAnalysisStatus status;
        if (execution.status() == null) {
            status = FailureAnalysisStatus.BLOCKED;
        } else if (execution.status().name().equals("PASSED")) {
            status = FailureAnalysisStatus.NO_FAILURE;
        } else if (execution.status().name().equals("FAILED")) {
            if ((deterministicFindings != null && !deterministicFindings.isEmpty()) || (aiFindings != null && !aiFindings.isEmpty())) {
                status = FailureAnalysisStatus.ANALYZED;
            } else {
                status = FailureAnalysisStatus.INCONCLUSIVE;
            }
        } else {
            status = FailureAnalysisStatus.BLOCKED;
        }

        FailureConfidence confidence = FailureConfidence.UNKNOWN;
        boolean human = false;
        boolean retry = false;
        boolean regen = false;

        if (deterministicFindings != null && !deterministicFindings.isEmpty()) {
            confidence = FailureConfidence.HIGH;
            human = true;
        }

        return new FailureAnalysisResult(
                executionId,
                deterministicFindings == null ? List.<FailureFinding>of() : List.copyOf(deterministicFindings),
                List.<FailureSuggestion>of(),
                evidence == null ? List.<FailureEvidence>of() : List.copyOf(evidence),
                List.of(),
                status,
                confidence,
                human,
                retry,
                regen,
                true
        );
    }
}