package com.br.criarcenariotestes.business.autoqa.model.scenario;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record BusinessRule(
        String identifier,
        String description,
        boolean explicit
) {
    public BusinessRule {
        identifier = identifier == null ? null : identifier.trim();
        description = description == null ? null : description.trim();
    }
}
