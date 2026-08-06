package com.br.criarcenariotestes.business.autoqa.model.review;

public record ReviewWarning(
        String code,
        String description,
        boolean blocking
) {
    public ReviewWarning {
        code = code == null ? null : code.trim();
        description = description == null ? null : description.trim();
    }
}
