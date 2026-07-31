package com.br.criarcenariotestes.business.autoqa.model.context;

/**
 * Sugestão de correção para uma falha específica.
 */
public record FixSuggestion(
        String failureType,      // Tipo de falha associada
        String suggestion,       // Descrição da sugestão
        String codeExample,      // Exemplo de código corrigido
        int priority             // Prioridade (1=baixa, 5=alta)
) {
}
