package com.br.criarcenariotestes.business.autoqa.knowledge.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record KnowledgeScanResult(
        Path normalizedProjectPath,
        List<KnowledgeFile> files,
        List<String> ignoredPaths,
        List<String> warnings,
        boolean truncated
) {
    public KnowledgeScanResult {
        normalizedProjectPath = Objects.requireNonNull(normalizedProjectPath, "normalizedProjectPath must not be null");
        files = files == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(files));
        ignoredPaths = ignoredPaths == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(ignoredPaths));
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public record KnowledgeFile(
            String relativePath,
            String name,
            String extension,
            long size,
            String content
    ) {
        public KnowledgeFile {
            relativePath = Objects.requireNonNull(relativePath, "relativePath must not be null").replace('\\', '/');
            name = Objects.requireNonNull(name, "name must not be null");
            extension = extension == null ? "" : extension;
            content = Objects.requireNonNull(content, "content must not be null");
        }
    }
}
