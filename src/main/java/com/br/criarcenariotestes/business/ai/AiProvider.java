package com.br.criarcenariotestes.business.ai;

import java.util.List;
import java.util.Map;

public interface AiProvider {

    String getName();

    String gerarResposta(String systemPrompt, String userPrompt);

    /**
     * Gera resposta considerando o histórico completo da conversa.
     * Cada item do histórico é um map com keys "role" e "content".
     * Por padrão extrai a última mensagem do usuário — providers devem sobrescrever para suporte multi-turn.
     */
    default String gerarRespostaComHistorico(String systemPrompt, List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return gerarResposta(systemPrompt, "");
        }
        String lastMessage = history.get(history.size() - 1).get("content");
        return gerarResposta(systemPrompt, lastMessage);
    }
}