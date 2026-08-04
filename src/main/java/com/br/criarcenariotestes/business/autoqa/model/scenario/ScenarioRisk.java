package com.br.criarcenariotestes.business.autoqa.model.scenario;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record ScenarioRisk(
        String description,
        RiskLevel level,
        String mitigation
) {
    public ScenarioRisk {
        description = description == null ? null : description.trim();
        mitigation = mitigation == null ? null : mitigation.trim();
    }
}
