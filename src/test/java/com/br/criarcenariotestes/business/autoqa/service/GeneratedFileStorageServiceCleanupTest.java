package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedFileStorageService cleanup")
class GeneratedFileStorageServiceCleanupTest {

    @TempDir
    Path projectDir;

    @Test
    @DisplayName("deve remover apenas pasta files e preservar manifest")
    void shouldRemoveOnlyFilesFolderAndKeepManifest() throws Exception {
        AutoQaProperties properties = new AutoQaProperties();
        GeneratedFileStorageService service = new GeneratedFileStorageService(properties);

        Path generatedDir = service.resolveGeneratedDir("exec-123", projectDir);
        Path filesDir = generatedDir.resolve("files/tests");
        Files.createDirectories(filesDir);
        Files.writeString(filesDir.resolve("login.spec.ts"), "content");
        Files.writeString(generatedDir.resolve("manifest.json"), "{\"executionId\":\"exec-123\"}");

        service.cleanupStagingFiles("exec-123", projectDir);

        assertThat(generatedDir.resolve("files")).doesNotExist();
        assertThat(generatedDir.resolve("manifest.json")).exists();
    }
}
