package com.br.criarcenariotestes.business.autoqa.model.planning;

public record PlanningWarning(
    String code,
    String description,
    boolean requiresHumanDecision
) {
    public PlanningWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }
}
