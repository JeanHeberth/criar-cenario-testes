package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 403 — auto-qa.enabled=false. */
public class AutoQaExecutionDisabledException extends AutoQaExecutionApiException {
    public AutoQaExecutionDisabledException(String message) { super(message); }
}
