package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
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
public class GeneratedFileWriter {

    private final GeneratedPathResolver pathResolver;

    public GeneratedFileWriter(GeneratedPathResolver pathResolver) {
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
    }

    /**
     * Escreve o arquivo na área isolada. Retorna null para REUSE/NONE (nenhuma escrita necessária).
     */
    public Path write(Path generatedBaseDir, UUID executionId, GeneratedFile file) {
        Objects.requireNonNull(file, "file must not be null");
        if (file.operation() == GeneratedFileOperation.REUSE || file.operation() == GeneratedFileOperation.NONE) {
            return null;
        }

        Path target = pathResolver.resolve(generatedBaseDir, executionId, file.relativePath());
        if (Files.exists(target)) {
            throw new GenerationWriteException("Arquivo já existe na área gerada: " + file.relativePath());
        }

        Path tmp = null;
        try {
            Files.createDirectories(target.getParent());
            tmp = Files.createTempFile(target.getParent(), "gen-", ".tmp");
            Files.writeString(tmp, file.content(), StandardCharsets.UTF_8);
            moveAtomicWithFallback(tmp, target);
            return target;
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new GenerationWriteException("Falha ao escrever arquivo: " + file.relativePath(), e);
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
