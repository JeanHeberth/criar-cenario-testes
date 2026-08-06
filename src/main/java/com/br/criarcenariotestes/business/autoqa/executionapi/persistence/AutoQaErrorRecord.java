package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import java.util.Objects;

public record AutoQaErrorRecord(String code, String message) {
    public AutoQaErrorRecord {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
