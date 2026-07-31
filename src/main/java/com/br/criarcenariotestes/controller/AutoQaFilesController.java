package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.service.GeneratedFileStorageService;
import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/auto-qa")
@RequiredArgsConstructor
public class AutoQaFilesController {

    private final AutoQaExecutionRepository executionRepository;
    private final GeneratedFileStorageService storageService;
    private final AutoQaProperties properties;

    @GetMapping("/executions/{executionId}/manifest")
    public ResponseEntity<Map> getManifest(@PathVariable String executionId) {
        AutoQaExecutionDocument doc = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution not found"));
        if (doc.getProjectPath() == null) {
            throw new ResponseStatusException(NOT_FOUND, "Project path missing for execution");
        }
        Path generatedDir = storageService.resolveGeneratedDir(executionId, Path.of(doc.getProjectPath()));
        Path manifest = generatedDir.resolve("manifest.json");
        if (!Files.exists(manifest)) {
            throw new ResponseStatusException(NOT_FOUND, "Manifest not found");
        }
        try {
            Map m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(manifest.toFile(), Map.class);
            return ResponseEntity.ok(m);
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "Unable to read manifest");
        }
    }

    @GetMapping("/executions/{executionId}/generated-files")
    public ResponseEntity<List<Map<String, Object>>> getGeneratedFiles(@PathVariable String executionId) {
        Map manifest = getManifest(executionId).getBody();
        if (manifest == null || !manifest.containsKey("files")) {
            return ResponseEntity.ok(List.of());
        }
        Object files = manifest.get("files");
        if (files instanceof List<?> list) {
            return ResponseEntity.ok((List<Map<String, Object>>) list);
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/executions/{executionId}/generated-files/content")
    public ResponseEntity<String> getGeneratedFileContent(
            @PathVariable String executionId,
            @RequestParam("relativePath") String relativePath
    ) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "relativePath is required");
        }
        AutoQaExecutionDocument doc = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution not found"));
        if (doc.getProjectPath() == null) {
            throw new ResponseStatusException(NOT_FOUND, "Project path missing for execution");
        }
        Path generatedDir = storageService.resolveGeneratedDir(executionId, Path.of(doc.getProjectPath()));
        Path filesDir = generatedDir.resolve("files");
        Path target = filesDir.resolve(relativePath).normalize();
        if (!target.startsWith(filesDir) || !Files.exists(target)) {
            throw new ResponseStatusException(NOT_FOUND, "File not found");
        }
        try {
            return ResponseEntity.ok(Files.readString(target));
        } catch (IOException e) {
            throw new ResponseStatusException(NOT_FOUND, "Unable to read file");
        }
    }

    @GetMapping("/executions/{executionId}/files/**")
    public ResponseEntity<FileSystemResource> getFile(
            @PathVariable String executionId,
            HttpServletRequest request
    ) {
        String relativePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (relativePath == null) {
            throw new ResponseStatusException(NOT_FOUND, "File path required");
        }
        
        // Remove prefix from path
        String prefix = "/api/auto-qa/executions/" + executionId + "/files/";
        if (relativePath.startsWith(prefix)) {
            relativePath = relativePath.substring(prefix.length());
        }
        
        if (relativePath.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "File path required");
        }
        return getFile(executionId, relativePath);
    }

    // Mantido para compatibilidade com testes/unitários existentes.
    public ResponseEntity<FileSystemResource> getFile(
            String executionId,
            String relativePath
    ) {
        AutoQaExecutionDocument doc = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution not found"));
        if (doc.getProjectPath() == null) {
            throw new ResponseStatusException(NOT_FOUND, "Project path missing for execution");
        }
        Path generatedDir = storageService.resolveGeneratedDir(executionId, Path.of(doc.getProjectPath()));
        Path filesDir = generatedDir.resolve("files");
        Path target = filesDir.resolve(relativePath).normalize();
        if (!target.startsWith(filesDir) || !Files.exists(target)) {
            throw new ResponseStatusException(NOT_FOUND, "File not found");
        }
        String contentType = null;
        try { contentType = Files.probeContentType(target); } catch (IOException ignored) {}
        FileSystemResource resource = new FileSystemResource(target.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + target.getFileName().toString() + "\"")
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/executions/{executionId}/download")
    public void downloadZip(@PathVariable String executionId, HttpServletResponse response) throws IOException {
        AutoQaExecutionDocument doc = executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Execution not found"));
        if (doc.getProjectPath() == null) {
            throw new ResponseStatusException(NOT_FOUND, "Project path missing for execution");
        }
        Path generatedDir = storageService.resolveGeneratedDir(executionId, Path.of(doc.getProjectPath()));
        Path filesDir = generatedDir.resolve("files");
        if (!Files.exists(filesDir)) {
            throw new ResponseStatusException(NOT_FOUND, "No generated files");
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auto-qa-" + executionId + ".zip");

        try (OutputStream os = response.getOutputStream(); ZipOutputStream zos = new ZipOutputStream(os)) {
            Files.walk(filesDir)
                    .filter(p -> Files.isRegularFile(p))
                    .forEach(p -> {
                        ZipEntry entry = new ZipEntry(filesDir.relativize(p).toString());
                        try (var is = Files.newInputStream(p)) {
                            zos.putNextEntry(entry);
                            is.transferTo(zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            zos.flush();
        }
    }
}
