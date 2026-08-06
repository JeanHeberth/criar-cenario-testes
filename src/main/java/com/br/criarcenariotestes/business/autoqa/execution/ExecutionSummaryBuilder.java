package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionWarning;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Monta o ExecutionResult final para cada desfecho possível (BLOCKED,
 * TIMED_OUT, ERROR, PASSED/FAILED). Decide quais ExecutionWarning
 * acompanham o resultado (truncamento, exitCode não zero, falha de parsing)
 * — nunca corrige status/exitCode incoerentes, apenas monta o que já foi
 * decidido por quem chama.
 */
@Component
public class ExecutionSummaryBuilder {

    public ExecutionResult buildBlocked(UUID executionId, CommandSpecification command, ExecutionWarning warning) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(warning, "warning must not be null");
        return new ExecutionResult(executionId, command, ExecutionStatus.BLOCKED, null, null, null, null,
                null, null, false, false, List.of(), List.of(warning), true);
    }

    public ExecutionResult buildTimedOut(UUID executionId, CommandSpecification command,
                                          Instant startedAt, Instant finishedAt,
                                          String stdout, String stderr,
                                          boolean stdoutTruncated, boolean stderrTruncated) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        List<ExecutionWarning> warnings = new ArrayList<>();
        warnings.add(new ExecutionWarning("TIMEOUT_REACHED", "Timeout de execução atingido", true));
        addTruncationWarnings(warnings, stdoutTruncated, stderrTruncated);
        Duration duration = (startedAt != null && finishedAt != null) ? Duration.between(startedAt, finishedAt) : null;
        return new ExecutionResult(executionId, command, ExecutionStatus.TIMED_OUT, null, startedAt, finishedAt,
                duration, stdout, stderr, stdoutTruncated, stderrTruncated, List.of(), List.copyOf(warnings), false);
    }

    public ExecutionResult buildError(UUID executionId, CommandSpecification command, String warningCode, String message) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        return new ExecutionResult(executionId, command, ExecutionStatus.ERROR, null, null, null, null,
                null, null, false, false, List.of(), List.of(new ExecutionWarning(warningCode, message, true)), false);
    }

    public ExecutionResult buildCompleted(UUID executionId, CommandSpecification command,
                                           ProcessExecutionService.ProcessOutcome outcome,
                                           List<TestExecutionSummary> summaries) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");

        ExecutionStatus status = outcome.exitCode() == 0 ? ExecutionStatus.PASSED : ExecutionStatus.FAILED;

        List<ExecutionWarning> warnings = new ArrayList<>();
        addTruncationWarnings(warnings, outcome.stdoutTruncated(), outcome.stderrTruncated());
        if (status == ExecutionStatus.FAILED) {
            warnings.add(new ExecutionWarning("EXIT_CODE_NON_ZERO", "exitCode=" + outcome.exitCode(), false));
        }

        List<TestExecutionSummary> safeSummaries = summaries == null ? List.of() : summaries;
        boolean stdoutNonBlank = outcome.stdout() != null && !outcome.stdout().isBlank();
        if (safeSummaries.isEmpty() && stdoutNonBlank) {
            warnings.add(new ExecutionWarning("RESULT_PARSE_FAILED",
                    "Não foi possível extrair resumo de testes do stdout", false));
        }

        Duration duration = Duration.between(outcome.startedAt(), outcome.finishedAt());
        return new ExecutionResult(executionId, command, status, outcome.exitCode(), outcome.startedAt(), outcome.finishedAt(),
                duration, outcome.stdout(), outcome.stderr(), outcome.stdoutTruncated(), outcome.stderrTruncated(),
                safeSummaries, List.copyOf(warnings), true);
    }

    private void addTruncationWarnings(List<ExecutionWarning> warnings, boolean stdoutTruncated, boolean stderrTruncated) {
        if (stdoutTruncated) {
            warnings.add(new ExecutionWarning("STDOUT_TRUNCATED", "stdout truncado", false));
        }
        if (stderrTruncated) {
            warnings.add(new ExecutionWarning("STDERR_TRUNCATED", "stderr truncado", false));
        }
    }
}
