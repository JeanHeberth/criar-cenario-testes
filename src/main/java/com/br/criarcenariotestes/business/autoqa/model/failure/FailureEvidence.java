package com.br.criarcenariotestes.business.autoqa.model.failure;

import java.util.Objects;

public record FailureEvidence(
        String source,
        String relativePath,
        Integer line,
        String testName,
        String message,
        String excerpt,
        boolean sanitized
) {
    public FailureEvidence {
        Objects.requireNonNull(source, "source must not be null");
        if (relativePath != null) relativePath = relativePath.trim();
        if (line != null && line < 0) throw new IllegalArgumentException("line must be positive");
        if (message != null) message = message.trim();
        if (excerpt != null && excerpt.length() > 512) excerpt = excerpt.substring(0, 512);
    }
}