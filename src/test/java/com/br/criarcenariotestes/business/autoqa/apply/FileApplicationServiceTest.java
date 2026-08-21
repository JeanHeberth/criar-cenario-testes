package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.generation.GeneratedPathResolver;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationManifest;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("FileApplicationService - Testes Unitários")
class FileApplicationServiceTest {

    private final ApplyPathResolver pathResolver = new ApplyPathResolver();
    private final GeneratedPathResolver generatedPathResolver = new GeneratedPathResolver();
    private final ApplyHashValidator hashValidator = new ApplyHashValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplyPreconditionValidator preconditionValidator = new ApplyPreconditionValidator();
    private final ApplyManifestValidator manifestValidator = new ApplyManifestValidator(objectMapper);
    private final FileBackupService backupService = new FileBackupService(pathResolver, hashValidator, objectMapper);
    private final AtomicFileApplicationService atomicService = new AtomicFileApplicationService();
    private final ApplyRollbackService rollbackService = new ApplyRollbackService(pathResolver, hashValidator);
    private final ApplySummaryBuilder summaryBuilder = new ApplySummaryBuilder();

    private FileApplicationService service;
    private Path projectRoot;
    private Path generatedBaseDir;
    private Path backupBaseDir;
    private UUID executionId;

    @BeforeEach
    void setUp(@TempDir Path root) throws IOException {
        executionId = UUID.randomUUID();
        projectRoot = Files.createDirectories(root.resolve("project"));
        generatedBaseDir = root.resolve("generated");
        backupBaseDir = root.resolve("backups");

        service = new FileApplicationService(preconditionValidator, manifestValidator, pathResolver,
                generatedPathResolver, hashValidator, backupService, atomicService, rollbackService, summaryBuilder);
        service.setGeneratedBaseDir(generatedBaseDir);
        service.setBackupBaseDir(backupBaseDir);
        manifestValidator.setGeneratedBaseDir(generatedBaseDir);
    }

    // ---- helpers ----

    private String writeGeneratedContent(String relativePath, String content) throws IOException {
        Path target = generatedBaseDir.resolve(executionId.toString()).resolve("files").resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return hashValidator.sha256(content);
    }

    private void writeManifest(List<GenerationManifest.GenerationManifestFile> files) throws IOException {
        Path executionRoot = generatedBaseDir.resolve(executionId.toString());
        Files.createDirectories(executionRoot);
        GenerationManifest manifest = new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT",
                "READY", "COMPLETED", "now", files, List.of());
        Files.writeString(executionRoot.resolve("manifest.json"), objectMapper.writeValueAsString(manifest), StandardCharsets.UTF_8);
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(projectRoot, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM, BuildTool.NPM, Set.of(), Set.of(AutomationFramework.PLAYWRIGHT), List.of(),
                null, List.of(), List.of(), DiscoveryConfidence.HIGH, true);
    }

    private TechnicalPlanResult plan() {
        return GenerationTestData.readyPlan();
    }

    private CodeReviewResult review(ReviewStatus status) {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                status, ReviewConfidence.HIGH, false, true);
    }

    private ApplyApproval approval(ApplyOperation... operations) {
        return new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(operations), true, true);
    }

    private GeneratedFile file(String path, GeneratedFileOperation op, String hash, boolean existingFile) {
        return new GeneratedFile(path, op, PlanComponentType.TEST, null, "UTF-8", hash, GeneratedFileStatus.GENERATED, existingFile, List.of(), List.of(), List.of());
    }

    private GenerationResult generation(GeneratedFile... files) {
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(files), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    // ---- CREATE ----

    @Test
    @DisplayName("Deve aplicar CREATE com sucesso, sem backup")
    void deveAplicarCreateComSucesso() throws IOException {
        String hash = writeGeneratedContent("src/Novo.spec.ts", "conteudo novo");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Novo.spec.ts", "CREATE", "TEST", "GENERATED", hash, false)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Novo.spec.ts", GeneratedFileOperation.CREATE, hash, false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.COMPLETED);
        assertThat(result.valid()).isTrue();
        assertThat(result.conflicts()).isEmpty();
        assertThat(result.backups()).isEmpty();
        assertThat(Files.readString(projectRoot.resolve("src/Novo.spec.ts"))).isEqualTo("conteudo novo");
        assertThat(result.files()).hasSize(1);
        assertThat(result.files().get(0).status()).isEqualTo(ApplyFileStatus.APPLIED);
        assertThat(result.files().get(0).backupCreated()).isFalse();
    }

    @Test
    @DisplayName("CREATE deve bloquear quando target já existe (TARGET_ALREADY_EXISTS)")
    void createDeveBloquearQuandoTargetJaExiste() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/Novo.spec.ts"), "já existe");
        String hash = writeGeneratedContent("src/Novo.spec.ts", "conteudo novo");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Novo.spec.ts", "CREATE", "TEST", "GENERATED", hash, false)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Novo.spec.ts", GeneratedFileOperation.CREATE, hash, false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.BLOCKED);
        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).type()).isEqualTo(ApplyConflict.TARGET_ALREADY_EXISTS);
        assertThat(Files.readString(projectRoot.resolve("src/Novo.spec.ts"))).isEqualTo("já existe");
        assertThat(result.backups()).isEmpty();
    }

    @Test
    @DisplayName("CREATE de CONFIGURATION já existente deve PULAR, sem bloquear o lote")
    void createDeConfiguracaoJaExistenteDevePular() throws IOException {
        // .env.example é documentação de setup: criada uma vez, mantida por quem
        // cuida do projeto. Tratá-la como conflito bloqueava o lote inteiro — os
        // testes, corretos e inéditos, não eram aplicados por causa dela.
        Files.writeString(projectRoot.resolve(".env.example"), "BASE_URL=");
        String hashConfig = writeGeneratedContent(".env.example", "BASE_URL=\nAUTH_USER=");
        Files.createDirectories(projectRoot.resolve("src"));
        String hashTeste = writeGeneratedContent("src/Novo.spec.ts", "conteudo novo");
        writeManifest(List.of(
                new GenerationManifest.GenerationManifestFile(".env.example", "CREATE", "CONFIGURATION", "GENERATED", hashConfig, false),
                new GenerationManifest.GenerationManifestFile("src/Novo.spec.ts", "CREATE", "TEST", "GENERATED", hashTeste, false)));

        GeneratedFile config = new GeneratedFile(".env.example", GeneratedFileOperation.CREATE,
                PlanComponentType.CONFIGURATION, null, "UTF-8", hashConfig, GeneratedFileStatus.GENERATED,
                false, List.of(), List.of(), List.of());

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(config, file("src/Novo.spec.ts", GeneratedFileOperation.CREATE, hashTeste, false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.conflicts()).isEmpty();
        // O teste inédito foi aplicado; a configuração existente ficou intacta.
        assertThat(Files.readString(projectRoot.resolve("src/Novo.spec.ts"))).isEqualTo("conteudo novo");
        assertThat(Files.readString(projectRoot.resolve(".env.example"))).isEqualTo("BASE_URL=");
        assertThat(result.files()).anyMatch(f -> f.relativePath().equals(".env.example")
                && f.status() == ApplyFileStatus.SKIPPED);
    }

    // ---- UPDATE ----

    @Test
    @DisplayName("Deve aplicar UPDATE com sucesso, criando backup do conteúdo original")
    void deveAplicarUpdateComSucesso() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/Existente.spec.ts"), "versão antiga", StandardCharsets.UTF_8);
        String originalHash = hashValidator.sha256("versão antiga");
        String newHash = writeGeneratedContent("src/Existente.spec.ts", "versão nova");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Existente.spec.ts", "UPDATE", "TEST", "GENERATED", newHash, true)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Existente.spec.ts", GeneratedFileOperation.UPDATE, newHash, true)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.UPDATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.COMPLETED);
        assertThat(result.valid()).isTrue();
        assertThat(Files.readString(projectRoot.resolve("src/Existente.spec.ts"))).isEqualTo("versão nova");
        assertThat(result.backups()).hasSize(1);
        assertThat(result.backups().get(0).sha256()).isEqualTo(originalHash);
        Path backupFile = backupBaseDir.resolve(executionId.toString()).resolve("files/src/Existente.spec.ts");
        assertThat(Files.readString(backupFile)).isEqualTo("versão antiga");
        assertThat(result.files().get(0).backupCreated()).isTrue();
    }

    @Test
    @DisplayName("UPDATE deve bloquear quando target não existe (TARGET_MISSING)")
    void updateDeveBloquearQuandoTargetNaoExiste() throws IOException {
        String hash = writeGeneratedContent("src/NaoExiste.spec.ts", "novo conteudo");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/NaoExiste.spec.ts", "UPDATE", "TEST", "GENERATED", hash, true)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/NaoExiste.spec.ts", GeneratedFileOperation.UPDATE, hash, true)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.UPDATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.BLOCKED);
        assertThat(result.conflicts().get(0).type()).isEqualTo(ApplyConflict.TARGET_MISSING);
        assertThat(result.backups()).isEmpty();
    }

    // ---- REUSE / NONE ----

    @Test
    @DisplayName("REUSE e NONE não devem escrever nada e devem ficar SKIPPED")
    void reuseENoneDevemFicarSkipped() throws IOException {
        writeManifest(List.of(
                new GenerationManifest.GenerationManifestFile("src/Reusado.spec.ts", "REUSE", "TEST", "SKIPPED", null, true),
                new GenerationManifest.GenerationManifestFile("src/Nenhum.spec.ts", "NONE", "TEST", "SKIPPED", null, true)
        ));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Reusado.spec.ts", GeneratedFileOperation.REUSE, null, true),
                        file("src/Nenhum.spec.ts", GeneratedFileOperation.NONE, null, true)),
                review(ReviewStatus.APPROVED), approval());

        assertThat(result.status()).isEqualTo(ApplyStatus.COMPLETED);
        assertThat(result.files()).hasSize(2);
        assertThat(result.files()).allSatisfy(f -> assertThat(f.status()).isEqualTo(ApplyFileStatus.SKIPPED));
        assertThat(result.backups()).isEmpty();
        assertThat(Files.exists(projectRoot.resolve("src/Reusado.spec.ts"))).isFalse();
    }

    // ---- manifesto ----

    @Test
    @DisplayName("Deve bloquear quando manifest.json não existe")
    void deveBloquearQuandoManifestNaoExiste() {
        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(), review(ReviewStatus.APPROVED), approval());

        assertThat(result.status()).isEqualTo(ApplyStatus.BLOCKED);
        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).type()).isEqualTo(ApplyConflict.MANIFEST_MISMATCH);
        assertThat(result.backups()).isEmpty();
        assertThat(result.files()).isEmpty();
    }

    // ---- hash do gerado ----

    @Test
    @DisplayName("Deve bloquear quando hash do conteúdo gerado diverge do declarado")
    void deveBloquearQuandoHashGeradoDiverge() throws IOException {
        writeGeneratedContent("src/Novo.spec.ts", "conteudo real diferente");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Novo.spec.ts", "CREATE", "TEST", "GENERATED", "hash-declarado-errado", false)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Novo.spec.ts", GeneratedFileOperation.CREATE, "hash-declarado-errado", false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.BLOCKED);
        assertThat(result.conflicts().get(0).type()).isEqualTo(ApplyConflict.GENERATED_HASH_MISMATCH);
    }

    // ---- transacionalidade ----

    @Test
    @DisplayName("Um conflito em um arquivo deve bloquear TODO o lote, mesmo com outro arquivo válido")
    void umConflitoDeveBloquearTodoOLote() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/JaExiste.spec.ts"), "já existe");

        String hashValido = writeGeneratedContent("src/Valido.spec.ts", "conteudo valido");
        String hashConflito = writeGeneratedContent("src/JaExiste.spec.ts", "conteudo novo");
        writeManifest(List.of(
                new GenerationManifest.GenerationManifestFile("src/Valido.spec.ts", "CREATE", "TEST", "GENERATED", hashValido, false),
                new GenerationManifest.GenerationManifestFile("src/JaExiste.spec.ts", "CREATE", "TEST", "GENERATED", hashConflito, false)
        ));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Valido.spec.ts", GeneratedFileOperation.CREATE, hashValido, false),
                        file("src/JaExiste.spec.ts", GeneratedFileOperation.CREATE, hashConflito, false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.BLOCKED);
        assertThat(Files.exists(projectRoot.resolve("src/Valido.spec.ts"))).isFalse();
        assertThat(result.backups()).isEmpty();
    }

    // ---- rollback por falha de escrita ----

    @Test
    @DisplayName("Falha de escrita deve disparar rollback e remover CREATE já aplicado no mesmo lote")
    void falhaDeEscritaDeveReverterCreateAnterior() throws IOException {
        assumeTrue(!System.getProperty("os.name").toLowerCase().contains("win"),
                "Teste de permissão POSIX não se aplica no Windows");

        String hashA = writeGeneratedContent("src/AAplicado.spec.ts", "conteudo A");
        String hashB = writeGeneratedContent("restrito/BFalha.spec.ts", "conteudo B");
        writeManifest(List.of(
                new GenerationManifest.GenerationManifestFile("src/AAplicado.spec.ts", "CREATE", "TEST", "GENERATED", hashA, false),
                new GenerationManifest.GenerationManifestFile("restrito/BFalha.spec.ts", "CREATE", "TEST", "GENERATED", hashB, false)
        ));

        Path restrictedDir = Files.createDirectories(projectRoot.resolve("restrito"));
        Files.setPosixFilePermissions(restrictedDir, PosixFilePermissions.fromString("r-xr-xr-x"));

        try {
            ApplyResult result = service.apply(executionId, discovery(), plan(),
                    generation(file("src/AAplicado.spec.ts", GeneratedFileOperation.CREATE, hashA, false),
                            file("restrito/BFalha.spec.ts", GeneratedFileOperation.CREATE, hashB, false)),
                    review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

            assertThat(result.status()).isEqualTo(ApplyStatus.ROLLED_BACK);
            assertThat(result.rollbackExecuted()).isTrue();
            assertThat(result.valid()).isTrue();
            assertThat(Files.exists(projectRoot.resolve("src/AAplicado.spec.ts"))).isFalse();
        } finally {
            Files.setPosixFilePermissions(restrictedDir, PosixFilePermissions.fromString("rwxrwxrwx"));
        }
    }

    // ---- falha de backup bloqueia tudo ----

    @Test
    @DisplayName("Falha ao criar backup deve bloquear toda a transação sem escrever nada")
    void falhaDeBackupDeveBloquearTudo() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Files.writeString(projectRoot.resolve("src/Existente.spec.ts"), "conteudo original", StandardCharsets.UTF_8);
        String newHash = writeGeneratedContent("src/Existente.spec.ts", "conteudo novo");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Existente.spec.ts", "UPDATE", "TEST", "GENERATED", newHash, true)));

        Path preexistingBackup = backupBaseDir.resolve(executionId.toString()).resolve("files/src/Existente.spec.ts");
        Files.createDirectories(preexistingBackup.getParent());
        Files.writeString(preexistingBackup, "backup pré-existente conflitante");

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Existente.spec.ts", GeneratedFileOperation.UPDATE, newHash, true)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.UPDATE));

        assertThat(result.status()).isEqualTo(ApplyStatus.FAILED);
        assertThat(result.valid()).isFalse();
        assertThat(Files.readString(projectRoot.resolve("src/Existente.spec.ts"))).isEqualTo("conteudo original");
        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("BACKUP_FAILED"));
    }

    // ---- garantias de exposição ----

    @Test
    @DisplayName("ApplyResult não deve expor o path absoluto do projeto")
    void applyResultNaoDeveExporPathAbsoluto() throws IOException {
        String hash = writeGeneratedContent("src/Novo.spec.ts", "conteudo");
        writeManifest(List.of(new GenerationManifest.GenerationManifestFile("src/Novo.spec.ts", "CREATE", "TEST", "GENERATED", hash, false)));

        ApplyResult result = service.apply(executionId, discovery(), plan(),
                generation(file("src/Novo.spec.ts", GeneratedFileOperation.CREATE, hash, false)),
                review(ReviewStatus.APPROVED), approval(ApplyOperation.CREATE));

        assertThat(result.projectRootReference()).doesNotContain(projectRoot.toAbsolutePath().toString());
    }
}
