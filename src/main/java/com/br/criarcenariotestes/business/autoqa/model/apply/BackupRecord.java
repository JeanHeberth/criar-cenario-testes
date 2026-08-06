package com.br.criarcenariotestes.business.autoqa.model.apply;

import java.time.LocalDateTime;
import java.util.Objects;

public record BackupRecord(
        String relativePath,
        String backupRelativePath,
        String sha256,
        LocalDateTime backedUpAt
) {
    public BackupRecord {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        relativePath = relativePath.trim();
        if (relativePath.isEmpty()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        Objects.requireNonNull(backupRelativePath, "backupRelativePath must not be null");
        backupRelativePath = backupRelativePath.trim();
        if (backupRelativePath.isEmpty()) {
            throw new IllegalArgumentException("backupRelativePath must not be blank");
        }
        Objects.requireNonNull(sha256, "sha256 must not be null");
        sha256 = sha256.trim();
        if (sha256.isEmpty()) {
            throw new IllegalArgumentException("sha256 must not be blank");
        }
        Objects.requireNonNull(backedUpAt, "backedUpAt must not be null");
    }
}
