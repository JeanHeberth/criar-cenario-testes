package com.br.criarcenariotestes.business.autoqa.execution;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Duplo de teste de {@link Process}. Nunca inicia um processo real do
 * sistema operacional — usado por todos os testes do pacote execution que
 * precisam simular exit code, timeout, processo que ignora destroy() e
 * falha de leitura, sem depender de nenhum executável externo.
 */
public class FakeProcess extends Process {

    private final int exitCode;
    private final InputStream stdout;
    private final InputStream stderr;
    private final boolean ignoreDestroy;
    private final boolean ignoreDestroyForcibly;
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final AtomicBoolean forciblyDestroyed = new AtomicBoolean(false);
    private final CountDownLatch terminationLatch = new CountDownLatch(1);

    private FakeProcess(int exitCode, InputStream stdout, InputStream stderr, boolean ignoreDestroy, boolean autoTerminate) {
        this(exitCode, stdout, stderr, ignoreDestroy, false, autoTerminate);
    }

    private FakeProcess(int exitCode, InputStream stdout, InputStream stderr, boolean ignoreDestroy,
                         boolean ignoreDestroyForcibly, boolean autoTerminate) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.ignoreDestroy = ignoreDestroy;
        this.ignoreDestroyForcibly = ignoreDestroyForcibly;
        if (autoTerminate) {
            alive.set(false);
            terminationLatch.countDown();
        }
    }

    /** Processo que já terminou no momento em que é retornado (caso comum de exit code 0 ou != 0). */
    public static FakeProcess completed(int exitCode, String stdout, String stderr) {
        return new FakeProcess(exitCode, toStream(stdout), toStream(stderr), false, true);
    }

    /** Processo "travado" que responde normalmente a destroy(). Simula o caminho feliz de timeout. */
    public static FakeProcess hangingRespondsToDestroy(int exitCodeAfterDestroy) {
        return new FakeProcess(exitCodeAfterDestroy, toStream(""), toStream(""), false, false);
    }

    /** Processo que ignora destroy() e só termina com destroyForcibly(). */
    public static FakeProcess ignoresDestroy(int exitCodeAfterForce) {
        return new FakeProcess(exitCodeAfterForce, toStream(""), toStream(""), true, false);
    }

    /** Processo cujos streams lançam IOException ao serem lidos. */
    public static FakeProcess brokenStreams() {
        return new FakeProcess(0, new BrokenInputStream(), new BrokenInputStream(), false, true);
    }

    /** Processo que ignora tanto destroy() quanto destroyForcibly() — nunca termina. */
    public static FakeProcess immortal() {
        return new FakeProcess(-1, toStream(""), toStream(""), true, true, false);
    }

    private static InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public OutputStream getOutputStream() {
        return OutputStream.nullOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return stdout;
    }

    @Override
    public InputStream getErrorStream() {
        return stderr;
    }

    @Override
    public int waitFor() throws InterruptedException {
        terminationLatch.await();
        return exitCode;
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return terminationLatch.await(timeout, unit);
    }

    @Override
    public int exitValue() {
        if (alive.get()) {
            throw new IllegalThreadStateException("process hasn't exited");
        }
        return exitCode;
    }

    @Override
    public void destroy() {
        if (ignoreDestroy) {
            return;
        }
        alive.set(false);
        terminationLatch.countDown();
    }

    @Override
    public Process destroyForcibly() {
        if (ignoreDestroyForcibly) {
            return this;
        }
        forciblyDestroyed.set(true);
        alive.set(false);
        terminationLatch.countDown();
        return this;
    }

    @Override
    public boolean isAlive() {
        return alive.get();
    }

    public boolean wasForciblyDestroyed() {
        return forciblyDestroyed.get();
    }

    private static class BrokenInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("simulated read failure");
        }
    }
}
