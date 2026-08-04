package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.FileToApply;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FileApplicationService")
class FileApplicationServiceTest {

    @TempDir
    Path projectDir;

    private FileApplicationService service;
    private AutoQaProperties props;

    @BeforeEach
    void setUp() {
        props = new AutoQaProperties();
        props.setAllowFileApplication(true);
        props.setBackupDirectory(".auto-qa/backups");
        service = new FileApplicationService(props, new BackupService(props));
    }

    // ─── Aplicação de arquivos ────────────────────────────────────────────────

    @Nested
    @DisplayName("Aplicação de arquivos")
    class FileApplication {

        @Test
        @DisplayName("deve rejeitar quando allowFileApplication=false")
        void rejectsWhenNotAllowed() {
            props.setAllowFileApplication(false);
            List<FileToApply> files = List.of(new FileToApply("tests/test.ts", GeneratedFileOperation.CREATE, "content"));
            
            try {
                service.applyFiles("exec-123", projectDir, files);
                fail("Should have thrown");
            } catch (Exception ex) {
                assertThat(ex.getMessage()).contains("not allowed");
            }
        }

        @Test
        @DisplayName("deve criar arquivo novo com operação CREATE")
        void createsNewFile() throws Exception {
            FileToApply f = new FileToApply("tests/new.spec.ts", GeneratedFileOperation.CREATE, "test('new', () => {});");
            
            service.applyFiles("exec-123", projectDir, List.of(f));
            
            Path created = projectDir.resolve("tests/new.spec.ts");
            assertThat(created).exists();
            assertThat(Files.readString(created)).contains("test('new'");
        }

        @Test
        @DisplayName("deve fazer backup antes de sobrescrever arquivo existente")
        void backupsBeforeOverwrite() throws Exception {
            Path existing = projectDir.resolve("tests/old.ts");
            Files.createDirectories(existing.getParent());
            Files.writeString(existing, "old content");

            FileToApply f = new FileToApply("tests/old.ts", GeneratedFileOperation.CREATE, "new content");
            
            service.applyFiles("exec-123", projectDir, List.of(f));
            
            // Arquivo deve ter novo conteúdo
            assertThat(Files.readString(existing)).isEqualTo("new content");
            
            // Backup deve existir
            Path backupDir = projectDir.resolve(".auto-qa/backups");
            assertThat(backupDir).exists();
        }

        @Test
        @DisplayName("deve rejeitar path com traversal (../../)")
        void rejectsPathTraversal() {
            FileToApply f = new FileToApply("../../secret.ts", GeneratedFileOperation.CREATE, "content");
            
            try {
                service.applyFiles("exec-123", projectDir, List.of(f));
                fail("Should have thrown");
            } catch (Exception ex) {
                assertThat(ex.getMessage()).contains("invalid");
            }
        }

        @Test
        @DisplayName("deve rejeitar arquivo com path absoluto")
        void rejectsAbsolutePath() {
            FileToApply f = new FileToApply("/etc/passwd", GeneratedFileOperation.CREATE, "content");
            
            try {
                service.applyFiles("exec-123", projectDir, List.of(f));
                fail("Should have thrown");
            } catch (Exception ex) {
                assertThat(ex.getMessage()).contains("invalid");
            }
        }

        @Test
        @DisplayName("deve criar subdiretórios necessários")
        void createsNestedDirectories() throws Exception {
            FileToApply f = new FileToApply("deep/nested/path/test.ts", GeneratedFileOperation.CREATE, "content");
            
            service.applyFiles("exec-123", projectDir, List.of(f));
            
            Path file = projectDir.resolve("deep/nested/path/test.ts");
            assertThat(file).exists();
        }

        @Test
        @DisplayName("deve processar múltiplos arquivos")
        void appliesMultipleFiles() throws Exception {
            List<FileToApply> files = List.of(
                    new FileToApply("tests/a.ts", GeneratedFileOperation.CREATE, "a"),
                    new FileToApply("tests/b.ts", GeneratedFileOperation.CREATE, "b"),
                    new FileToApply("tests/c.ts", GeneratedFileOperation.CREATE, "c")
            );
            
            service.applyFiles("exec-123", projectDir, files);
            
            assertThat(projectDir.resolve("tests/a.ts")).exists();
            assertThat(projectDir.resolve("tests/b.ts")).exists();
            assertThat(projectDir.resolve("tests/c.ts")).exists();
        }
    }
}
