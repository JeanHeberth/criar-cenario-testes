package com.br.criarcenariotestes.business.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatHistoryRequest(
        @NotBlank String sessionId,
        @NotBlank String agentId,
        @NotBlank String message
) {
}

