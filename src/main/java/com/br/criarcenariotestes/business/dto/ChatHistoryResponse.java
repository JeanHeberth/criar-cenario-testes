package com.br.criarcenariotestes.business.dto;

import java.util.List;

public record ChatHistoryResponse(
        String sessionId,
        String agentId,
        List<ChatMessageDto> messages
) {
}

