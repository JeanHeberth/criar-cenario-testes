package com.br.criarcenariotestes.business.autoqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lançada quando ocorre falha durante a execução de um teste de automação.
 */
public class TestExecutionException extends ResponseStatusException {

    public TestExecutionException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }

    public TestExecutionException(String reason, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
    }

    public static TestExecutionException executionDisabled() {
        return new TestExecutionException(
                "Execução de testes desabilitada. Habilite 'auto-qa.allow-command-execution' no application.yml"
        );
    }

    public static TestExecutionException commandNotAllowed(String command) {
        return new TestExecutionException(
                "Comando não permitido pela política de segurança: " + command
        );
    }

    public static TestExecutionException timeout(long minutes) {
        return new TestExecutionException(
                "Timeout de execução excedido: " + minutes + " minuto(s)"
        );
    }

    public static TestExecutionException processFailed(int exitCode, String stderr) {
        String details = stderr != null && stderr.length() > 300
                ? stderr.substring(0, 300) + "..."
                : stderr;
        return new TestExecutionException(
                "Execução finalizada com exit code " + exitCode + ". Stderr: " + details
        );
    }

    public static TestExecutionException applicationNotApproved() {
        return new TestExecutionException(
                "Os arquivos gerados ainda não foram aprovados e aplicados. Aprove antes de executar"
        );
    }
}
