package com.br.criarcenariotestes.business.autoqa.model.context;

import java.time.LocalDateTime;

/**
 * Resultado de um backup de arquivos.
 */
public record BackupResult(
        String backupId,
        String backupPath,
        LocalDateTime createdAt,
        int filesBackedUp
) {}
