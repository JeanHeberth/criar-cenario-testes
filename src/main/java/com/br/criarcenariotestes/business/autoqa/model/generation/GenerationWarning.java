package com.br.criarcenariotestes.business.autoqa.model.generation;

import com.fasterxml.jackson.annotation.JsonCreator;

public record GenerationWarning(
        String code,
        String description,
        boolean blocking
) {
    public GenerationWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GenerationWarning fromString(String text) {
        int colonIndex = text.indexOf(':');
        if (colonIndex > 0) {
            return new GenerationWarning(text.substring(0, colonIndex).trim(), text.substring(colonIndex + 1).trim(), false);
        }
        return new GenerationWarning("UNKNOWN", text.trim(), false);
    }
}
