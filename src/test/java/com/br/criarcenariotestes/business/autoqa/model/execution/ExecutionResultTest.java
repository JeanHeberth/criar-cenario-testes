package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionResult - Testes Unitários")
class ExecutionResultTest {

    private CommandSpecification command() {
        return new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "projeto",
                Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST);
    }

    @Test
    @DisplayName("Deve criar ExecutionResult PASSED")
    void deveCriarExecutionResultPassed() {
        Instant now = Instant.now();
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                now, now.plusSeconds(5), Duration.ofSeconds(5), "ok", "", false, false, List.of(), List.of(), true);

        assertThat(result.status()).isEqualTo(ExecutionStatus.PASSED);
        assertThat(result.exitCode()).isZero();
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve criar ExecutionResult FAILED")
    void deveCriarExecutionResultFailed() {
        Instant now = Instant.now();
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.FAILED, 1,
                now, now.plusSeconds(5), Duration.ofSeconds(5), "algumas falhas", "", false, false, List.of(), List.of(), true);

        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve criar ExecutionResult TIMED_OUT com exitCode nulo")
    void deveCriarExecutionResultTimedOut() {
        Instant now = Instant.now();
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.TIMED_OUT, null,
                now, now.plusSeconds(600), Duration.ofSeconds(600), "", "", false, false, List.of(),
                List.of(new ExecutionWarning("TIMEOUT_REACHED", "timeout", true)), false);

        assertThat(result.exitCode()).isNull();
        assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("Deve criar ExecutionResult BLOCKED sem processo iniciado")
    void deveCriarExecutionResultBlocked() {
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.BLOCKED, null,
                null, null, null, null, null, false, false, List.of(),
                List.of(new ExecutionWarning("COMMAND_NOT_ALLOWED", "bloqueado", true)), true);

        assertThat(result.status()).isEqualTo(ExecutionStatus.BLOCKED);
        assertThat(result.startedAt()).isNull();
        assertThat(result.exitCode()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> new ExecutionResult(null, command(), ExecutionStatus.BLOCKED, null,
                null, null, null, null, null, false, false, List.of(), List.of(), true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve permitir command nulo quando BLOCKED sem nenhum candidato resolvido")
    void devePermitirCommandNuloQuandoBlockedSemCandidato() {
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), null, ExecutionStatus.BLOCKED, null,
                null, null, null, null, null, false, false, List.of(),
                List.of(new ExecutionWarning("UNSUPPORTED_FRAMEWORK", "framework desconhecido", true)), true);

        assertThat(result.command()).isNull();
        assertThat(result.status()).isEqualTo(ExecutionStatus.BLOCKED);
    }

    @Test
    @DisplayName("Deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        assertThatThrownBy(() -> new ExecutionResult(UUID.randomUUID(), command(), null, null,
                null, null, null, null, null, false, false, List.of(), List.of(), true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar duration negativa")
    void deveRejeitarDurationNegativa() {
        assertThatThrownBy(() -> new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(-1), "", "", false, false, List.of(), List.of(), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "", "", false, false, List.of(), List.of(), true);

        assertThatThrownBy(() -> result.summaries().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.warnings().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Não deve expor projectPath absoluto em nenhum campo do record")
    void naoDeveExporProjectPath() {
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "ok", "", false, false, List.of(), List.of(), true);

        assertThat(result.toString()).doesNotContain("/Users").doesNotContain("/home");
    }

    @Test
    @DisplayName("Não deve expor variáveis de ambiente sensíveis no toString")
    void naoDeveExporEnvironmentSensivel() {
        CommandSpecification cmd = new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(),
                "projeto", Duration.ofMinutes(1), Map.of("PATH", "/usr/bin"), ExecutionCommandType.TEST);
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), cmd, ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "ok", "", false, false, List.of(), List.of(), true);

        assertThat(result.toString()).doesNotContainIgnoringCase("token").doesNotContainIgnoringCase("secret");
    }

    @Test
    @DisplayName("Deve manter contrato estável de campos (não corrigir status no compact constructor)")
    void deveManterContratoJson() {
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.FAILED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "", "", false, false, List.of(), List.of(), true);

        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.exitCode()).isZero();
    }
}
