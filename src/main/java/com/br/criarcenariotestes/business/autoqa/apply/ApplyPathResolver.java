package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyConflictException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Única classe autorizada a resolver o caminho físico de um relativePath
 * dentro de uma raiz confiável (projeto real ou área de backup). Nunca
 * modifica nada em disco — apenas resolve e valida.
 */
@Component
public class ApplyPathResolver {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");

    public Path resolve(Path root, String relativePath) {
        Objects.requireNonNull(root, "root must not be null");
        if (relativePath == null || relativePath.isBlank()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "relativePath must not be blank");
        }
        if (PATH_TRAVERSAL.matcher(relativePath).find()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Path traversal detectado: " + relativePath);
        }
        if (ABSOLUTE_UNIX.matcher(relativePath).find()
                || ABSOLUTE_WINDOWS.matcher(relativePath).find()
                || ABSOLUTE_UNC.matcher(relativePath).find()
                || FILE_URI.matcher(relativePath).find()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Caminho absoluto não permitido: " + relativePath);
        }

        Path normalizedRoot = root.normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();

        if (!resolved.equals(normalizedRoot) && !resolved.startsWith(normalizedRoot)) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Caminho resolvido sai da raiz: " + relativePath);
        }

        rejectSymlinkSegments(normalizedRoot, resolved, relativePath);

        return resolved;
    }

    private void rejectSymlinkSegments(Path root, Path resolved, String relativePath) {
        Path current = resolved;
        while (current != null && (current.equals(root) || current.startsWith(root))) {
            if (Files.isSymbolicLink(current)) {
                throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                        "Symlink detectado no caminho: " + current);
            }
            if (current.equals(root)) {
                break;
            }
            current = current.getParent();
        }
    }
}
