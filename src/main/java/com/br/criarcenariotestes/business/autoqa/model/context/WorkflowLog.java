package com.br.criarcenariotestes.business.autoqa.model.context;

import java.time.LocalDateTime;

/**
 * Registro imutável de log de uma etapa do workflow Auto QA.
 * Não contém informações sensíveis como senhas, tokens ou conteúdo de .env.
 */
public record WorkflowLog(

        LocalDateTime timestamp,

        String step,

        String message,

        LogLevel level

) {

    public enum LogLevel {
        INFO, WARNING, ERROR
    }

    public static WorkflowLog info(String step, String message) {
        return new WorkflowLog(LocalDateTime.now(), step, message, LogLevel.INFO);
    }

    public static WorkflowLog warning(String step, String message) {
        return new WorkflowLog(LocalDateTime.now(), step, message, LogLevel.WARNING);
    }

    public static WorkflowLog error(String step, String message) {
        return new WorkflowLog(LocalDateTime.now(), step, message, LogLevel.ERROR);
    }
}
