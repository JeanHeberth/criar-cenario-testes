package com.br.criarcenariotestes.business.autoqa.model.context;

import java.time.LocalDateTime;

public record TestExecutionResult(
        String executionId,
        String framework,
        String command,
        int exitCode,
        String stdout,
        String stderr,
        LocalDateTime executedAt
) {
    public boolean success() {
        return exitCode == 0;
    }
}
