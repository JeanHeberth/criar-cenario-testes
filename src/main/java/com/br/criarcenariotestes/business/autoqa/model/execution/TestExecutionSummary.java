package com.br.criarcenariotestes.business.autoqa.model.execution;

import java.util.List;

public record TestExecutionSummary(
        String framework,
        int total,
        int passed,
        int failed,
        int skipped,
        int errors,
        List<String> failedTests,
        List<String> warnings
) {
    public TestExecutionSummary {
        framework = framework == null ? null : framework.trim();
        if (total < 0) throw new IllegalArgumentException("total must not be negative");
        if (passed < 0) throw new IllegalArgumentException("passed must not be negative");
        if (failed < 0) throw new IllegalArgumentException("failed must not be negative");
        if (skipped < 0) throw new IllegalArgumentException("skipped must not be negative");
        if (errors < 0) throw new IllegalArgumentException("errors must not be negative");
        failedTests = failedTests == null ? List.of() : List.copyOf(failedTests);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
