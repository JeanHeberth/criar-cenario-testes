package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.FileToApply;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Serviço que aplica arquivos gerados ao projeto de automação.
 * Cria backup automático antes de sobrescrever.
 * Valida: allowFileApplication, path traversal, operações perigosas.
 */
@Service
@RequiredArgsConstructor
public class FileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(FileApplicationService.class);
    private final AutoQaProperties properties;
    private final BackupService backupService;

    public void applyFiles(String executionId, Path projectPath, List<FileToApply> files) {
        applyFiles(executionId, projectPath, files, true, Map.of());
    }

    public void applyFiles(
            String executionId,
            Path projectPath,
            List<FileToApply> files,
            boolean allowFileUpdate,
            Map<String, String> expectedGeneratedHashes
    ) {
        // Validar se aplicação de arquivos está permitida
        if (!properties.isAllowFileApplication()) {
            throw new IllegalStateException("File application is not allowed (allowFileApplication=false)");
        }

        for (FileToApply file : files) {
            String expectedHash = expectedGeneratedHashes.get(file.relativePath());
            applyFile(executionId, projectPath, file, allowFileUpdate, expectedHash);
        }
    }

    private void applyFile(
            String executionId,
            Path projectPath,
            FileToApply file,
            boolean allowFileUpdate,
            String expectedGeneratedHash
    ) {
        if (file.operation() == null) {
            throw new IllegalArgumentException("operation is required for file: " + file.relativePath());
        }
        if ("DELETE".equalsIgnoreCase(file.operation().name())) {
            throw new IllegalArgumentException("DELETE operation is not allowed in this version");
        }

        // Normalizar caminho
        Path normalizedPath = Path.of(file.relativePath()).normalize();

        // Validar path traversal
        if (normalizedPath.isAbsolute() || normalizedPath.toString().contains("..")) {
            throw new IllegalArgumentException("invalid path: " + file.relativePath());
        }

        Path targetFile = projectPath.resolve(normalizedPath);

        // Dupla validação: garantir que está dentro do projeto
        if (!targetFile.normalize().startsWith(projectPath.normalize())) {
            throw new IllegalArgumentException("invalid path: " + file.relativePath());
        }

        if ("UPDATE".equalsIgnoreCase(file.operation().name()) && !allowFileUpdate) {
            throw new IllegalArgumentException("UPDATE operation is not allowed for this execution");
        }

        if (expectedGeneratedHash != null && !expectedGeneratedHash.isBlank()) {
            String contentHash = sha256(file.content());
            if (!expectedGeneratedHash.equals(contentHash)) {
                throw new IllegalArgumentException("generated hash mismatch for file: " + file.relativePath());
            }
        }

        if (file.generatedHash() != null && !file.generatedHash().isBlank()) {
            String contentHash = sha256(file.content());
            if (!file.generatedHash().equals(contentHash)) {
                throw new IllegalArgumentException("payload generated hash mismatch for file: " + file.relativePath());
            }
        }

        if (Files.exists(targetFile)
                && file.originalHash() != null
                && !file.originalHash().isBlank()) {
            try {
                String currentHash = sha256(Files.readString(targetFile));
                if (!file.originalHash().equals(currentHash)) {
                    throw new IllegalArgumentException("file changed externally since analysis: " + file.relativePath());
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read existing file: " + file.relativePath(), ex);
            }
        }

        // Se arquivo existe e vamos sobrescrever, criar backup antes
        if (Files.exists(targetFile)) {
            backupService.createBackup(projectPath, file.relativePath());
        }

        // Criar diretórios pai se necessário
        try {
            Files.createDirectories(targetFile.getParent());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create parent directories: " + ex.getMessage(), ex);
        }

        // Escrever arquivo (criação/atualização)
        try {
            Files.write(targetFile, file.content().getBytes(StandardCharsets.UTF_8));
            log.info("Applied file: {} ({})", file.relativePath(), file.operation());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write file: " + ex.getMessage(), ex);
        }
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = content != null ? content.getBytes(StandardCharsets.UTF_8) : new byte[0];
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
