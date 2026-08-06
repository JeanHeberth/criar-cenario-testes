package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.model.apply.AppliedFile;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyWarning;
import com.br.criarcenariotestes.business.autoqa.model.apply.BackupRecord;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Monta o ApplyResult final. Sanitiza a referência do projeto real (nunca o
 * path absoluto) e expõe o backupRoot como caminho relativo. Não decide
 * coerência entre status/valid/rollbackExecuted — isso é responsabilidade de
 * quem chama (FileApplicationService), que já resolveu o fluxo.
 */
@Component
public class ApplySummaryBuilder {

    public ApplyResult build(UUID executionId,
                              List<AppliedFile> files,
                              List<BackupRecord> backups,
                              List<ApplyConflict> conflicts,
                              List<ApplyWarning> warnings,
                              Path projectRoot,
                              Path backupRoot,
                              ApplyStatus status,
                              boolean rollbackExecuted,
                              boolean valid) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(status, "status must not be null");

        return new ApplyResult(
                executionId,
                files,
                backups,
                conflicts,
                warnings,
                sanitizeProjectRoot(projectRoot),
                sanitizeBackupRoot(backupRoot),
                status,
                rollbackExecuted,
                valid
        );
    }

    private String sanitizeProjectRoot(Path projectRoot) {
        if (projectRoot == null) {
            return null;
        }
        Path fileName = projectRoot.getFileName();
        return fileName != null ? fileName.toString() : "project";
    }

    private String sanitizeBackupRoot(Path backupRoot) {
        if (backupRoot == null) {
            return null;
        }
        return backupRoot.toString().replace('\\', '/');
    }
}
