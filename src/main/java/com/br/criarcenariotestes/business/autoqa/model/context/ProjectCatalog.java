package com.br.criarcenariotestes.business.autoqa.model.context;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Catálogo completo do projeto de automação obtido pelo scanner.
 * Não contém informações sensíveis — .env e credenciais são excluídos pelo scanner.
 */
@Getter
@Builder
public class ProjectCatalog {

    private final Path projectRoot;

    private final List<ProjectCatalogEntry> entries;

    private final int totalFilesScanned;

    private final long totalContentBytes;

    private final List<String> ignoredPaths;

    private final List<String> warnings;

    private final LocalDateTime scannedAt;

    public List<ProjectCatalogEntry> prioritizedEntries() {
        return entries.stream()
                .filter(ProjectCatalogEntry::prioritized)
                .collect(Collectors.toList());
    }

    public List<ProjectCatalogEntry> typeScriptEntries() {
        return entries.stream()
                .filter(ProjectCatalogEntry::isTypeScript)
                .collect(Collectors.toList());
    }

    public List<ProjectCatalogEntry> entriesWithContent() {
        return entries.stream()
                .filter(ProjectCatalogEntry::hasContent)
                .collect(Collectors.toList());
    }

    public long totalContentKb() {
        return totalContentBytes / 1024;
    }

    public boolean isEmpty() {
        return entries == null || entries.isEmpty();
    }
}
