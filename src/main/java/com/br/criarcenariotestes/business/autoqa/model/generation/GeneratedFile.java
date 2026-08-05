package com.br.criarcenariotestes.business.autoqa.model.generation;

import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO não confiável (recebido da IA ou construído pelo GenerationService).
 * A validação semântica completa ocorre em GenerationValidator.
 */
public record GeneratedFile(
        String relativePath,
        GeneratedFileOperation operation,
        PlanComponentType componentType,
        String content,
        String encoding,
        String sha256,
        GeneratedFileStatus status,
        boolean existingFile,
        List<String> reusedComponents,
        List<String> dependencies,
        List<GenerationWarning> warnings
) {
    public GeneratedFile {
        relativePath = relativePath == null ? null : relativePath.trim();
        encoding = encoding == null || encoding.isBlank() ? "UTF-8" : encoding.trim();
        sha256 = sha256 == null ? null : sha256.trim();
        reusedComponents = copyList(reusedComponents);
        dependencies = copyList(dependencies);
        warnings = copyList(warnings);
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
