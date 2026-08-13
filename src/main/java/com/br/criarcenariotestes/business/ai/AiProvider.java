package com.br.criarcenariotestes.business.ai;

import java.util.List;
import java.util.Map;

public interface AiProvider {

    String getName();

    String gerarResposta(String systemPrompt, String userPrompt);

    /**
     * FASE15-BUG-006: variante que permite sobrescrever, só nesta chamada, o
     * limite de tokens de saída configurado para o provider (ex.: o gerador
     * de cenários precisa de mais espaço que os demais agentes, para evitar
     * truncamento da resposta). Providers que não sobrescrevem este método
     * simplesmente ignoram o override e usam seu limite configurado padrão.
     */
    default String gerarResposta(String systemPrompt, String userPrompt, Integer maxTokensOverride) {
        return gerarResposta(systemPrompt, userPrompt);
    }

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