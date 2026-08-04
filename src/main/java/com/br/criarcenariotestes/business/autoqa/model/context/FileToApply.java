package com.br.criarcenariotestes.business.autoqa.model.context;

import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;

/**
 * Arquivo a ser aplicado no projeto de automação.
 */
public record FileToApply(
        String relativePath,
        GeneratedFileOperation operation,
        String content,
        String originalHash,
        String generatedHash
) {
    public FileToApply(String relativePath, GeneratedFileOperation operation, String content) {
        this(relativePath, operation, content, null, null);
    }
}
