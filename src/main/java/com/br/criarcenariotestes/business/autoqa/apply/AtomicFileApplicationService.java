package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyIoException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Escrita atômica no projeto real: arquivo temporário no mesmo diretório do
 * alvo, seguido de move atômico (com fallback seguro quando o filesystem não
 * suporta ATOMIC_MOVE). Nunca executa comandos externos.
 */
@Component
public class AtomicFileApplicationService {

    /** CREATE: o alvo não pode existir; nunca sobrescreve. */
    public void writeCreate(Path target, String content) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (Files.exists(target)) {
            throw new ApplyIoException("Alvo já existe, CREATE não pode sobrescrever: " + target);
        }
        writeAtomically(target, content, false);
    }

    /** UPDATE: o alvo deve existir; substitui de forma controlada. */
    public void writeUpdate(Path target, String content) {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (!Files.exists(target)) {
            throw new ApplyIoException("Alvo não existe, UPDATE requer arquivo existente: " + target);
        }
        writeAtomically(target, content, true);
    }

    private void writeAtomically(Path target, String content, boolean replaceExisting) {
        Path tmp = null;
        try {
            Files.createDirectories(target.getParent());
            tmp = Files.createTempFile(target.getParent(), "apply-", ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            moveAtomicWithFallback(tmp, target, replaceExisting);
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new ApplyIoException("Falha na escrita atômica de " + target, e);
        }
    }

    private void moveAtomicWithFallback(Path source, Path target, boolean replaceExisting) throws IOException {
        try {
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException e) {
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
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
