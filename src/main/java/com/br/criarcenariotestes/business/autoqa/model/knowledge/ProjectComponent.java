package com.br.criarcenariotestes.business.autoqa.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * DTO de metadados extraídos do projeto.
 * O conteúdo integral do arquivo não é armazenado.
 */
public record ProjectComponent(
        String relativePath,
        String name,
        ComponentType type,
        SourceLanguage language,
        String packageName,
        List<String> declaredClasses,
        List<String> declaredMethods,
        List<String> imports,
        List<String> annotations,
        List<String> tags,
        boolean testComponent,
        boolean reusable,
        List<String> warnings
) {
    public ProjectComponent {
        relativePath = normalizeRelativePath(relativePath);
        name = requireText(name, "name");
        type = type == null ? ComponentType.UNKNOWN : type;
        language = language == null ? SourceLanguage.UNKNOWN : language;
        packageName = packageName == null ? null : packageName.trim();
        declaredClasses = copyList(declaredClasses);
        declaredMethods = copyList(declaredMethods);
        imports = copyList(imports);
        annotations = copyList(annotations);
        tags = copyList(tags);
        warnings = copyList(warnings);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private static String normalizeRelativePath(String relativePath) {
        String normalized = requireText(relativePath, "relativePath").replace('\\', '/');
        if (normalized.startsWith("/")
                || normalized.startsWith("//")
                || normalized.matches("(?i)^[A-Z]:/.*")
                || normalized.startsWith("../")
                || normalized.contains("/../")
                || normalized.endsWith("/..")) {
            throw new IllegalArgumentException("relativePath must be relative");
        }
        return normalized;
    }

    private static <T> List<T> copyList(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
