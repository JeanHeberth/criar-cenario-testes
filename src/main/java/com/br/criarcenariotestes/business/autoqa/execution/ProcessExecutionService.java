package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessStartException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTimeoutException;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executa um único CommandSpecification usando ProcessBuilder diretamente,
 * sem shell — executable e arguments sempre separados. É, junto com
 * {@link ProcessFactory}, a única classe do pacote execution autorizada a
 * usar ProcessBuilder. Nunca usa Runtime.exec, bash -c, sh -c, cmd /c,
 * powershell ou eval.
 */
@Component
public class ProcessExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ProcessExecutionService.class);

    private final ProcessFactory processFactory;
    private final ProcessOutputCollector outputCollector;
    private final ProcessTerminationService terminationService;

    public ProcessExecutionService(ProcessFactory processFactory,
                                    ProcessOutputCollector outputCollector,
                                    ProcessTerminationService terminationService) {
        this.processFactory = Objects.requireNonNull(processFactory, "processFactory must not be null");
        this.outputCollector = Objects.requireNonNull(outputCollector, "outputCollector must not be null");
        this.terminationService = Objects.requireNonNull(terminationService, "terminationService must not be null");
    }

    public ProcessOutcome execute(CommandSpecification command, Path workingDirectory) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");

        ProcessBuilder processBuilder = buildProcessBuilder(command, workingDirectory);

        Instant startedAt = Instant.now();
        Process process;
        try {
            process = processFactory.start(processBuilder);
        } catch (IOException e) {
            throw new ProcessStartException("Falha ao iniciar processo " + command.commandId(), e);
        }

        ExecutorService collectorExecutor = Executors.newSingleThreadExecutor(this::newDaemonThread);
        Future<ProcessOutputCollector.CollectedOutput> outputFuture =
                collectorExecutor.submit(() -> outputCollector.collect(process));

        try {
            boolean finishedInTime = waitQuietly(process, command.timeout().toMillis());

            if (!finishedInTime) {
                log.warn("Process timeout reached. commandId={}, timeout={}", command.commandId(), command.timeout());
                terminationService.terminate(process);
                ProcessOutputCollector.CollectedOutput partial = joinOutput(outputFuture);
                throw new ProcessTimeoutException(
                        "Timeout de " + command.timeout() + " atingido para " + command.commandId(),
                        partial.stdout(), partial.stderr(), partial.stdoutTruncated(), partial.stderrTruncated());
            }

            ProcessOutputCollector.CollectedOutput output = joinOutput(outputFuture);
            Instant finishedAt = Instant.now();
            int exitCode = process.exitValue();

            return new ProcessOutcome(exitCode, startedAt, finishedAt,
                    output.stdout(), output.stderr(), output.stdoutTruncated(), output.stderrTruncated());
        } finally {
            collectorExecutor.shutdownNow();
        }
    }

    private ProcessBuilder buildProcessBuilder(CommandSpecification command, Path workingDirectory) {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command.executable());
        fullCommand.addAll(command.arguments());

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(false);

        Map<String, String> environment = processBuilder.environment();
        environment.clear();
        environment.putAll(command.environment());

        return processBuilder;
    }

    private boolean waitQuietly(Process process, long timeoutMillis) {
        try {
            return process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private ProcessOutputCollector.CollectedOutput joinOutput(Future<ProcessOutputCollector.CollectedOutput> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new ProcessOutputCollector.CollectedOutput("", "", false, false);
        }
    }

    private Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "process-execution-output");
        thread.setDaemon(true);
        return thread;
    }

    public record ProcessOutcome(
            int exitCode,
            Instant startedAt,
            Instant finishedAt,
            String stdout,
            String stderr,
            boolean stdoutTruncated,
            boolean stderrTruncated
    ) {
    }
}
