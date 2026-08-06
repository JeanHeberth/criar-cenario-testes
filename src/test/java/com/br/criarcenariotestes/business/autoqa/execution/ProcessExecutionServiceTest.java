package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessStartException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTimeoutException;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProcessExecutionService - Testes Unitários")
class ProcessExecutionServiceTest {

    private final ProcessOutputCollector outputCollector = new ProcessOutputCollector();
    private final ProcessTerminationService terminationService = new ProcessTerminationService();

    private CommandSpecification command(Duration timeout) {
        return new CommandSpecification(ExecutionCommandId.GRADLE_WRAPPER_TEST, "./gradlew", List.of("test"),
                "projeto", timeout, Map.of("PATH", "/usr/bin"), ExecutionCommandType.TEST);
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve iniciar o processo permitido via ProcessFactory")
    void deveIniciarProcessoPermitido(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "ok", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        ProcessExecutionService.ProcessOutcome outcome = service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(outcome.exitCode()).isZero();
        assertThat(factory.lastProcessBuilder()).isNotNull();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve usar o workingDirectory informado")
    void deveUsarWorkingDirectory(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().directory()).isEqualTo(dir.toFile());
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve passar executable e argumentos como elementos separados")
    void devePassarArgumentosSeparados(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().command()).containsExactly("./gradlew", "test");
    }

    @Test
    @Timeout(5)
    @DisplayName("Não deve usar shell — nenhum elemento do comando é bash/sh/cmd/powershell")
    void deveNaoUsarShell(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().command())
                .noneMatch(part -> part.equals("bash") || part.equals("sh") || part.equals("cmd") || part.equals("powershell"));
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve capturar exitCode zero")
    void deveCapturarExitCodeZero(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "tudo ok", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        ProcessExecutionService.ProcessOutcome outcome = service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.stdout()).isEqualTo("tudo ok");
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve capturar exitCode não zero")
    void deveCapturarExitCodeNaoZero(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(1, "", "falhou"));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        ProcessExecutionService.ProcessOutcome outcome = service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(outcome.exitCode()).isEqualTo(1);
        assertThat(outcome.stderr()).isEqualTo("falhou");
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve aplicar o timeout configurado e lançar ProcessTimeoutException")
    void deveAplicarTimeout(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.hangingRespondsToDestroy(0));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        assertThatThrownBy(() -> service.execute(command(Duration.ofMillis(50)), dir))
                .isInstanceOf(ProcessTimeoutException.class);
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve encerrar o processo ao atingir o timeout")
    void deveEncerrarProcessoNoTimeout(@TempDir Path dir) {
        FakeProcess process = FakeProcess.hangingRespondsToDestroy(0);
        FakeProcessFactory factory = new FakeProcessFactory(process);
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        assertThatThrownBy(() -> service.execute(command(Duration.ofMillis(50)), dir))
                .isInstanceOf(ProcessTimeoutException.class);
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve forçar encerramento quando o processo ignora destroy() no timeout")
    void deveForcarEncerramentoQuandoNecessario(@TempDir Path dir) {
        FakeProcess process = FakeProcess.ignoresDestroy(137);
        FakeProcessFactory factory = new FakeProcessFactory(process);
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        assertThatThrownBy(() -> service.execute(command(Duration.ofMillis(50)), dir))
                .isInstanceOf(ProcessTimeoutException.class);
        assertThat(process.wasForciblyDestroyed()).isTrue();
    }

    @Test
    @Timeout(5)
    @DisplayName("Não deve deixar processo órfão após timeout")
    void deveNaoDeixarProcessoOrfao(@TempDir Path dir) {
        FakeProcess process = FakeProcess.ignoresDestroy(1);
        FakeProcessFactory factory = new FakeProcessFactory(process);
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        assertThatThrownBy(() -> service.execute(command(Duration.ofMillis(50)), dir))
                .isInstanceOf(ProcessTimeoutException.class);
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve limpar o environment herdado antes de aplicar o permitido")
    void deveReduzirEnvironment(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().environment()).containsExactlyEntriesOf(Map.of("PATH", "/usr/bin"));
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve preservar PATH quando presente no CommandSpecification")
    void devePreservarPath(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().environment()).containsEntry("PATH", "/usr/bin");
    }

    @Test
    @Timeout(5)
    @DisplayName("Não deve repassar nenhuma variável fora da allowlist do CommandSpecification")
    void deveRemoverVariavelSensivel(@TempDir Path dir) {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        assertThat(factory.lastProcessBuilder().environment().keySet())
                .noneMatch(key -> key.toUpperCase().contains("KEY") || key.toUpperCase().contains("TOKEN")
                        || key.toUpperCase().contains("SECRET"));
    }

    @Test
    @Timeout(5)
    @DisplayName("Não deve criar ou modificar arquivos no workingDirectory")
    void deveNaoModificarArquivos(@TempDir Path dir) throws IOException {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        service.execute(command(Duration.ofSeconds(5)), dir);

        try (var files = Files.list(dir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve lançar ProcessStartException quando o processo não conseguir iniciar")
    void deveLancarProcessStartExceptionQuandoFalhaAoIniciar(@TempDir Path dir) {
        FakeProcessFactory factory = FakeProcessFactory.failingToStart(new IOException("executável não encontrado"));
        ProcessExecutionService service = new ProcessExecutionService(factory, outputCollector, terminationService);

        assertThatThrownBy(() -> service.execute(command(Duration.ofSeconds(5)), dir))
                .isInstanceOf(ProcessStartException.class);
    }
}
