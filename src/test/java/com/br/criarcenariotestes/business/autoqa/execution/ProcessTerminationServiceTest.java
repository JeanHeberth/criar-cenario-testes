package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTerminationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProcessTerminationService - Testes Unitários")
class ProcessTerminationServiceTest {

    private final ProcessTerminationService service = new ProcessTerminationService();
    private static final Duration SHORT_GRACE = Duration.ofMillis(100);

    @Test
    @Timeout(5)
    @DisplayName("Deve confirmar término quando destroy() funciona")
    void deveConfirmarTerminoQuandoDestroyFunciona() {
        FakeProcess process = FakeProcess.hangingRespondsToDestroy(0);

        ProcessTerminationService.TerminationOutcome outcome = service.terminate(process, SHORT_GRACE);

        assertThat(outcome.confirmed()).isTrue();
        assertThat(outcome.forced()).isFalse();
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve forçar encerramento quando destroy() é ignorado")
    void deveForcarQuandoDestroyEIgnorado() {
        FakeProcess process = FakeProcess.ignoresDestroy(137);

        ProcessTerminationService.TerminationOutcome outcome = service.terminate(process, SHORT_GRACE);

        assertThat(outcome.confirmed()).isTrue();
        assertThat(outcome.forced()).isTrue();
        assertThat(process.wasForciblyDestroyed()).isTrue();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve confirmar imediatamente quando o processo já está encerrado")
    void deveConfirmarProcessoJaEncerrado() {
        FakeProcess process = FakeProcess.completed(0, "", "");

        ProcessTerminationService.TerminationOutcome outcome = service.terminate(process, SHORT_GRACE);

        assertThat(outcome.confirmed()).isTrue();
        assertThat(outcome.forced()).isFalse();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve lançar ProcessTerminationException quando não conseguir confirmar o término")
    void deveLancarExceptionQuandoNaoConfirmar() {
        FakeProcess process = FakeProcess.immortal();

        assertThatThrownBy(() -> service.terminate(process, SHORT_GRACE))
                .isInstanceOf(ProcessTerminationException.class);
    }

    @Test
    @Timeout(5)
    @DisplayName("Não deve deixar o processo vivo após terminate() bem-sucedido")
    void naoDeveDeixarProcessoVivo() {
        FakeProcess process = FakeProcess.ignoresDestroy(1);

        service.terminate(process, SHORT_GRACE);

        assertThat(process.isAlive()).isFalse();
    }

    @Test
    @DisplayName("terminate() com timeout default deve funcionar para processo já encerrado")
    void terminateComTimeoutDefaultDeveFuncionar() {
        FakeProcess process = FakeProcess.completed(0, "", "");

        ProcessTerminationService.TerminationOutcome outcome = service.terminate(process);

        assertThat(outcome.confirmed()).isTrue();
    }
}
