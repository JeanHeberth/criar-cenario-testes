package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.BackupResult;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Serviço que cria backups de arquivos antes de aplicar alterações.
 * Backup é armazenado em .auto-qa/backups/<timestamp>/ com estrutura preservada.
 */
@Service
@RequiredArgsConstructor
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private final AutoQaProperties properties;

    public BackupResult createBackup(Path projectPath, String fileReference) {
        String backupId = UUID.randomUUID().toString();
        Path backupDir = projectPath.resolve(properties.getBackupDirectory()).resolve(backupId);
        int filesBackedUp = 0;

        try {
            Files.createDirectories(backupDir);
            log.info("Created backup directory: {}", backupDir);

            // Se fileReference é um arquivo, fazer backup dele
            Path fileToBackup = projectPath.resolve(fileReference).normalize();
            if (Files.exists(fileToBackup) && Files.isRegularFile(fileToBackup)) {
                Path backupTarget = backupDir.resolve(projectPath.relativize(fileToBackup));
                Files.createDirectories(backupTarget.getParent());
                Files.copy(fileToBackup, backupTarget, StandardCopyOption.REPLACE_EXISTING);
                filesBackedUp = 1;
            }

        } catch (IOException ex) {
            log.error("Failed to create backup: {}", ex.getMessage(), ex);
            throw new RuntimeException("Backup failed: " + ex.getMessage(), ex);
        }

        return new BackupResult(
                backupId,
                backupDir.toString(),
                LocalDateTime.now(),
                filesBackedUp
        );
    }
}
