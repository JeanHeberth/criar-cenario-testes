package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.ChatHistoryRequest;
import com.br.criarcenariotestes.business.dto.ChatHistoryResponse;
import com.br.criarcenariotestes.business.dto.ChatMessageDto;
import com.br.criarcenariotestes.infrastructure.entity.chat.ChatMessage;
import com.br.criarcenariotestes.infrastructure.entity.chat.ChatSession;
import com.br.criarcenariotestes.infrastructure.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final AiProviderResolver aiProviderResolver;
    private final AgentLoaderService agentLoaderService;

    public ChatHistoryResponse processMessage(ChatHistoryRequest request) {
        // Busca ou cria sessão
        ChatSession session = chatSessionRepository.findBySessionId(request.sessionId())
                .orElseGet(() -> ChatSession.builder()
                        .sessionId(request.sessionId())
                        .agentId(request.agentId())
                        .build());

        // Carrega as instruções do agente
        String systemPrompt = agentLoaderService.loadAgentInstructions(request.agentId());

        // Salva mensagem do usuário
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content(request.message())
                .build();
        session.getMessages().add(userMessage);

        // Monta histórico para a IA
        List<Map<String, String>> history = session.getMessages().stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .toList();

        // Chama o provider ativo
        AiProvider provider = aiProviderResolver.getActiveProvider();
        log.info("Chamando provider={} para sessionId={} agentId={}",
                provider.getName(), request.sessionId(), request.agentId());

        String aiResponse;
        try {
            aiResponse = provider.gerarRespostaComHistorico(systemPrompt, history);
        } catch (Exception ex) {
            log.error("Erro ao chamar provider={}", provider.getName(), ex);
            aiResponse = "❌ Desculpe, ocorreu um erro ao processar sua mensagem. Tente novamente.";
        }

        // Salva resposta da IA
        ChatMessage assistantMessage = ChatMessage.builder()
                .role("assistant")
                .content(aiResponse)
                .build();
        session.getMessages().add(assistantMessage);
        session.setUpdatedAt(LocalDateTime.now());
        session.setAgentId(request.agentId());

        chatSessionRepository.save(session);

        return toResponse(session);
    }

    public ChatHistoryResponse getHistory(String sessionId) {
        return chatSessionRepository.findBySessionId(sessionId)
                .map(this::toResponse)
                .orElse(new ChatHistoryResponse(sessionId, null, List.of()));
    }

    public void clearSession(String sessionId) {
        chatSessionRepository.findBySessionId(sessionId)
                .ifPresent(chatSessionRepository::delete);
        log.info("Sessão removida: {}", sessionId);
    }

    private ChatHistoryResponse toResponse(ChatSession session) {
        List<ChatMessageDto> messages = session.getMessages().stream()
                .map(m -> new ChatMessageDto(m.getRole(), m.getContent(), m.getTimestamp()))
                .toList();
        return new ChatHistoryResponse(session.getSessionId(), session.getAgentId(), messages);
    }
}

