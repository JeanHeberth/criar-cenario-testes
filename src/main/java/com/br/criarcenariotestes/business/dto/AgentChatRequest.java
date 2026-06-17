package com.br.criarcenariotestes.business.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(
        @NotBlank String agent,
        @NotBlank String message
) {
}

