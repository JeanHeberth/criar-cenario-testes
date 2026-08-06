package com.br.criarcenariotestes.business.autoqa.execution;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * Captura stdout e stderr de um {@link Process} em paralelo (duas threads),
 * evitando o deadlock clássico de ler um stream sequencialmente enquanto o
 * outro enche o buffer do sistema operacional e trava o processo filho.
 * Nunca bloqueia indefinidamente: o executor é sempre encerrado em finally,
 * mesmo em caso de falha de leitura.
 */
@Component
public class ProcessOutputCollector {

    static final int MAX_STREAM_CHARS = 50_000;

    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)\\b(password|senha|secret|apikey|api_key|token|private_key)\\b(\\s*[:=]\\s*)[\"']?([^\\s,;\"'`]{3,})");
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile("(?i)\\b(bearer\\s+)([a-z0-9._-]{8,})");

    public CollectedOutput collect(Process process) {
        ExecutorService executor = Executors.newFixedThreadPool(2, this::newDaemonThread);
        try {
            Future<StreamResult> stdoutFuture = executor.submit(readTask(process.getInputStream()));
            Future<StreamResult> stderrFuture = executor.submit(readTask(process.getErrorStream()));

            StreamResult stdout = resolve(stdoutFuture);
            StreamResult stderr = resolve(stderrFuture);

            return new CollectedOutput(
                    redact(stdout.content()), redact(stderr.content()),
                    stdout.truncated(), stderr.truncated()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "process-output-collector");
        thread.setDaemon(true);
        return thread;
    }

    private Callable<StreamResult> readTask(java.io.InputStream input) {
        return () -> readStream(input);
    }

    private StreamResult resolve(Future<StreamResult> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            return new StreamResult("", false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new StreamResult("", false);
        }
    }

    private StreamResult readStream(java.io.InputStream input) {
        StringBuilder buffer = new StringBuilder();
        long totalRead = 0;
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            char[] chunk = new char[4096];
            int n;
            while ((n = reader.read(chunk)) != -1) {
                totalRead += n;
                buffer.append(chunk, 0, n);
                if (buffer.length() > MAX_STREAM_CHARS) {
                    buffer.delete(0, buffer.length() - MAX_STREAM_CHARS);
                }
            }
        } catch (IOException ignored) {
            // conteúdo parcial já capturado é suficiente; a leitura nunca lança para o chamador
        }
        return new StreamResult(buffer.toString(), totalRead > MAX_STREAM_CHARS);
    }

    private String redact(String content) {
        String sanitized = AUTHORIZATION_BEARER.matcher(content).replaceAll("$1[REDACTED]");
        sanitized = KEY_VALUE_SECRET.matcher(sanitized).replaceAll("$1$2[REDACTED]");
        return sanitized;
    }

    private record StreamResult(String content, boolean truncated) {
    }

    public record CollectedOutput(String stdout, String stderr, boolean stdoutTruncated, boolean stderrTruncated) {
    }
}
