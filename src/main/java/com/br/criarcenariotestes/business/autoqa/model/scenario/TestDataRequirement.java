package com.br.criarcenariotestes.business.autoqa.model.scenario;

/**
 * DTO não confiável recebido da IA.
 * A validação semântica completa ocorre em ScenarioAnalysisValidator.
 */
public record TestDataRequirement(
        String name,
        TestDataType type,
        boolean required,
        String description,
        String example
) {
    public TestDataRequirement {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
        example = example == null ? null : example.trim();
    }
}
