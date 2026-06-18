package com.br.criarcenariotestes.infrastructure.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    private String sessionId;
    private String agentId;

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}

