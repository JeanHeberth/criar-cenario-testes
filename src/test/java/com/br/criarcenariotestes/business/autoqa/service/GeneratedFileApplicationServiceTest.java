package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.FileToApply;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeneratedFileApplicationService")
class GeneratedFileApplicationServiceTest {

    @Mock
    private FileApplicationService fileApplicationService;

    @Mock
    private AutoQaExecutionRepository executionRepository;

    @Mock
    private GeneratedFileStorageService generatedFileStorageService;

    @Test
    @DisplayName("deve limpar staging após aplicar com sucesso")
    void shouldCleanupStagingAfterApplySuccess() {
        AutoQaExecutionDocument doc = new AutoQaExecutionDocument();
        doc.setExecutionId("exec-1");
        doc.setAllowFileUpdate(true);
        doc.setGeneratedFileMetadata(List.of(
                new GeneratedFileMetadata("tests/a.spec.ts", GeneratedFileOperation.CREATE, "hash-a", "exec-1/files/tests/a.spec.ts")
        ));

        when(executionRepository.findByExecutionId("exec-1")).thenReturn(Optional.of(doc));

        GeneratedFileApplicationService service = new GeneratedFileApplicationService(
                fileApplicationService,
                executionRepository,
                generatedFileStorageService
        );

        Path projectPath = Path.of("/tmp/project");
        List<FileToApply> files = List.of(
                new FileToApply("tests/a.spec.ts", GeneratedFileOperation.CREATE, "content")
        );

        service.apply("exec-1", projectPath, files);

        verify(fileApplicationService).applyFiles(eq("exec-1"), eq(projectPath), eq(files), eq(true), any());
        verify(generatedFileStorageService).cleanupStagingFiles("exec-1", projectPath);
    }

    @Test
    @DisplayName("não deve limpar staging se aplicação falhar")
    void shouldNotCleanupWhenApplyFails() {
        AutoQaExecutionDocument doc = new AutoQaExecutionDocument();
        doc.setExecutionId("exec-2");
        doc.setAllowFileUpdate(true);
        doc.setGeneratedFileMetadata(List.of());

        when(executionRepository.findByExecutionId("exec-2")).thenReturn(Optional.of(doc));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("erro"))
                .when(fileApplicationService).applyFiles(eq("exec-2"), any(), any(), eq(true), any());

        GeneratedFileApplicationService service = new GeneratedFileApplicationService(
                fileApplicationService,
                executionRepository,
                generatedFileStorageService
        );

        try {
            service.apply("exec-2", Path.of("/tmp/project"), List.of());
        } catch (Exception ignored) {
            // esperado
        }

        verify(generatedFileStorageService, never()).cleanupStagingFiles(any(), any());
    }
}
