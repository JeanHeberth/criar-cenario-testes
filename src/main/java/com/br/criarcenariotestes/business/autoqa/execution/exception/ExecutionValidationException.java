package com.br.criarcenariotestes.business.autoqa.execution.exception;

/**
 * Falha estrutural que impede a execução de sequer começar: ApplyResult em
 * status proibido ou aprovação de execução ausente/negada. Nunca é usada
 * para bloqueios de política de comando (ver CommandNotAllowedException).
 */
public class ExecutionValidationException extends ExecutionException {
    public ExecutionValidationException(String message) { super(message); }
}
