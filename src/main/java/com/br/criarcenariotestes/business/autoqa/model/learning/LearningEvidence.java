package com.br.criarcenariotestes.business.autoqa.model.learning;

import java.util.Objects;

/**
 * Evidência imutável de um LearningItem. Nunca contém código completo,
 * stdout/stderr completos, projectPath absoluto ou segredo — apenas
 * metadados já estruturados pelas fases anteriores.
 */
public record LearningEvidence(
        String source,
        String referenceType,
        String referenceId,
        String relativePath,
        String description,
        String excerpt,
        boolean sanitized
) {
    private static final int MAX_EXCERPT_LENGTH = 300;

    public LearningEvidence {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(referenceType, "referenceType must not be null");
        Objects.requireNonNull(description, "description must not be null");
        referenceId = referenceId == null ? null : referenceId.trim();
        relativePath = normalizeRelativePath(relativePath);
        if (excerpt != null && excerpt.length() > MAX_EXCERPT_LENGTH) {
            excerpt = excerpt.substring(0, MAX_EXCERPT_LENGTH);
        }
    }

    private static String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        String trimmed = relativePath.trim().replace('\\', '/');
        if (trimmed.startsWith("/")
                || trimmed.matches("(?i)^[A-Z]:/.*")
                || trimmed.startsWith("../")
                || trimmed.contains("/../")
                || trimmed.equals("..")
                || trimmed.endsWith("/..")) {
            throw new IllegalArgumentException("relativePath must be relative and must not contain path traversal");
        }
        return trimmed;
    }
}
