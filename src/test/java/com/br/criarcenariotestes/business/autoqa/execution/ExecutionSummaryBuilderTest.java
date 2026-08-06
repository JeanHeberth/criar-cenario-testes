package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionWarning;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionSummaryBuilder - Testes Unitários")
class ExecutionSummaryBuilderTest {

    private final ExecutionSummaryBuilder builder = new ExecutionSummaryBuilder();

    private CommandSpecification command() {
        return new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "projeto",
                Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST);
    }

    private ProcessExecutionService.ProcessOutcome outcome(int exitCode, String stdout, String stderr,
                                                            boolean stdoutTrunc, boolean stderrTrunc) {
        Instant now = Instant.now();
        return new ProcessExecutionService.ProcessOutcome(exitCode, now, now.plusSeconds(2), stdout, stderr, stdoutTrunc, stderrTrunc);
    }

    @Test
    @DisplayName("Deve montar resultado BLOCKED com o warning informado")
    void deveMontarBlocked() {
        UUID executionId = UUID.randomUUID();
        ExecutionWarning warning = new ExecutionWarning("COMMAND_NOT_ALLOWED", "bloqueado", true);

        ExecutionResult result = builder.buildBlocked(executionId, null, warning);

        assertThat(result.status()).isEqualTo(ExecutionStatus.BLOCKED);
        assertThat(result.command()).isNull();
        assertThat(result.warnings()).containsExactly(warning);
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve montar resultado TIMED_OUT com warning TIMEOUT_REACHED")
    void deveMontarTimedOutComWarningTimeout() {
        Instant startedAt = Instant.now();
        Instant finishedAt = startedAt.plusSeconds(600);

        ExecutionResult result = builder.buildTimedOut(UUID.randomUUID(), command(), startedAt, finishedAt,
                "saída parcial", "", false, false);

        assertThat(result.status()).isEqualTo(ExecutionStatus.TIMED_OUT);
        assertThat(result.valid()).isFalse();
        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("TIMEOUT_REACHED"));
        assertThat(result.duration()).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    @DisplayName("Deve montar resultado ERROR com o warning informado")
    void deveMontarErrorComWarning() {
        ExecutionResult result = builder.buildError(UUID.randomUUID(), command(),
                "PROCESS_START_FAILED", "não foi possível iniciar");

        assertThat(result.status()).isEqualTo(ExecutionStatus.ERROR);
        assertThat(result.valid()).isFalse();
        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("PROCESS_START_FAILED"));
    }

    @Test
    @DisplayName("Deve montar PASSED quando exitCode=0")
    void deveMontarPassedQuandoExitCodeZero() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "3 passed", "", false, false), List.of());

        assertThat(result.status()).isEqualTo(ExecutionStatus.PASSED);
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve montar FAILED quando exitCode!=0, com warning EXIT_CODE_NON_ZERO")
    void deveMontarFailedQuandoExitCodeNaoZero() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(1, "1 failed", "", false, false), List.of());

        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.valid()).isTrue();
        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("EXIT_CODE_NON_ZERO"));
    }

    @Test
    @DisplayName("Deve adicionar warning de truncamento de stdout")
    void deveAdicionarWarningDeTruncamentoStdout() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "saida", "", true, false), List.of());

        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("STDOUT_TRUNCATED"));
    }

    @Test
    @DisplayName("Deve adicionar warning de truncamento de stderr")
    void deveAdicionarWarningDeTruncamentoStderr() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "", "erro", false, true), List.of());

        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("STDERR_TRUNCATED"));
    }

    @Test
    @DisplayName("Deve adicionar RESULT_PARSE_FAILED quando summaries vazio e stdout não vazio")
    void deveAdicionarResultParseFailedQuandoSummaryVazioComStdoutNaoVazio() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "saída não reconhecida", "", false, false), List.of());

        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("RESULT_PARSE_FAILED"));
    }

    @Test
    @DisplayName("Não deve adicionar RESULT_PARSE_FAILED quando stdout está vazio")
    void naoDeveAdicionarResultParseFailedQuandoStdoutVazio() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "", "", false, false), List.of());

        assertThat(result.warnings()).noneMatch(w -> w.code().equals("RESULT_PARSE_FAILED"));
    }

    @Test
    @DisplayName("Não deve adicionar RESULT_PARSE_FAILED quando summaries não está vazio")
    void naoDeveAdicionarResultParseFailedQuandoSummaryPresente() {
        TestExecutionSummary summary = new TestExecutionSummary("PYTEST", 1, 1, 0, 0, 0, List.of(), List.of());

        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "1 passed", "", false, false), List.of(summary));

        assertThat(result.warnings()).noneMatch(w -> w.code().equals("RESULT_PARSE_FAILED"));
        assertThat(result.summaries()).containsExactly(summary);
    }

    @Test
    @DisplayName("Deve calcular duration a partir de startedAt/finishedAt")
    void deveCalcularDuration() {
        ExecutionResult result = builder.buildCompleted(UUID.randomUUID(), command(),
                outcome(0, "", "", false, false), List.of());

        assertThat(result.duration()).isEqualTo(Duration.ofSeconds(2));
    }
}
