package com.br.criarcenariotestes.business.ai;

public interface AiProvider {

    String getName();

    String gerarResposta(String systemPrompt, String userPrompt);
}