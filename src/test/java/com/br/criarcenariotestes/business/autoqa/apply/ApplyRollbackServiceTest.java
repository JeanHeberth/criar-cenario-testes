package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.model.apply.AppliedFile;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyRollbackService - Testes Unitários")
class ApplyRollbackServiceTest {

    private final ApplyPathResolver pathResolver = new ApplyPathResolver();
    private final ApplyHashValidator hashValidator = new ApplyHashValidator();
    private final ApplyRollbackService service = new ApplyRollbackService(pathResolver, hashValidator);

    @Test
    @DisplayName("Deve remover arquivo CREATE aplicado e reportar sucesso")
    void deveRemoverArquivoCreateAplicado(@TempDir Path projectRoot, @TempDir Path backupRoot) throws IOException {
        Path created = projectRoot.resolve("src/Novo.java");
        Files.createDirectories(created.getParent());
        Files.writeString(created, "conteudo novo");

        AppliedFile applied = new AppliedFile("src/Novo.java", ApplyOperation.CREATE, ApplyFileStatus.APPLIED,
                null, "hash", false, null, List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(applied), projectRoot, backupRoot);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.inconsistentFiles()).isEmpty();
        assertThat(Files.exists(created)).isFalse();
    }

    @Test
    @DisplayName("Deve restaurar arquivo UPDATE a partir do backup com hash validado")
    void deveRestaurarArquivoUpdateDoBackup(@TempDir Path projectRoot, @TempDir Path backupRoot) throws IOException {
        Path target = projectRoot.resolve("src/Existente.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "conteudo modificado");

        Path backupFile = backupRoot.resolve("files/src/Existente.java");
        Files.createDirectories(backupFile.getParent());
        Files.writeString(backupFile, "conteudo original", StandardCharsets.UTF_8);
        String originalHash = hashValidator.sha256OfFile(backupFile);

        AppliedFile applied = new AppliedFile("src/Existente.java", ApplyOperation.UPDATE, ApplyFileStatus.APPLIED,
                originalHash, "hashNovo", true, "files/src/Existente.java", List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(applied), projectRoot, backupRoot);

        assertThat(outcome.success()).isTrue();
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("conteudo original");
    }

    @Test
    @DisplayName("Deve ignorar REUSE e NONE, pois nunca escreveram nada")
    void deveIgnorarReuseENone(@TempDir Path projectRoot, @TempDir Path backupRoot) {
        AppliedFile reuse = new AppliedFile("src/Reusado.java", ApplyOperation.REUSE, ApplyFileStatus.SKIPPED,
                null, null, false, null, List.of());
        AppliedFile none = new AppliedFile("src/Nenhum.java", ApplyOperation.NONE, ApplyFileStatus.SKIPPED,
                null, null, false, null, List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(reuse, none), projectRoot, backupRoot);

        assertThat(outcome.success()).isTrue();
    }

    @Test
    @DisplayName("Deve marcar como inconsistente quando o backup do UPDATE não existe")
    void deveMarcarInconsistenteQuandoBackupNaoExiste(@TempDir Path projectRoot, @TempDir Path backupRoot) throws IOException {
        Path target = projectRoot.resolve("src/Existente.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "modificado");

        AppliedFile applied = new AppliedFile("src/Existente.java", ApplyOperation.UPDATE, ApplyFileStatus.APPLIED,
                "hashAntigo", "hashNovo", true, "files/src/Existente.java", List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(applied), projectRoot, backupRoot);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.inconsistentFiles()).containsExactly("src/Existente.java");
    }

    @Test
    @DisplayName("Deve marcar como inconsistente quando hash restaurado não confere com o esperado")
    void deveMarcarInconsistenteQuandoHashNaoConfere(@TempDir Path projectRoot, @TempDir Path backupRoot) throws IOException {
        Path target = projectRoot.resolve("src/Existente.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "modificado");

        Path backupFile = backupRoot.resolve("files/src/Existente.java");
        Files.createDirectories(backupFile.getParent());
        Files.writeString(backupFile, "conteudo original", StandardCharsets.UTF_8);

        AppliedFile applied = new AppliedFile("src/Existente.java", ApplyOperation.UPDATE, ApplyFileStatus.APPLIED,
                "hash-esperado-que-nunca-vai-bater", "hashNovo", true, "files/src/Existente.java", List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(applied), projectRoot, backupRoot);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.inconsistentFiles()).containsExactly("src/Existente.java");
    }

    @Test
    @DisplayName("Deve reverter múltiplos arquivos, continuando mesmo quando um deles falha")
    void deveReverterMultiplosArquivosContinuandoAposFalha(@TempDir Path projectRoot, @TempDir Path backupRoot) throws IOException {
        Path createdOk = projectRoot.resolve("src/CreateOk.java");
        Files.createDirectories(createdOk.getParent());
        Files.writeString(createdOk, "novo");

        AppliedFile createApplied = new AppliedFile("src/CreateOk.java", ApplyOperation.CREATE, ApplyFileStatus.APPLIED,
                null, "hash", false, null, List.of());
        AppliedFile updateSemBackup = new AppliedFile("src/SemBackup.java", ApplyOperation.UPDATE, ApplyFileStatus.APPLIED,
                "hash", "hash2", true, "files/src/SemBackup.java", List.of());

        ApplyRollbackService.RollbackOutcome outcome = service.rollback(
                List.of(createApplied, updateSemBackup), projectRoot, backupRoot);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.inconsistentFiles()).containsExactly("src/SemBackup.java");
        assertThat(Files.exists(createdOk)).isFalse();
    }

    @Test
    @DisplayName("Rollback de lista vazia deve ser sucesso")
    void rollbackDeListaVaziaDeveSerSucesso(@TempDir Path projectRoot, @TempDir Path backupRoot) {
        ApplyRollbackService.RollbackOutcome outcome = service.rollback(List.of(), projectRoot, backupRoot);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.inconsistentFiles()).isEmpty();
    }
}
