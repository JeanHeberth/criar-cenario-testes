package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 409 — operação concorrente/duplicada (lock IN_PROGRESS, aprovação já registrada). */
public class AutoQaExecutionConflictException extends AutoQaExecutionApiException {
    public AutoQaExecutionConflictException(String message) { super(message); }
}
