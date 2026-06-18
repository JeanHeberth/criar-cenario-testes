package com.br.criarcenariotestes.infrastructure.repository;

import com.br.criarcenariotestes.infrastructure.entity.chat.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    Optional<ChatSession> findBySessionId(String sessionId);
}

