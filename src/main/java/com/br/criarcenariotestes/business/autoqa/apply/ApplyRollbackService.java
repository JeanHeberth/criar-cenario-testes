package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.model.apply.AppliedFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reverte, em ordem inversa à aplicação, os arquivos já aplicados numa
 * transação que falhou: remove CREATE, restaura UPDATE a partir do backup com
 * validação de hash pós-restauração. Opera somente sobre os arquivos
 * registrados nesta execução — nunca varre o projectRoot livremente
 * (nenhum Files.walk irrestrito).
 */
@Component
public class ApplyRollbackService {

    private final ApplyPathResolver pathResolver;
    private final ApplyHashValidator hashValidator;

    public ApplyRollbackService(ApplyPathResolver pathResolver, ApplyHashValidator hashValidator) {
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
        this.hashValidator = Objects.requireNonNull(hashValidator, "hashValidator must not be null");
    }

    public RollbackOutcome rollback(List<AppliedFile> appliedSoFar, Path projectRoot, Path backupExecutionRoot) {
        Objects.requireNonNull(projectRoot, "projectRoot must not be null");
        Objects.requireNonNull(backupExecutionRoot, "backupExecutionRoot must not be null");

        List<AppliedFile> reversed = new ArrayList<>(appliedSoFar == null ? List.of() : appliedSoFar);
        Collections.reverse(reversed);

        List<String> inconsistent = new ArrayList<>();
        for (AppliedFile applied : reversed) {
            try {
                rollbackOne(applied, projectRoot, backupExecutionRoot);
            } catch (RuntimeException | IOException e) {
                inconsistent.add(applied.relativePath());
            }
        }
        return new RollbackOutcome(inconsistent.isEmpty(), inconsistent);
    }

    private void rollbackOne(AppliedFile applied, Path projectRoot, Path backupExecutionRoot) throws IOException {
        switch (applied.operation()) {
            case CREATE -> rollbackCreate(applied, projectRoot);
            case UPDATE -> rollbackUpdate(applied, projectRoot, backupExecutionRoot);
            default -> { /* REUSE/NONE nunca escreveram nada; não há o que reverter */ }
        }
    }

    private void rollbackCreate(AppliedFile applied, Path projectRoot) throws IOException {
        Path target = pathResolver.resolve(projectRoot, applied.relativePath());
        Files.deleteIfExists(target);
        if (Files.exists(target)) {
            throw new IOException("Arquivo CREATE ainda existe após tentativa de remoção: " + target);
        }
    }

    private void rollbackUpdate(AppliedFile applied, Path projectRoot, Path backupExecutionRoot) throws IOException {
        if (applied.backupRelativePath() == null) {
            throw new IOException("Backup ausente para restaurar UPDATE: " + applied.relativePath());
        }
        Path backupSource = backupExecutionRoot.resolve(applied.backupRelativePath()).normalize();
        if (!backupSource.equals(backupExecutionRoot) && !backupSource.startsWith(backupExecutionRoot)) {
            throw new IOException("Caminho de backup fora da raiz de execução: " + backupSource);
        }
        if (!Files.exists(backupSource)) {
            throw new IOException("Arquivo de backup não encontrado: " + backupSource);
        }

        Path target = pathResolver.resolve(projectRoot, applied.relativePath());
        Files.copy(backupSource, target, StandardCopyOption.REPLACE_EXISTING);

        String restoredHash = hashValidator.sha256OfFile(target);
        if (applied.sourceSha256() != null && !hashValidator.matches(applied.sourceSha256(), restoredHash)) {
            throw new IOException("Hash restaurado não confere com o original para " + applied.relativePath());
        }
    }

    public record RollbackOutcome(boolean success, List<String> inconsistentFiles) {
        public RollbackOutcome {
            inconsistentFiles = inconsistentFiles == null ? List.of() : List.copyOf(inconsistentFiles);
        }
    }
}
