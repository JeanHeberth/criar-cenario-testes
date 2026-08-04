package com.br.criarcenariotestes.business.autoqa.discovery.scanner;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ProjectScanResult(
        Path root,
        List<Path> files,
        List<String> relativeFiles,
        List<String> warnings
) {
    public ProjectScanResult {
        root = Objects.requireNonNull(root, "root must not be null");
        files = files == null ? List.of() : List.copyOf(files);
        relativeFiles = relativeFiles == null ? List.of() : List.copyOf(relativeFiles);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
