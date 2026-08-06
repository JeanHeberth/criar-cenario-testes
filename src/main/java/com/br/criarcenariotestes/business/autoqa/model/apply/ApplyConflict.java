package com.br.criarcenariotestes.business.autoqa.model.apply;

import java.util.Objects;

/**
 * Todo ApplyConflict é, por definição, bloqueante: sua simples presença em
 * ApplyResult.conflicts() impede toda a transação de aplicação (nenhum
 * backup, nenhuma escrita, nenhum rollback).
 */
public record ApplyConflict(
        String relativePath,
        String type,
        String message
) {
    public static final String TARGET_ALREADY_EXISTS = "TARGET_ALREADY_EXISTS";
    public static final String TARGET_MISSING = "TARGET_MISSING";
    public static final String ORIGINAL_FILE_CHANGED = "ORIGINAL_FILE_CHANGED";
    public static final String GENERATED_HASH_MISMATCH = "GENERATED_HASH_MISMATCH";
    public static final String MANIFEST_MISMATCH = "MANIFEST_MISMATCH";
    public static final String PATH_SECURITY_VIOLATION = "PATH_SECURITY_VIOLATION";

    public ApplyConflict {
        relativePath = relativePath == null ? null : relativePath.trim();
        Objects.requireNonNull(type, "type must not be null");
        type = type.trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        Objects.requireNonNull(message, "message must not be null");
        message = message.trim();
    }
}
