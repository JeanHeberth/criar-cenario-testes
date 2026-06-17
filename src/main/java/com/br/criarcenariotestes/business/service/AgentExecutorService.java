package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.ai.OpenAiProvider;
import com.br.criarcenariotestes.business.dto.AgentChatRequest;
import com.br.criarcenariotestes.business.dto.AgentChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentExecutorService {

    private final AgentLoaderService agentLoaderService;
    private final OpenAiProvider openAiProvider;

    public AgentChatResponse executeAgent(AgentChatRequest request) {
        String instructions = agentLoaderService.loadAgentInstructions(request.agent());
        String response = openAiProvider.gerarResposta(instructions, request.message());
        return new AgentChatResponse(response);
    }
}

