package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 422 — snapshot incompleto ou impossível de reidratar. */
public class AutoQaSnapshotException extends AutoQaExecutionApiException {
    public AutoQaSnapshotException(String message) { super(message); }
    public AutoQaSnapshotException(String message, Throwable cause) { super(message, cause); }
}
