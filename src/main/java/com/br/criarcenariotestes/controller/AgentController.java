package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.AgentChatRequest;
import com.br.criarcenariotestes.business.dto.AgentChatResponse;
import com.br.criarcenariotestes.business.dto.AgentInfoResponse;
import com.br.criarcenariotestes.business.service.AgentExecutorService;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ResponseEntity<List<AgentInfoResponse>> listAgents() {
        return ResponseEntity.ok(agentLoaderService.listAgents());
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentChatResponse> chatWithAgent(@Valid @RequestBody AgentChatRequest request) {
        return ResponseEntity.ok(agentExecutorService.executeAgent(request));
    }
}

