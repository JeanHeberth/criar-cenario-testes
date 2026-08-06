package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;

import java.time.Instant;
import java.util.Objects;

public record AutoQaStageRecord(AutoQaStage stage, String status, Instant startedAt, Instant finishedAt, String summary) {
    public AutoQaStageRecord {
        Objects.requireNonNull(stage, "stage must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
