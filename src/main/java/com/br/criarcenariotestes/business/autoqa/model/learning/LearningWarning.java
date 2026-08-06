package com.br.criarcenariotestes.business.autoqa.model.learning;

import java.util.Objects;

public record LearningWarning(String code, String description, boolean blocking) {
    public LearningWarning {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
