package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import java.util.Objects;

public record AutoQaWarningRecord(String code, String description, boolean blocking) {
    public AutoQaWarningRecord {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
