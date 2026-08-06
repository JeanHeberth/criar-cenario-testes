package com.br.criarcenariotestes.business.autoqa.apply.exception;

/**
 * Falha técnica de I/O durante a escrita atômica (CREATE/UPDATE) ou leitura
 * de hash de arquivo já existente no projeto real.
 */
public class ApplyIoException extends ApplyException {
    public ApplyIoException(String message) { super(message); }
    public ApplyIoException(String message, Throwable cause) { super(message, cause); }
}
