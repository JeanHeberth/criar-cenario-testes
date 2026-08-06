package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/** Mapeada para HTTP 409 — conflito de versão otimista (@Version) detectado na persistência. */
public class AutoQaOptimisticLockException extends AutoQaExecutionApiException {
    public AutoQaOptimisticLockException(String message) { super(message); }
    public AutoQaOptimisticLockException(String message, Throwable cause) { super(message, cause); }
}
