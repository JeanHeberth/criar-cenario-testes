package com.br.criarcenariotestes.business.autoqa.model.apply;

import java.util.List;
import java.util.Objects;

public record AppliedFile(
        String relativePath,
        ApplyOperation operation,
        ApplyFileStatus status,
        String sourceSha256,
        String appliedSha256,
        boolean backupCreated,
        String backupRelativePath,
        List<ApplyWarning> warnings
) {
    public AppliedFile {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        relativePath = relativePath.trim();
        if (relativePath.isEmpty()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(status, "status must not be null");
        sourceSha256 = sourceSha256 == null ? null : sourceSha256.trim();
        appliedSha256 = appliedSha256 == null ? null : appliedSha256.trim();
        backupRelativePath = backupRelativePath == null ? null : backupRelativePath.trim();
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
