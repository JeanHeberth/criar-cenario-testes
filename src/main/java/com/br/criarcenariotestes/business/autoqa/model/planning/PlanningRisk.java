package com.br.criarcenariotestes.business.autoqa.model.planning;

public record PlanningRisk(
    String description,
    String impact,
    String mitigation,
    boolean blocking
) {
    public PlanningRisk {
        description = description == null ? null : description.trim();
        impact = impact == null ? null : impact.trim();
        mitigation = mitigation == null ? null : mitigation.trim();
    }
}
