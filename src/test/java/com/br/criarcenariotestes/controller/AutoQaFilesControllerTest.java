package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.service.GeneratedFileStorageService;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoQaFilesController - Unit Tests")
class AutoQaFilesControllerTest {

    @Mock
    private AutoQaExecutionRepository executionRepository;

    @Mock
    private GeneratedFileStorageService storageService;

    @Mock
    private AutoQaProperties properties;

    private AutoQaFilesController controller;

    @TempDir
    Path projectDir;

    private String executionId;
    private AutoQaExecutionDocument doc;

    @BeforeEach
    void setUp() throws Exception {
        executionId = UUID.randomUUID().toString();
        controller = new AutoQaFilesController(executionRepository, storageService, properties);

        doc = new AutoQaExecutionDocument();
        doc.setExecutionId(executionId);
        doc.setProjectPath(projectDir.toString());
        doc.setTitle("Test Execution");
    }

    @Test
    @DisplayName("getManifest deve buscar execução e resolver diretório")
    void getManifestShouldResolveDirectoryAndReadManifest() throws IOException {
        // Setup
        Path manifestPath = projectDir.resolve(".auto-qa/generated/" + executionId + "/manifest.json");
        Files.createDirectories(manifestPath.getParent());
        Files.writeString(manifestPath, "{\"executionId\":\"" + executionId + "\",\"files\":[]}");

        when(executionRepository.findByExecutionId(executionId)).thenReturn(Optional.of(doc));
        when(storageService.resolveGeneratedDir(executionId, projectDir)).thenReturn(manifestPath.getParent());

        // Act
        org.springframework.http.ResponseEntity<Map> response = controller.getManifest(executionId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).containsKey("executionId");
        verify(executionRepository, times(1)).findByExecutionId(executionId);
        verify(storageService, times(1)).resolveGeneratedDir(executionId, projectDir);
    }

    @Test
    @DisplayName("getFile deve retornar FileSystemResource para arquivo válido")
    void getFileShouldReturnResource() throws IOException {
        // Setup
        Path filesDir = projectDir.resolve(".auto-qa/generated/" + executionId + "/files");
        Path testFile = filesDir.resolve("tests/login.spec.ts");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, "test('login', () => {});");

        when(executionRepository.findByExecutionId(executionId)).thenReturn(Optional.of(doc));
        when(storageService.resolveGeneratedDir(executionId, projectDir)).thenReturn(filesDir.getParent());

        // Act
        org.springframework.http.ResponseEntity<FileSystemResource> response = controller.getFile(executionId, "tests/login.spec.ts");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getFile deve rejeitar path traversal")
    void getFileShouldRejectPathTraversal() throws IOException {
        // Setup
        Path filesDir = projectDir.resolve(".auto-qa/generated/" + executionId + "/files");
        Files.createDirectories(filesDir);

        when(executionRepository.findByExecutionId(executionId)).thenReturn(Optional.of(doc));
        when(storageService.resolveGeneratedDir(executionId, projectDir)).thenReturn(filesDir.getParent());

        // Act & Assert
        try {
            controller.getFile(executionId, "../../secret.txt");
            assertThat(false).isTrue(); // Should throw
        } catch (Exception ex) {
            assertThat(ex).isNotNull();
        }
    }
}

