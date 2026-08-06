package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyBackupException;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyConflictException;
import com.br.criarcenariotestes.business.autoqa.model.apply.BackupRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cria backups em .auto-qa/backups/&lt;executionId&gt;/files/&lt;relativePath&gt;
 * e escreve o backup-manifest.json correspondente. Só é chamado para UPDATE.
 * Qualquer falha aqui bloqueia toda a transação de aplicação (ApplyBackupException).
 */
@Component
public class FileBackupService {

    static final String MANIFEST_FILE_NAME = "backup-manifest.json";

    private final ApplyPathResolver pathResolver;
    private final ApplyHashValidator hashValidator;
    private final ObjectMapper objectMapper;

    public FileBackupService(ApplyPathResolver pathResolver, ApplyHashValidator hashValidator, ObjectMapper objectMapper) {
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
        this.hashValidator = Objects.requireNonNull(hashValidator, "hashValidator must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy();
    }

    /**
     * Copia sourceFile (arquivo original, ainda intocado no projeto real) para
     * a área de backup, preservando bytes exatos. Nunca sobrescreve um backup
     * já existente para o mesmo relativePath na mesma execução.
     */
    public BackupRecord backup(Path backupBaseDir, UUID executionId, String relativePath, Path sourceFile) {
        Objects.requireNonNull(backupBaseDir, "backupBaseDir must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(sourceFile, "sourceFile must not be null");

        Path filesRoot = backupBaseDir.resolve(executionId.toString()).resolve("files").normalize();
        Path backupTarget;
        try {
            backupTarget = pathResolver.resolve(filesRoot, relativePath);
        } catch (ApplyConflictException e) {
            throw new ApplyBackupException("Caminho de backup inseguro para " + relativePath, e);
        }

        try {
            Files.createDirectories(backupTarget.getParent());
            Files.copy(sourceFile, backupTarget);
        } catch (IOException e) {
            throw new ApplyBackupException("Falha ao criar backup de " + relativePath, e);
        }

        String sha256 = hashValidator.sha256OfFile(backupTarget);
        return new BackupRecord(relativePath, "files/" + relativePath, sha256, LocalDateTime.now());
    }

    /** Escreve backup-manifest.json de forma atômica, uma única vez, ao final dos backups. */
    public Path writeManifest(Path backupBaseDir, UUID executionId, List<BackupRecord> records) {
        Objects.requireNonNull(backupBaseDir, "backupBaseDir must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");

        Path executionRoot = backupBaseDir.resolve(executionId.toString()).normalize();
        Path manifestPath = executionRoot.resolve(MANIFEST_FILE_NAME);

        List<ManifestEntry> entries = (records == null ? List.<BackupRecord>of() : records).stream()
                .map(r -> new ManifestEntry(r.relativePath(), r.backupRelativePath(), r.sha256(), r.backedUpAt().toString()))
                .toList();

        Path tmp = null;
        try {
            Files.createDirectories(executionRoot);
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new BackupManifest(executionId, entries));
            tmp = Files.createTempFile(executionRoot, "backup-manifest-", ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            moveAtomicWithFallback(tmp, manifestPath);
            return manifestPath;
        } catch (IOException e) {
            deleteQuietly(tmp);
            throw new ApplyBackupException("Falha ao escrever backup-manifest.json", e);
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

    public record BackupManifest(UUID executionId, List<ManifestEntry> backups) {}

    public record ManifestEntry(String relativePath, String backupRelativePath, String sha256, String backedUpAt) {}
}
