package com.br.criarcenariotestes.business.autoqa.model.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO não confiável (recebido parcialmente da IA e completado pelo GenerationService).
 * A validação semântica completa ocorre em GenerationValidator.
 */
public record GenerationResult(
        UUID executionId,
        String framework,
        String language,
        List<GeneratedFile> files,
        List<String> reusedFiles,
        List<GenerationWarning> warnings,
        String generatedRoot,
        String manifestRelativePath,
        GenerationStatus status,
        GenerationConfidence confidence,
        boolean valid
) {
    public GenerationResult {
        framework = framework == null ? null : framework.trim();
        language = language == null ? null : language.trim();
        files = copyList(files);
        reusedFiles = copyList(reusedFiles);
        warnings = copyList(warnings);
        generatedRoot = generatedRoot == null ? null : generatedRoot.trim();
        manifestRelativePath = manifestRelativePath == null ? null : manifestRelativePath.trim();
        confidence = confidence == null ? GenerationConfidence.UNKNOWN : confidence;
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
