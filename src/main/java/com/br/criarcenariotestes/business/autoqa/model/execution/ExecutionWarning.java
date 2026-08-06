package com.br.criarcenariotestes.business.autoqa.model.execution;

public record ExecutionWarning(
        String code,
        String description,
        boolean blocking
) {
    public ExecutionWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }
}
