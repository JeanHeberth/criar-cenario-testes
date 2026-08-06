package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExecutionEvidenceExtractor {

    public List<FailureEvidence> extract(ExecutionResult execution) {
        Objects.requireNonNull(execution, "execution must not be null");
        List<FailureEvidence> out = new ArrayList<>();
        if (execution.summaries() != null) {
            for (TestExecutionSummary s : execution.summaries()) {
                for (String t : s.failedTests()) {
                    FailureEvidence ev = new FailureEvidence(
                            "test-summary",
                            null,
                            null,
                            t,
                            "test failed",
                            t.length() > 256 ? t.substring(0, 256) : t,
                            true
                    );
                    out.add(ev);
                }
            }
        }
        if (execution.stderr() != null && !execution.stderr().isBlank()) {
            String stderr = execution.stderr();
            List<String> lines = List.of(stderr.split("\n"));
            for (String l : lines.stream().filter(x -> x.contains("AssertionError") || x.contains("Exception") || x.toLowerCase().contains("error")).collect(Collectors.toList())) {
                FailureEvidence ev = new FailureEvidence("stderr", null, null, null, l.trim(), l.length() > 512 ? l.substring(0,512) : l, true);
                out.add(ev);
            }
        }
        return out.stream().distinct().collect(Collectors.toList());
    }
}