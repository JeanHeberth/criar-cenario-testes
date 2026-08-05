package com.br.criarcenariotestes.business.autoqa.model.generation;

public record GenerationWarning(
        String code,
        String description,
        boolean blocking
) {
    public GenerationWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }
}
