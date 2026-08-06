package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisValidationException;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FailureAnalysisValidator {

    public FailureAnalysisResult validate(FailureAnalysisResult result, ExecutionResult execution, List<FailureFinding> deterministicFindings) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(execution, "execution must not be null");

        // executionId
        if (!result.executionId().equals(execution.executionId())) {
            throw new FailureAnalysisValidationException("executionId mismatch");
        }

        FailureAnalysisStatus status = result.status();
        ExecutionStatus execStatus = execution.status();

        // NO_FAILURE only for PASSED
        if (status == FailureAnalysisStatus.NO_FAILURE && execStatus != ExecutionStatus.PASSED) {
            throw new FailureAnalysisValidationException("NO_FAILURE only allowed for PASSED executions");
        }

        if (execStatus == ExecutionStatus.PASSED && status != FailureAnalysisStatus.NO_FAILURE) {
            throw new FailureAnalysisValidationException("PASSED execution must result in NO_FAILURE");
        }

        if (execStatus == ExecutionStatus.ERROR || execStatus == ExecutionStatus.BLOCKED || execStatus == ExecutionStatus.TIMED_OUT || execStatus == ExecutionStatus.CANCELLED) {
            if (status != FailureAnalysisStatus.BLOCKED) {
                throw new FailureAnalysisValidationException("Operational execution must result in BLOCKED status");
            }
        }

        // explicit null checks for collections
        if (result.findings() == null) throw new FailureAnalysisValidationException("findings list must not be null");
        if (result.suggestions() == null) throw new FailureAnalysisValidationException("suggestions list must not be null");
        if (result.globalEvidence() == null) throw new FailureAnalysisValidationException("globalEvidence list must not be null");
        if (result.warnings() == null) throw new FailureAnalysisValidationException("warnings list must not be null");

        // basic checks for nulls in collections: use defensive iteration as contains(null) may NPE on immutable lists
        if (result.findings().stream().anyMatch(Objects::isNull)
                || result.globalEvidence().stream().anyMatch(Objects::isNull)
                || result.suggestions().stream().anyMatch(Objects::isNull)
                || result.warnings().stream().anyMatch(Objects::isNull)) {
            throw new FailureAnalysisValidationException("collections must not contain nulls");
        }

        // ANALYZED requires findings
        if ((status == FailureAnalysisStatus.ANALYZED || status == FailureAnalysisStatus.ANALYZED_WITH_WARNINGS) && result.findings().isEmpty()) {
            throw new FailureAnalysisValidationException("ANALYZED status requires at least one finding");
        }

        // CRITICAL implies blocking
        for (FailureFinding f : result.findings()) {
            if (f.severity() == FailureSeverity.CRITICAL && !f.blocking()) {
                throw new FailureAnalysisValidationException("CRITICAL finding must be blocking");
            }
            if (f.code() == null || f.code().isBlank()) throw new FailureAnalysisValidationException("finding code missing");
            if (f.category() == null) throw new FailureAnalysisValidationException("finding category missing");
            if (f.origin() == null) throw new FailureAnalysisValidationException("finding origin missing");
            if (f.severity() == null) throw new FailureAnalysisValidationException("finding severity missing");
            if (f.confidence() == null) throw new FailureAnalysisValidationException("finding confidence missing");
            if (f.title() == null || f.title().isBlank()) throw new FailureAnalysisValidationException("finding title missing");
            if (f.description() == null || f.description().isBlank()) throw new FailureAnalysisValidationException("finding description missing");
            if (f.probableCause() == null || f.probableCause().isBlank()) throw new FailureAnalysisValidationException("finding probableCause missing");
            if (f.evidence() == null) throw new FailureAnalysisValidationException("finding evidence null");
            for (FailureEvidence ev : f.evidence()) {
                if (ev == null) throw new FailureAnalysisValidationException("evidence element null");
                if (ev.source() == null || ev.source().isBlank()) throw new FailureAnalysisValidationException("evidence source missing");
                if (ev.relativePath() != null && ev.relativePath().contains("..")) throw new FailureAnalysisValidationException("evidence contains path traversal");
                if (ev.line() != null && ev.line() <= 0) throw new FailureAnalysisValidationException("evidence line must be positive");
            }
        }

        // preserve deterministic findings (must be subset)
        for (FailureFinding df : deterministicFindings) {
            boolean found = result.findings().stream().anyMatch(f -> f.code().equals(df.code()));
            if (!found) throw new FailureAnalysisValidationException("deterministic finding omitted: " + df.code());
        }

        // suggestions must not contain code/diff/patch
        for (FailureSuggestion s : result.suggestions()) {
            if (s.description() != null && (s.description().contains("diff") || s.description().contains("patch") || s.description().contains("<code>"))) {
                throw new FailureAnalysisValidationException("suggestion contains forbidden content");
            }
        }

        return result; // do not modify
    }
}