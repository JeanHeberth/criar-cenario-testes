package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 403 — allow-file-application/allow-command-execution/sensitive-actions-enabled=false. */
public class AutoQaSensitiveActionDisabledException extends AutoQaExecutionApiException {
    public AutoQaSensitiveActionDisabledException(String message) { super(message); }
}
