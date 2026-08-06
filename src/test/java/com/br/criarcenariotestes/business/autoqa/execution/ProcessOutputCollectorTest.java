package com.br.criarcenariotestes.business.autoqa.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProcessOutputCollector - Testes Unitários")
class ProcessOutputCollectorTest {

    private final ProcessOutputCollector collector = new ProcessOutputCollector();

    @Test
    @DisplayName("Deve capturar stdout")
    void deveCapturarStdout() {
        FakeProcess process = FakeProcess.completed(0, "saída padrão", "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).isEqualTo("saída padrão");
    }

    @Test
    @DisplayName("Deve capturar stderr")
    void deveCapturarStderr() {
        FakeProcess process = FakeProcess.completed(1, "", "erro padrão");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stderr()).isEqualTo("erro padrão");
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve capturar stdout e stderr em paralelo, mesmo com streams grandes")
    void deveCapturarEmParalelo() {
        String bigStdout = "x".repeat(80_000);
        String bigStderr = "y".repeat(80_000);
        FakeProcess process = FakeProcess.completed(0, bigStdout, bigStderr);

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).isNotEmpty();
        assertThat(output.stderr()).isNotEmpty();
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve evitar deadlock ao ler ambos os streams simultaneamente")
    void deveEvitarDeadlock() {
        String bigStdout = "a".repeat(200_000);
        String bigStderr = "b".repeat(200_000);
        FakeProcess process = FakeProcess.completed(0, bigStdout, bigStderr);

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdoutTruncated()).isTrue();
        assertThat(output.stderrTruncated()).isTrue();
    }

    @Test
    @DisplayName("Deve truncar stdout acima do limite por stream")
    void deveTruncarStdout() {
        String hugeStdout = "z".repeat(ProcessOutputCollector.MAX_STREAM_CHARS + 100);
        FakeProcess process = FakeProcess.completed(0, hugeStdout, "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).hasSize(ProcessOutputCollector.MAX_STREAM_CHARS);
        assertThat(output.stdoutTruncated()).isTrue();
    }

    @Test
    @DisplayName("Deve truncar stderr acima do limite por stream")
    void deveTruncarStderr() {
        String hugeStderr = "w".repeat(ProcessOutputCollector.MAX_STREAM_CHARS + 100);
        FakeProcess process = FakeProcess.completed(1, "", hugeStderr);

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stderr()).hasSize(ProcessOutputCollector.MAX_STREAM_CHARS);
        assertThat(output.stderrTruncated()).isTrue();
    }

    @Test
    @DisplayName("Deve preservar a parte final da saída ao truncar")
    void devePreservarFinalDaSaida() {
        String stdout = "inicio-descartado" + "-".repeat(ProcessOutputCollector.MAX_STREAM_CHARS) + "FINAL_UTIL";
        FakeProcess process = FakeProcess.completed(0, stdout, "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).endsWith("FINAL_UTIL");
        assertThat(output.stdout()).doesNotContain("inicio-descartado");
    }

    @Test
    @DisplayName("Deve usar UTF-8 para caracteres acentuados")
    void deveUsarUtf8() {
        FakeProcess process = FakeProcess.completed(0, "execução concluída com êxito – café", "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).isEqualTo("execução concluída com êxito – café");
    }

    @Test
    @DisplayName("Deve redigir segredo no formato chave=valor")
    void deveRedigirSegredo() {
        FakeProcess process = FakeProcess.completed(0, "API_KEY=sk-1234567890abcdef iniciando testes", "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).doesNotContain("sk-1234567890abcdef");
        assertThat(output.stdout()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir token do tipo Bearer")
    void deveRedigirToken() {
        FakeProcess process = FakeProcess.completed(0, "", "Authorization: Bearer abcdef1234567890");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stderr()).doesNotContain("abcdef1234567890");
        assertThat(output.stderr()).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Não deve armazenar saída sem limite, mesmo para entradas muito grandes")
    void deveNaoArmazenarSemLimite() {
        String enorme = "e".repeat(5_000_000);
        FakeProcess process = FakeProcess.completed(0, enorme, "");

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout().length()).isLessThanOrEqualTo(ProcessOutputCollector.MAX_STREAM_CHARS);
    }

    @Test
    @Timeout(10)
    @DisplayName("Deve ser thread-safe sob chamadas concorrentes")
    void deveSerThreadSafe() throws InterruptedException {
        int concurrentCalls = 10;
        CountDownLatch latch = new CountDownLatch(concurrentCalls);
        List<String> results = new CopyOnWriteArrayList<>();

        IntStream.range(0, concurrentCalls).forEach(i -> new Thread(() -> {
            try {
                FakeProcess process = FakeProcess.completed(0, "saida-" + i, "");
                ProcessOutputCollector.CollectedOutput output = collector.collect(process);
                results.add(output.stdout());
            } finally {
                latch.countDown();
            }
        }).start());

        assertThat(latch.await(8, TimeUnit.SECONDS)).isTrue();
        assertThat(results).hasSize(concurrentCalls);
        IntStream.range(0, concurrentCalls).forEach(i -> assertThat(results).contains("saida-" + i));
    }

    @Test
    @Timeout(5)
    @DisplayName("Deve retornar prontamente mesmo com falha de leitura nos streams")
    void deveRetornarComFalhaDeLeitura() {
        FakeProcess process = FakeProcess.brokenStreams();

        ProcessOutputCollector.CollectedOutput output = collector.collect(process);

        assertThat(output.stdout()).isEmpty();
        assertThat(output.stderr()).isEmpty();
    }
}
