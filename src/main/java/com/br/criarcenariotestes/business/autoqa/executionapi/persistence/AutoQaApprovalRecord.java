package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import java.time.Instant;
import java.util.Objects;

public record AutoQaApprovalRecord(String type, String approvedBy, Instant approvedAt, boolean approved) {
    public AutoQaApprovalRecord {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(approvedBy, "approvedBy must not be null");
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
    }
}
