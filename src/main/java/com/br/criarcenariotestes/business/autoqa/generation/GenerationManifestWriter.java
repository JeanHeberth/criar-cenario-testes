package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Component
public class GenerationManifestWriter {

    static final String MANIFEST_FILE_NAME = "manifest.json";

    private final ObjectMapper objectMapper;

    public GenerationManifestWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy();
    }

    public Path write(Path generatedBaseDir, UUID executionId, GenerationManifest manifest) {
        Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(manifest, "manifest must not be null");

        Path executionRoot = generatedBaseDir.resolve(executionId.toString()).normalize();
        Path manifestPath = executionRoot.resolve(MANIFEST_FILE_NAME);

        Path tmp = null;
        try {
            Files.createDirectories(executionRoot);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
            tmp = Files.createTempFile(executionRoot, "manifest-", ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            moveAtomicWithFallback(tmp, manifestPath);
            return manifestPath;
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new GenerationWriteException("Falha ao escrever manifest", e);
        }
    }

    private void moveAtomicWithFallback(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort de limpeza de arquivo temporário
        }
    }
}
