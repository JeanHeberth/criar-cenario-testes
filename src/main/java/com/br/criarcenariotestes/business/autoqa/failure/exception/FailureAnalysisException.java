package com.br.criarcenariotestes.business.autoqa.failure.exception;

public class FailureAnalysisException extends RuntimeException {
    public FailureAnalysisException(String message) { super(message); }
    public FailureAnalysisException(String message, Throwable cause) { super(message, cause); }
}