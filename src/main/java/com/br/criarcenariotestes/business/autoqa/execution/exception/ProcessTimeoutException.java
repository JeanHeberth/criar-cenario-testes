package com.br.criarcenariotestes.business.autoqa.execution.exception;

/**
 * Sinaliza que o timeout configurado foi atingido e o processo foi
 * encerrado. Carrega a saída parcial já capturada até o momento do
 * encerramento forçado. Capturada por TestExecutionService para montar um
 * ExecutionResult com status TIMED_OUT.
 */
public class ProcessTimeoutException extends ExecutionException {

    private final String stdout;
    private final String stderr;
    private final boolean stdoutTruncated;
    private final boolean stderrTruncated;

    public ProcessTimeoutException(String message, String stdout, String stderr,
                                    boolean stdoutTruncated, boolean stderrTruncated) {
        super(message);
        this.stdout = stdout;
        this.stderr = stderr;
        this.stdoutTruncated = stdoutTruncated;
        this.stderrTruncated = stderrTruncated;
    }

    public String stdout() {
        return stdout;
    }

    public String stderr() {
        return stderr;
    }

    public boolean stdoutTruncated() {
        return stdoutTruncated;
    }

    public boolean stderrTruncated() {
        return stderrTruncated;
    }
}
