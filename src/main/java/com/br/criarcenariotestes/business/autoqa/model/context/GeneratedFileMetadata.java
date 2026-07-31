package com.br.criarcenariotestes.business.autoqa.model.context;

import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;

/**
 * Metadados de um arquivo gerado, persistidos no MongoDB.
 * O conteúdo do arquivo fica no filesystem — aqui apenas referência.
 */
public record GeneratedFileMetadata(

        String relativePath,

        GeneratedFileOperation operation,

        String generatedHash,

        String storedRelativePath

) {}
