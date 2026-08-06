package com.br.criarcenariotestes.business.autoqa.model.apply;

public record ApplyWarning(
        String code,
        String description,
        String severity,
        boolean blocking
) {
    public ApplyWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
        severity = (severity == null || severity.isBlank()) ? "INFO" : severity.trim();
    }
}
