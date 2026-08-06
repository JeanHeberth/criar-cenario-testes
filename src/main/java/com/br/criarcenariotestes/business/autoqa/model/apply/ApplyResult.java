package com.br.criarcenariotestes.business.autoqa.model.apply;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Resultado final da Fase 8, montado exclusivamente por ApplySummaryBuilder.
 * Nunca inclui conteúdo de arquivo, projectPath absoluto ou backup físico.
 * A coerência entre status/valid/rollbackExecuted é responsabilidade de quem
 * constrói o resultado (ApplySummaryBuilder / FileApplicationService), não
 * deste compact constructor.
 */
public record ApplyResult(
        UUID executionId,
        List<AppliedFile> files,
        List<BackupRecord> backups,
        List<ApplyConflict> conflicts,
        List<ApplyWarning> warnings,
        String projectRootReference,
        String backupRoot,
        ApplyStatus status,
        boolean rollbackExecuted,
        boolean valid
) {
    public ApplyResult {
        Objects.requireNonNull(executionId, "executionId must not be null");
        files = files == null ? List.of() : List.copyOf(files);
        backups = backups == null ? List.of() : List.copyOf(backups);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        projectRootReference = projectRootReference == null ? null : projectRootReference.trim();
        backupRoot = backupRoot == null ? null : backupRoot.trim();
        Objects.requireNonNull(status, "status must not be null");
    }
}
