package com.br.criarcenariotestes.business.autoqa.model.planning;

import com.fasterxml.jackson.annotation.JsonCreator;

public record PlanningWarning(
    String code,
    String description,
    boolean requiresHumanDecision
) {
    public PlanningWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PlanningWarning fromString(String text) {
        int colonIndex = text.indexOf(':');
        if (colonIndex > 0) {
            return new PlanningWarning(text.substring(0, colonIndex).trim(), text.substring(colonIndex + 1).trim(), false);
        }
        return new PlanningWarning("UNKNOWN", text.trim(), false);
    }
}
