package com.br.criarcenariotestes.business.autoqa.model.scenario;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record ScenarioAmbiguity(
        String description,
        String question,
        boolean blocking
) {
    public ScenarioAmbiguity {
        description = description == null ? null : description.trim();
        question = question == null ? null : question.trim();
    }
}
