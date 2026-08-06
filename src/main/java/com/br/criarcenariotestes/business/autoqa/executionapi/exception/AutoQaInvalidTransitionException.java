package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 409 — ação pedida não é permitida no AutoQaWorkflowStatus atual. */
public class AutoQaInvalidTransitionException extends AutoQaExecutionApiException {
    public AutoQaInvalidTransitionException(String message) { super(message); }
}
