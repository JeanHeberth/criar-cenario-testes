package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class GeneratedPathResolver {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");

    public Path resolve(Path generatedBaseDir, UUID executionId, String relativePath) {
        Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");
        if (relativePath == null || relativePath.isBlank()) {
            throw new GenerationWriteException("relativePath must not be blank");
        }
        if (PATH_TRAVERSAL.matcher(relativePath).find()) {
            throw new GenerationWriteException("Path traversal detectado: " + relativePath);
        }
        if (ABSOLUTE_UNIX.matcher(relativePath).find()
                || ABSOLUTE_WINDOWS.matcher(relativePath).find()
                || ABSOLUTE_UNC.matcher(relativePath).find()
                || FILE_URI.matcher(relativePath).find()) {
            throw new GenerationWriteException("Caminho absoluto não permitido: " + relativePath);
        }

        Path executionRoot = generatedBaseDir.resolve(executionId.toString()).resolve("files").normalize();
        Path resolved = executionRoot.resolve(relativePath).normalize();

        if (!resolved.startsWith(executionRoot)) {
            throw new GenerationWriteException("Caminho resolvido sai da raiz de execução: " + relativePath);
        }

        rejectSymlinkAncestors(executionRoot, resolved);

        return resolved;
    }

    private void rejectSymlinkAncestors(Path executionRoot, Path resolved) {
        Path current = resolved.getParent();
        while (current != null && current.startsWith(executionRoot)) {
            if (Files.exists(current) && Files.isSymbolicLink(current)) {
                throw new GenerationWriteException("Symlink detectado no caminho: " + current);
            }
            current = current.getParent();
        }
    }
}
