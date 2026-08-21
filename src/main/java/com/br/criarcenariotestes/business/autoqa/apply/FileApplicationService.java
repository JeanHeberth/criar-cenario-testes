package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyBackupException;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyConflictException;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyIoException;
import com.br.criarcenariotestes.business.autoqa.generation.GeneratedPathResolver;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.apply.AppliedFile;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyWarning;
import com.br.criarcenariotestes.business.autoqa.model.apply.BackupRecord;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquestrador stateless da Fase 8. Aplica no projeto real somente arquivos
 * já planejados, gerados e revisados. Não executa IA, comandos externos ou
 * testes do projeto analisado. A aplicação é transacional: todos os
 * conflitos são detectados antes de qualquer backup, todos os backups
 * acontecem antes de qualquer escrita, e uma falha em qualquer escrita
 * dispara rollback do que já foi aplicado nesta chamada.
 */
@Service
public class FileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(FileApplicationService.class);

    private final ApplyPreconditionValidator preconditionValidator;
    private final ApplyManifestValidator manifestValidator;
    private final ApplyPathResolver pathResolver;
    private final GeneratedPathResolver generatedPathResolver;
    private final ApplyHashValidator hashValidator;
    private final FileBackupService backupService;
    private final AtomicFileApplicationService atomicService;
    private final ApplyRollbackService rollbackService;
    private final ApplySummaryBuilder summaryBuilder;

    private Path generatedBaseDir = Path.of(".auto-qa", "generated");
    private Path backupBaseDir = Path.of(".auto-qa", "backups");

    public FileApplicationService(ApplyPreconditionValidator preconditionValidator,
                                   ApplyManifestValidator manifestValidator,
                                   ApplyPathResolver pathResolver,
                                   GeneratedPathResolver generatedPathResolver,
                                   ApplyHashValidator hashValidator,
                                   FileBackupService backupService,
                                   AtomicFileApplicationService atomicService,
                                   ApplyRollbackService rollbackService,
                                   ApplySummaryBuilder summaryBuilder) {
        this.preconditionValidator = Objects.requireNonNull(preconditionValidator);
        this.manifestValidator = Objects.requireNonNull(manifestValidator);
        this.pathResolver = Objects.requireNonNull(pathResolver);
        this.generatedPathResolver = Objects.requireNonNull(generatedPathResolver);
        this.hashValidator = Objects.requireNonNull(hashValidator);
        this.backupService = Objects.requireNonNull(backupService);
        this.atomicService = Objects.requireNonNull(atomicService);
        this.rollbackService = Objects.requireNonNull(rollbackService);
        this.summaryBuilder = Objects.requireNonNull(summaryBuilder);
    }

    /** Visível apenas para testes: permite isolar a leitura da área gerada em diretório temporário. */
    void setGeneratedBaseDir(Path generatedBaseDir) {
        this.generatedBaseDir = Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
    }

    /** Visível apenas para testes: permite isolar backups em diretório temporário. */
    void setBackupBaseDir(Path backupBaseDir) {
        this.backupBaseDir = Objects.requireNonNull(backupBaseDir, "backupBaseDir must not be null");
    }

    public ApplyResult apply(UUID executionId,
                              ProjectDiscoveryResult discovery,
                              TechnicalPlanResult plan,
                              GenerationResult generation,
                              CodeReviewResult review,
                              ApplyApproval approval) {
        if (executionId == null) throw new IllegalArgumentException("executionId must not be null");
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        if (generation == null) throw new IllegalArgumentException("generation must not be null");
        if (review == null) throw new IllegalArgumentException("review must not be null");

        preconditionValidator.validate(plan, generation, review, approval);

        log.info("Apply started. executionId={}", executionId);

        Path projectRoot = discovery.getNormalizedProjectPath().normalize();
        Path backupExecutionRoot = backupBaseDir.resolve(executionId.toString()).normalize();

        List<ApplyConflict> manifestConflicts = manifestValidator.validate(executionId, discovery, generation);
        if (!manifestConflicts.isEmpty()) {
            log.warn("Apply blocked by manifest conflicts. executionId={}, conflicts={}", executionId, manifestConflicts.size());
            return summaryBuilder.build(executionId, List.of(), List.of(), manifestConflicts, List.of(),
                    projectRoot, backupExecutionRoot, ApplyStatus.BLOCKED, false, true);
        }

        List<AppliedFile> skippedFiles = new ArrayList<>();
        List<ResolvedApply> toApply = new ArrayList<>();
        List<ApplyConflict> conflicts = new ArrayList<>();

        for (GeneratedFile generatedFile : generation.files()) {
            if (generatedFile == null) continue;
            detectOne(executionId, projectRoot, generatedFile, skippedFiles, toApply, conflicts);
        }

        if (!conflicts.isEmpty()) {
            log.warn("Apply blocked by conflicts. executionId={}, conflicts={}", executionId, conflicts.size());
            return summaryBuilder.build(executionId, skippedFiles, List.of(), conflicts, List.of(),
                    projectRoot, backupExecutionRoot, ApplyStatus.BLOCKED, false, true);
        }

        toApply.sort(Comparator.comparing(r -> r.relativePath()));

        List<BackupRecord> backups = new ArrayList<>();
        Map<String, BackupRecord> backupByPath = new HashMap<>();
        try {
            for (ResolvedApply resolved : toApply) {
                if (resolved.operation() == ApplyOperation.UPDATE) {
                    BackupRecord record = backupService.backup(backupBaseDir, executionId, resolved.relativePath(), resolved.target());
                    backups.add(record);
                    backupByPath.put(resolved.relativePath(), record);
                }
            }
            if (!backups.isEmpty()) {
                backupService.writeManifest(backupBaseDir, executionId, backups);
            }
        } catch (ApplyBackupException e) {
            log.error("Apply failed during backup. executionId={}, reason={}", executionId, e.getMessage());
            ApplyWarning warning = new ApplyWarning("BACKUP_FAILED", e.getMessage(), "CRITICAL", true);
            return summaryBuilder.build(executionId, skippedFiles, backups, List.of(), List.of(warning),
                    projectRoot, backupExecutionRoot, ApplyStatus.FAILED, false, false);
        }

        List<AppliedFile> appliedFiles = new ArrayList<>();
        for (ResolvedApply resolved : toApply) {
            try {
                applyOne(resolved, backupByPath, appliedFiles);
            } catch (ApplyIoException writeFailure) {
                log.error("Apply write failed, rolling back. executionId={}, relativePath={}, reason={}",
                        executionId, resolved.relativePath(), writeFailure.getMessage());
                return handleWriteFailure(executionId, discovery, projectRoot, backupExecutionRoot,
                        skippedFiles, backups, appliedFiles, resolved, writeFailure);
            }
        }

        List<AppliedFile> finalFiles = new ArrayList<>(skippedFiles);
        finalFiles.addAll(appliedFiles);

        boolean hasWarnings = !generation.warnings().isEmpty() || review.status() == ReviewStatus.APPROVED_WITH_WARNINGS;
        ApplyStatus status = hasWarnings ? ApplyStatus.COMPLETED_WITH_WARNINGS : ApplyStatus.COMPLETED;

        log.info("Apply finished. executionId={}, status={}, applied={}", executionId, status, appliedFiles.size());
        return summaryBuilder.build(executionId, finalFiles, backups, List.of(), List.of(),
                projectRoot, backupExecutionRoot, status, false, true);
    }

    private void detectOne(UUID executionId, Path projectRoot, GeneratedFile generatedFile,
                            List<AppliedFile> skippedFiles, List<ResolvedApply> toApply, List<ApplyConflict> conflicts) {
        ApplyOperation operation = ApplyOperation.from(generatedFile.operation());

        if (operation == ApplyOperation.REUSE || operation == ApplyOperation.NONE) {
            skippedFiles.add(new AppliedFile(generatedFile.relativePath(), operation, ApplyFileStatus.SKIPPED,
                    null, null, false, null, List.of()));
            return;
        }

        String content;
        try {
            Path generatedPath = generatedPathResolver.resolve(generatedBaseDir, executionId, generatedFile.relativePath());
            if (!Files.exists(generatedPath) || !Files.isRegularFile(generatedPath)) {
                conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.GENERATED_HASH_MISMATCH,
                        "Arquivo gerado não encontrado para verificação de hash: " + generatedFile.relativePath()));
                return;
            }
            content = Files.readString(generatedPath, StandardCharsets.UTF_8);
        } catch (GenerationWriteException e) {
            conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.PATH_SECURITY_VIOLATION, e.getMessage()));
            return;
        } catch (IOException e) {
            conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.GENERATED_HASH_MISMATCH,
                    "Falha ao ler conteúdo gerado: " + e.getMessage()));
            return;
        }

        String actualHash = hashValidator.sha256(content);
        if (!hashValidator.matches(generatedFile.sha256(), actualHash)) {
            conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.GENERATED_HASH_MISMATCH,
                    "Hash do conteúdo gerado não confere com GenerationResult para " + generatedFile.relativePath()));
            return;
        }

        Path target;
        try {
            target = pathResolver.resolve(projectRoot, generatedFile.relativePath());
        } catch (ApplyConflictException e) {
            conflicts.add(new ApplyConflict(generatedFile.relativePath(), e.conflictType(), e.getMessage()));
            return;
        }

        if (operation == ApplyOperation.CREATE) {
            if (Files.exists(target)) {
                // Template de configuração que já existe é PULADO, não conflito.
                // .env.example é documentação de setup: criada uma vez e mantida por
                // quem cuida do projeto. Tratá-la como conflito bloqueava o LOTE
                // INTEIRO — os testes, corretos e inéditos, deixavam de ser aplicados
                // por causa de um arquivo acessório que já estava lá.
                // Só vale para CONFIGURATION: sobrescrever teste em silêncio continua
                // proibido, e por isso segue como conflito.
                if (generatedFile.componentType() == PlanComponentType.CONFIGURATION) {
                    log.info("Apply pulou configuração já existente. executionId={}, arquivo={}",
                            executionId, generatedFile.relativePath());
                    skippedFiles.add(new AppliedFile(generatedFile.relativePath(), operation, ApplyFileStatus.SKIPPED,
                            null, null, false, null, List.of()));
                    return;
                }
                conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.TARGET_ALREADY_EXISTS,
                        "Arquivo alvo já existe para CREATE: " + generatedFile.relativePath()));
                return;
            }
            toApply.add(new ResolvedApply(generatedFile.relativePath(), ApplyOperation.CREATE, target, content, null, actualHash));
        } else {
            if (!Files.exists(target)) {
                conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.TARGET_MISSING,
                        "Arquivo alvo não existe para UPDATE: " + generatedFile.relativePath()));
                return;
            }
            String currentHash;
            try {
                currentHash = hashValidator.sha256OfFile(target);
            } catch (ApplyIoException e) {
                conflicts.add(new ApplyConflict(generatedFile.relativePath(), ApplyConflict.ORIGINAL_FILE_CHANGED,
                        "Não foi possível ler o arquivo original para verificação de hash: " + e.getMessage()));
                return;
            }
            toApply.add(new ResolvedApply(generatedFile.relativePath(), ApplyOperation.UPDATE, target, content, currentHash, actualHash));
        }
    }

    private void applyOne(ResolvedApply resolved, Map<String, BackupRecord> backupByPath, List<AppliedFile> appliedFiles) {
        if (resolved.operation() == ApplyOperation.UPDATE) {
            String hashImediatamenteAntes = hashValidator.sha256OfFile(resolved.target());
            if (!hashValidator.matches(resolved.sourceSha256(), hashImediatamenteAntes)) {
                throw new ApplyIoException("Hash do arquivo original mudou entre a detecção e a escrita: " + resolved.relativePath());
            }
            atomicService.writeUpdate(resolved.target(), resolved.content());
        } else {
            atomicService.writeCreate(resolved.target(), resolved.content());
        }

        String finalHash = hashValidator.sha256OfFile(resolved.target());
        if (!hashValidator.matches(resolved.expectedSha256(), finalHash)) {
            throw new ApplyIoException("Hash final não confere após a escrita: " + resolved.relativePath());
        }

        BackupRecord backupRecord = backupByPath.get(resolved.relativePath());
        appliedFiles.add(new AppliedFile(resolved.relativePath(), resolved.operation(), ApplyFileStatus.APPLIED,
                resolved.sourceSha256(), finalHash, backupRecord != null,
                backupRecord != null ? backupRecord.backupRelativePath() : null, List.of()));
    }

    private ApplyResult handleWriteFailure(UUID executionId, ProjectDiscoveryResult discovery, Path projectRoot,
                                            Path backupExecutionRoot, List<AppliedFile> skippedFiles,
                                            List<BackupRecord> backups, List<AppliedFile> appliedFiles,
                                            ResolvedApply failedResolved, ApplyIoException writeFailure) {
        ApplyRollbackService.RollbackOutcome outcome = rollbackService.rollback(appliedFiles, projectRoot, backupExecutionRoot);

        List<AppliedFile> finalFiles = new ArrayList<>(skippedFiles);
        for (AppliedFile applied : appliedFiles) {
            boolean inconsistent = outcome.inconsistentFiles().contains(applied.relativePath());
            finalFiles.add(new AppliedFile(applied.relativePath(), applied.operation(),
                    inconsistent ? ApplyFileStatus.FAILED : ApplyFileStatus.ROLLED_BACK,
                    applied.sourceSha256(), applied.appliedSha256(), applied.backupCreated(),
                    applied.backupRelativePath(), List.of()));
        }
        BackupRecord failedBackup = backups.stream()
                .filter(b -> b.relativePath().equals(failedResolved.relativePath()))
                .findFirst().orElse(null);
        finalFiles.add(new AppliedFile(failedResolved.relativePath(), failedResolved.operation(), ApplyFileStatus.FAILED,
                failedResolved.sourceSha256(), null, failedBackup != null,
                failedBackup != null ? failedBackup.backupRelativePath() : null,
                List.of(new ApplyWarning("WRITE_FAILED", writeFailure.getMessage(), "CRITICAL", true))));

        List<ApplyWarning> warnings = new ArrayList<>();
        ApplyStatus status;
        boolean valid;
        if (outcome.success()) {
            status = ApplyStatus.ROLLED_BACK;
            valid = true;
        } else {
            status = ApplyStatus.FAILED;
            valid = false;
            warnings.add(new ApplyWarning("ROLLBACK_INCONSISTENT_STATE",
                    "Arquivos em estado inconsistente após rollback: " + outcome.inconsistentFiles(), "CRITICAL", true));
        }

        log.error("Apply rollback finished. executionId={}, rollbackSuccess={}, status={}",
                executionId, outcome.success(), status);

        return summaryBuilder.build(executionId, finalFiles, backups, List.of(), warnings,
                projectRoot, backupExecutionRoot, status, true, valid);
    }

    private record ResolvedApply(String relativePath, ApplyOperation operation, Path target, String content,
                                  String sourceSha256, String expectedSha256) {
    }
}
