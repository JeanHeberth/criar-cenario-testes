package com.br.criarcenariotestes.business.autoqa.apply.exception;

/**
 * Falha técnica ao criar backup. Bloqueia toda a transação de aplicação —
 * nenhum arquivo é escrito se o backup de um UPDATE falhar.
 */
public class ApplyBackupException extends ApplyException {
    public ApplyBackupException(String message) { super(message); }
    public ApplyBackupException(String message, Throwable cause) { super(message, cause); }
}
