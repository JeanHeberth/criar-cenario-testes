package com.br.criarcenariotestes.business.autoqa.model.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Representa o conteúdo seguro de manifest.json: apenas metadados, nunca conteúdo de arquivo.
 */
public record GenerationManifest(
        UUID executionId,
        String framework,
        String language,
        String planStatus,
        String generationStatus,
        String generatedAt,
        List<GenerationManifestFile> files,
        List<GenerationWarning> warnings
) {
    public GenerationManifest {
        framework = framework == null ? null : framework.trim();
        language = language == null ? null : language.trim();
        planStatus = planStatus == null ? null : planStatus.trim();
        generationStatus = generationStatus == null ? null : generationStatus.trim();
        generatedAt = generatedAt == null ? null : generatedAt.trim();
        files = copyList(files);
        warnings = copyList(warnings);
    }

    public record GenerationManifestFile(
            String relativePath,
            String operation,
            String componentType,
            String status,
            String sha256,
            boolean existingFile
    ) {
        public GenerationManifestFile {
            relativePath = relativePath == null ? null : relativePath.trim();
            operation = operation == null ? null : operation.trim();
            componentType = componentType == null ? null : componentType.trim();
            status = status == null ? null : status.trim();
            sha256 = sha256 == null ? null : sha256.trim();
        }
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
