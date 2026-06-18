package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.AgentChatRequest;
import com.br.criarcenariotestes.business.dto.AgentChatResponse;
import com.br.criarcenariotestes.business.dto.AgentInfoResponse;
import com.br.criarcenariotestes.business.dto.ChatHistoryRequest;
import com.br.criarcenariotestes.business.dto.ChatHistoryResponse;
import com.br.criarcenariotestes.business.service.AgentExecutorService;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import com.br.criarcenariotestes.business.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentLoaderService agentLoaderService;
    private final AgentExecutorService agentExecutorService;
    private final ChatSessionService chatSessionService;

    @GetMapping
    public ResponseEntity<List<AgentInfoResponse>> listAgents() {
        return ResponseEntity.ok(agentLoaderService.listAgents());
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentChatResponse> chatWithAgent(@Valid @RequestBody AgentChatRequest request) {
        return ResponseEntity.ok(agentExecutorService.executeAgent(request));
    }

    // ─── Chat com histórico de sessão ──────────────────────────────────────────

    @PostMapping("/sessions/chat")
    public ResponseEntity<ChatHistoryResponse> chatWithSession(@Valid @RequestBody ChatHistoryRequest request) {
        return ResponseEntity.ok(chatSessionService.processMessage(request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatHistoryResponse> getSessionHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatSessionService.getHistory(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatSessionService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
