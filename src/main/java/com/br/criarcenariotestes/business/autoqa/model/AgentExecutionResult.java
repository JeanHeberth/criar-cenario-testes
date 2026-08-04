package com.br.criarcenariotestes.business.autoqa.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record AgentExecutionResult(
        boolean success,
        String message,
        LocalDateTime executedAt
) {

    public AgentExecutionResult {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        message = message.trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        Objects.requireNonNull(executedAt, "executedAt must not be null");
    }

    public static AgentExecutionResult success(String message) {
        return new AgentExecutionResult(true, message, LocalDateTime.now());
    }

    public static AgentExecutionResult failure(String message) {
        return new AgentExecutionResult(false, message, LocalDateTime.now());
    }
}
