package com.br.criarcenariotestes.business.autoqa.model.failure;

import java.util.Objects;

public record FailureWarning(String code, String message) {
    public FailureWarning {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}