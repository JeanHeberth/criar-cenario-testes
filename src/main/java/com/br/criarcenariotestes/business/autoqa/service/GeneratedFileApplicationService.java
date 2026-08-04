package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.FileToApply;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.response.FileApplicationResponse;
import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GeneratedFileApplicationService {

    private final FileApplicationService fileApplicationService;
    private final AutoQaExecutionRepository executionRepository;
    private final GeneratedFileStorageService generatedFileStorageService;

    public FileApplicationResponse apply(String executionId, Path projectPath, List<FileToApply> files) {
        AutoQaExecutionDocument execution = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "Execução não encontrada: " + executionId));

        Map<String, String> expectedHashes = execution.getGeneratedFileMetadata() == null
                ? Map.of()
                : execution.getGeneratedFileMetadata().stream()
                .collect(Collectors.toMap(
                        GeneratedFileMetadata::relativePath,
                        GeneratedFileMetadata::generatedHash,
                        (left, right) -> right
                ));

        fileApplicationService.applyFiles(
                executionId,
                projectPath,
                files,
                execution.isAllowFileUpdate(),
                expectedHashes
        );
        generatedFileStorageService.cleanupStagingFiles(executionId, projectPath);
        return new FileApplicationResponse(
                true,
                null,
                files != null ? files.size() : 0,
                "Files applied successfully"
        );
    }
}
