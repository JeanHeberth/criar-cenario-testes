package com.br.criarcenariotestes.business.autoqa.execution.exception;

/**
 * Falha ao confirmar o encerramento de um processo (nem destroy() nem
 * destroyForcibly() conseguiram terminá-lo dentro do prazo).
 */
public class ProcessTerminationException extends ExecutionException {
    public ProcessTerminationException(String message) { super(message); }
}
