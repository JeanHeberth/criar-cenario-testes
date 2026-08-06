package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyBackupException;
import com.br.criarcenariotestes.business.autoqa.model.apply.BackupRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileBackupService - Testes Unitários")
class FileBackupServiceTest {

    private final ApplyPathResolver pathResolver = new ApplyPathResolver();
    private final ApplyHashValidator hashValidator = new ApplyHashValidator();
    private final FileBackupService service = new FileBackupService(pathResolver, hashValidator, new ObjectMapper());

    @Test
    @DisplayName("Deve criar backup preservando bytes exatos e sha256 correto")
    void deveCriarBackupPreservandoBytes(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");
        Path source = tempDir.resolve("original.txt");
        Files.writeString(source, "conteúdo original", StandardCharsets.UTF_8);
        UUID executionId = UUID.randomUUID();

        BackupRecord record = service.backup(backupBaseDir, executionId, "src/Original.txt", source);

        Path backupFile = backupBaseDir.resolve(executionId.toString()).resolve("files/src/Original.txt");
        assertThat(Files.readString(backupFile, StandardCharsets.UTF_8)).isEqualTo("conteúdo original");
        assertThat(record.relativePath()).isEqualTo("src/Original.txt");
        assertThat(record.backupRelativePath()).isEqualTo("files/src/Original.txt");
        assertThat(record.sha256()).isEqualTo(hashValidator.sha256OfFile(backupFile));
        assertThat(record.backedUpAt()).isNotNull();
    }

    @Test
    @DisplayName("Não deve alterar o arquivo original ao criar backup")
    void naoDeveAlterarOriginal(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");
        Path source = tempDir.resolve("original.txt");
        Files.writeString(source, "conteúdo original", StandardCharsets.UTF_8);
        String hashAntes = hashValidator.sha256OfFile(source);

        service.backup(backupBaseDir, UUID.randomUUID(), "src/Original.txt", source);

        assertThat(hashValidator.sha256OfFile(source)).isEqualTo(hashAntes);
    }

    @Test
    @DisplayName("Deve lançar ApplyBackupException quando backup já existe para o mesmo relativePath")
    void deveLancarExceptionQuandoBackupJaExiste(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");
        Path source = tempDir.resolve("original.txt");
        Files.writeString(source, "v1", StandardCharsets.UTF_8);
        UUID executionId = UUID.randomUUID();
        service.backup(backupBaseDir, executionId, "src/Original.txt", source);

        assertThatThrownBy(() -> service.backup(backupBaseDir, executionId, "src/Original.txt", source))
                .isInstanceOf(ApplyBackupException.class);
    }

    @Test
    @DisplayName("Deve lançar ApplyBackupException quando sourceFile não existe")
    void deveLancarExceptionQuandoSourceNaoExiste(@TempDir Path tempDir) {
        Path backupBaseDir = tempDir.resolve("backups");
        Path missing = tempDir.resolve("nao-existe.txt");

        assertThatThrownBy(() -> service.backup(backupBaseDir, UUID.randomUUID(), "src/Original.txt", missing))
                .isInstanceOf(ApplyBackupException.class);
    }

    @Test
    @DisplayName("Deve lançar ApplyBackupException para relativePath inseguro")
    void deveLancarExceptionParaPathInseguro(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");
        Path source = tempDir.resolve("original.txt");
        Files.writeString(source, "v1", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.backup(backupBaseDir, UUID.randomUUID(), "../fora.txt", source))
                .isInstanceOf(ApplyBackupException.class);
    }

    @Test
    @DisplayName("writeManifest deve escrever JSON legível com os registros de backup")
    void deveEscreverManifestLegivel(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");
        UUID executionId = UUID.randomUUID();
        BackupRecord record = new BackupRecord("src/Foo.java", "files/src/Foo.java", "hash123",
                java.time.LocalDateTime.now());

        Path manifestPath = service.writeManifest(backupBaseDir, executionId, List.of(record));

        assertThat(Files.exists(manifestPath)).isTrue();
        FileBackupService.BackupManifest manifest = new ObjectMapper()
                .readValue(manifestPath.toFile(), FileBackupService.BackupManifest.class);
        assertThat(manifest.executionId()).isEqualTo(executionId);
        assertThat(manifest.backups()).hasSize(1);
        assertThat(manifest.backups().get(0).relativePath()).isEqualTo("src/Foo.java");
        assertThat(manifest.backups().get(0).sha256()).isEqualTo("hash123");
    }

    @Test
    @DisplayName("writeManifest deve tratar lista nula como vazia")
    void deveTratarListaNulaComoVazia(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve("backups");

        Path manifestPath = service.writeManifest(backupBaseDir, UUID.randomUUID(), null);

        FileBackupService.BackupManifest manifest = new ObjectMapper()
                .readValue(manifestPath.toFile(), FileBackupService.BackupManifest.class);
        assertThat(manifest.backups()).isEmpty();
    }

    @Test
    @DisplayName("Backup deve ficar somente dentro de .auto-qa/backups/<executionId>/")
    void backupDeveFicarSomenteNaRaizDaExecucao(@TempDir Path tempDir) throws IOException {
        Path backupBaseDir = tempDir.resolve(".auto-qa").resolve("backups");
        Path source = tempDir.resolve("original.txt");
        Files.writeString(source, "v1", StandardCharsets.UTF_8);
        UUID executionId = UUID.randomUUID();

        BackupRecord record = service.backup(backupBaseDir, executionId, "src/Foo.java", source);

        Path expectedRoot = backupBaseDir.resolve(executionId.toString());
        Path backupFile = backupBaseDir.resolve(executionId.toString()).resolve(record.backupRelativePath());
        assertThat(backupFile.normalize().startsWith(expectedRoot.normalize())).isTrue();
    }
}
