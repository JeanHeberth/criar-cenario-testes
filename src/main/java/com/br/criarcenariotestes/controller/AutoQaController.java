package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import com.br.criarcenariotestes.business.autoqa.model.request.FileApplicationRequest;
import com.br.criarcenariotestes.business.autoqa.model.request.ProjectValidationRequest;
import com.br.criarcenariotestes.business.autoqa.model.response.AutoQaResponse;
import com.br.criarcenariotestes.business.autoqa.model.response.FileApplicationResponse;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectFolderSelectionResponse;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectValidationResponse;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.service.AutoQaWorkflowService;
import com.br.criarcenariotestes.business.autoqa.service.GeneratedFileApplicationService;
import com.br.criarcenariotestes.business.autoqa.service.ProjectFolderSelectionService;
import com.br.criarcenariotestes.business.autoqa.service.ProjectPathValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/auto-qa")
@RequiredArgsConstructor
public class AutoQaController {

    private static final Logger log = LoggerFactory.getLogger(AutoQaController.class);

    private final ProjectPathValidationService pathValidationService;
    private final ProjectFolderSelectionService projectFolderSelectionService;
    private final AutoQaWorkflowService workflowService;
    private final GeneratedFileApplicationService generatedFileApplicationService;
    private final AutoQaProperties autoQaProperties;

    @PostMapping("/project/validate")
    public ResponseEntity<ProjectValidationResponse> validateProjectPath(
            @Valid @RequestBody ProjectValidationRequest request
    ) {
        checkModuleEnabled();
        log.info("Validando caminho do projeto Auto QA. path=\'{}\'", request.projectPath());
        ProjectValidationResponse response = pathValidationService.validate(request.projectPath());
        HttpStatus status = response.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/project/select-folder")
    public ResponseEntity<ProjectFolderSelectionResponse> selectFolder() {
        checkModuleEnabled();
        log.info("Abrindo seletor de pasta do projeto Auto QA");
        ProjectFolderSelectionResponse response = projectFolderSelectionService.selectFolderAndValidate();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze")
    public ResponseEntity<AutoQaResponse> analyze(
            @Valid @RequestBody AutoQaRequest request
    ) {
        checkModuleEnabled();
        log.info("Iniciando análise Auto QA. title=\'{}\'", request.title());
        AutoQaResponse response = workflowService.analyze(request);
        HttpStatus status = response != null && response.status() != null
                && "ERROR".equals(response.status().name())
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/executions/{executionId}/generate")
    public ResponseEntity<AutoQaResponse> generate(@PathVariable String executionId) {
        checkModuleEnabled();
        return ResponseEntity.ok(workflowService.generate(executionId));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<AutoQaResponse> getExecution(@PathVariable String executionId) {
        checkModuleEnabled();
        AutoQaResponse response = workflowService.getExecution(executionId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Execução não encontrada: " + executionId);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/executions/{executionId}/apply")
    public ResponseEntity<FileApplicationResponse> apply(
            @PathVariable String executionId,
            @Valid @RequestBody FileApplicationRequest request
    ) {
        return applyInternal(executionId, request);
    }

    @PostMapping("/executions/{executionId}/apply-files")
    public ResponseEntity<FileApplicationResponse> applyFiles(
            @PathVariable String executionId,
            @Valid @RequestBody FileApplicationRequest request
    ) {
        return applyInternal(executionId, request);
    }

    @PostMapping("/executions/{executionId}/execute")
    public ResponseEntity<AutoQaResponse> execute(@PathVariable String executionId) {
        checkModuleEnabled();
        return ResponseEntity.ok(workflowService.execute(executionId));
    }

    @PostMapping("/executions/{executionId}/discard")
    public ResponseEntity<AutoQaResponse> discard(@PathVariable String executionId) {
        checkModuleEnabled();
        return ResponseEntity.ok(workflowService.discard(executionId));
    }

    private ResponseEntity<FileApplicationResponse> applyInternal(
            String executionId,
            FileApplicationRequest request
    ) {
        checkModuleEnabled();
        AutoQaResponse execution = workflowService.getExecution(executionId);
        if (execution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Execução não encontrada: " + executionId);
        }

        try {
            log.info("Aplicando arquivos gerados para execution='{}'", executionId);
            FileApplicationResponse response = generatedFileApplicationService.apply(
                    executionId,
                    Path.of(execution.projectPath()),
                    request.files()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            log.warn("File application not allowed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to apply files: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private void checkModuleEnabled() {
        if (!autoQaProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O módulo Auto QA está desabilitado");
        }
    }
}
