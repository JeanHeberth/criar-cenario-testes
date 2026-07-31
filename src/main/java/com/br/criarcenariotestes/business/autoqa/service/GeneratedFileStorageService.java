package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

/**
 * Serviço que persiste arquivos gerados pela AI em
 * .auto-qa/generated/<executionId>/files/ e cria manifest.json com metadados.
 */
@Service
@RequiredArgsConstructor
public class GeneratedFileStorageService {

    private final AutoQaProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<GeneratedFileMetadata> store(
            String executionId,
            Path projectPath,
            GeneratedCodeResponse response
    ) {
        return store(
                executionId,
                projectPath,
                response,
                AutomationFramework.UNKNOWN,
                AutomationLanguage.UNKNOWN,
                null,
                AutoQaStatus.CODE_GENERATED,
                1
        );
    }

    public List<GeneratedFileMetadata> store(
            String executionId,
            Path projectPath,
            GeneratedCodeResponse response,
            AutomationFramework framework,
            AutomationLanguage language,
            String scenario,
            AutoQaStatus status,
            int revision
    ) {
        if (response == null || response.files() == null || response.files().isEmpty()) {
            return List.of();
        }

        try {
            Path generatedDir = resolveGeneratedDir(executionId, projectPath);
            Path filesDir = generatedDir.resolve("files");
            Files.createDirectories(filesDir);

            List<GeneratedFileMetadata> result = new ArrayList<>();
            List<Map<String, Object>> manifestFiles = new ArrayList<>();

            for (GeneratedFile f : response.files()) {
                if (f == null) continue;
                if (!f.isRelativePath()) continue;
                if (!f.hasContent()) continue;
                if (f.operation() == null) continue;
                if (f.operation().name().equalsIgnoreCase("DELETE")) continue;

                Path target = filesDir.resolve(f.relativePath()).normalize();
                // Segurança: o target deve ficar dentro de filesDir
                if (!target.startsWith(filesDir)) {
                    continue;
                }
                Files.createDirectories(target.getParent());

                // Escrever conteúdo via arquivo temporário e mover
                Path tmp = Files.createTempFile(filesDir, "tmp-", ".tmp");
                Files.writeString(tmp, f.content(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ex) {
                    // fallback sem ATOMIC_MOVE
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }

                // Calcular hash SHA-256
                String hash = computeSha256(target);
                long size = Files.size(target);

                // storedRelativePath: caminho relativo dentro do diretório generated (ex: files/tests/..)
                Path storedRel = generatedDir.getFileName().resolve("files").resolve(filesDir.relativize(target));
                String storedRelativePath = storedRel.toString();

                result.add(new GeneratedFileMetadata(f.relativePath(), f.operation(), hash, storedRelativePath));

                Map<String, Object> fileEntry = new HashMap<>();
                fileEntry.put("relativePath", f.relativePath());
                fileEntry.put("storedRelativePath", storedRelativePath);
                fileEntry.put("hash", hash);
                fileEntry.put("size", size);
                fileEntry.put("operation", f.operation().name());
                manifestFiles.add(fileEntry);
            }

            // Criar manifest
            Map<String, Object> manifest = new HashMap<>();
            manifest.put("executionId", executionId);
            manifest.put("timestamp", Instant.now().toString());
            manifest.put("projectPath", projectPath.toString());
            manifest.put("framework", framework != null ? framework.name() : "UNKNOWN");
            manifest.put("language", language != null ? language.name() : "UNKNOWN");
            manifest.put("scenario", scenario);
            manifest.put("revision", revision);
            manifest.put("status", status != null ? status.name() : AutoQaStatus.CODE_GENERATED.name());
            manifest.put("files", manifestFiles);
            mapper.writeValue(generatedDir.resolve("manifest.json" ).toFile(), manifest);

            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store generated files", e);
        }
    }

    public Path resolveGeneratedDir(String executionId, Path projectPath) {
        Path base = projectPath.resolve(properties.getGeneratedDirectory()).normalize();
        return base.resolve(executionId).normalize();
    }

    /**
     * Remove apenas os arquivos gerados em staging (subpasta files/),
     * preservando manifest e metadados da execução.
     */
    public void cleanupStagingFiles(String executionId, Path projectPath) {
        Path generatedDir = resolveGeneratedDir(executionId, projectPath);
        Path filesDir = generatedDir.resolve("files").normalize();
        if (!filesDir.startsWith(generatedDir) || !Files.exists(filesDir)) {
            return;
        }

        try (var walk = Files.walk(filesDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new RuntimeException("Failed to delete staging file: " + path, ex);
                        }
                    });
        } catch (IOException ex) {
            throw new RuntimeException("Failed to cleanup staging files", ex);
        }
    }

    private String computeSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Unable to compute hash", e);
        }
    }
}
