package com.br.criarcenariotestes.business.autoqa.model.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado final da Fase 9, montado exclusivamente por ExecutionSummaryBuilder.
 * Nunca inclui projectPath absoluto ou credenciais. A coerência entre
 * status/exitCode/valid é responsabilidade de quem constrói o resultado, não
 * deste compact constructor. command é nulo apenas quando BLOCKED sem nenhum
 * CommandSpecification resolvido (ex.: framework/build tool UNKNOWN — nenhum
 * candidato determinístico existia para ser validado).
 */
public record ExecutionResult(
        UUID executionId,
        CommandSpecification command,
        ExecutionStatus status,
        Integer exitCode,
        Instant startedAt,
        Instant finishedAt,
        Duration duration,
        String stdout,
        String stderr,
        boolean stdoutTruncated,
        boolean stderrTruncated,
        List<TestExecutionSummary> summaries,
        List<ExecutionWarning> warnings,
        boolean valid
) {
    public ExecutionResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (duration != null && duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        summaries = summaries == null ? List.of() : List.copyOf(summaries);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
