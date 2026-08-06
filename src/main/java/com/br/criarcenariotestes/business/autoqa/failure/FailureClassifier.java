package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.failure.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FailureClassifier {

    public List<FailureFinding> classify(List<FailureEvidence> evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        List<FailureFinding> findings = new ArrayList<>();
        for (FailureEvidence e : evidence) {
            String msg = e.message() == null ? "" : e.message().toLowerCase();
            if (msg.contains("assertionerror") || (msg.contains("expected") && msg.contains("actual"))) {
                FailureFinding f = new FailureFinding(
                        "DF-ASSERT-1",
                        FailureCategory.ASSERTION_FAILURE,
                        FailureOrigin.TEST_CODE,
                        FailureSeverity.HIGH,
                        FailureConfidence.HIGH,
                        "Assertion failure detected",
                        "Deterministic assertion failure based on evidence",
                        "Assertion mismatch between expected and actual",
                        List.of(e.testName() == null ? "" : e.testName()),
                        List.of(),
                        List.of(e),
                        true,
                        false
                );
                findings.add(f);
            } else if (msg.contains("timeout") || msg.contains("timed out")) {
                FailureFinding f = new FailureFinding(
                        "DF-TIMEOUT-1",
                        FailureCategory.TIMEOUT_FAILURE,
                        FailureOrigin.TEST_CODE,
                        FailureSeverity.MEDIUM,
                        FailureConfidence.MEDIUM,
                        "Timeout detected",
                        "Test reported a timeout",
                        "Operation timed out",
                        List.of(e.testName() == null ? "" : e.testName()),
                        List.of(),
                        List.of(e),
                        false,
                        false
                );
                findings.add(f);
            } else {
                // unknown - do not invent
            }
        }
        return List.copyOf(findings);
    }
}