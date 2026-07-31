package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.BackupResult;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BackupService")
class BackupServiceTest {

    @TempDir
    Path projectDir;

    private BackupService service;
    private AutoQaProperties props;

    @BeforeEach
    void setUp() {
        props = new AutoQaProperties();
        props.setBackupDirectory(".auto-qa/backups");
        service = new BackupService(props);
    }

    // ─── Criação de backup ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Criação de backup")
    class BackupCreation {

        @Test
        @DisplayName("deve criar diretório .auto-qa/backups/<timestamp>/")
        void createsBackupDirectory() {
            BackupResult result = service.createBackup(projectDir, "test-backup");
            Path backupDir = projectDir.resolve(".auto-qa/backups").resolve(result.backupId());
            assertThat(backupDir).exists();
            assertThat(result.backupId()).isNotBlank();
            assertThat(result.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("deve retornar backupPath como String normalizado")
        void returnsNormalizedBackupPath() {
            BackupResult result = service.createBackup(projectDir, "test");
            assertThat(result.backupPath()).isNotBlank();
            assertThat(result.backupPath()).doesNotContain("\\");
        }

        @Test
        @DisplayName("deve fazer backup de arquivos existentes")
        void backupsExistingFiles() throws Exception {
            Path originalFile = projectDir.resolve("tests/login.spec.ts");
            Files.createDirectories(originalFile.getParent());
            Files.writeString(originalFile, "test('login', () => {});");

            BackupResult result = service.createBackup(projectDir, originalFile.toString());

            Path backedUpFile = projectDir.resolve(".auto-qa/backups").resolve(result.backupId()).resolve("tests/login.spec.ts");
            assertThat(backedUpFile).exists();
            assertThat(Files.readString(backedUpFile)).contains("test('login'");
        }

        @Test
        @DisplayName("deve ignorar arquivos que não existem (sem erro)")
        void ignoresNonExistentFiles() {
            BackupResult result = service.createBackup(projectDir, "nonexistent/file.ts");
            assertThat(result.filesBackedUp()).isZero();
        }
    }
}
