package com.br.criarcenariotestes.business.dto;

import java.time.LocalDateTime;

public record ChatMessageDto(
        String role,
        String content,
        LocalDateTime timestamp
) {
}

