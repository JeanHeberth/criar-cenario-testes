package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationManifest;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("ApplyManifestValidator - Testes Unitários")
class ApplyManifestValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ApplyManifestValidator validator;
    private Path baseDir;
    private final UUID executionId = UUID.randomUUID();
    private final ProjectDiscoveryResult discovery = GenerationTestData.playwrightDiscovery();

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        baseDir = tempDir;
        validator = new ApplyManifestValidator(objectMapper);
        validator.setGeneratedBaseDir(baseDir);
    }

    private GeneratedFile file(String path, String hash) {
        return new GeneratedFile(path, GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", hash, GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
    }

    private GenerationResult generation(GeneratedFile... files) {
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(files), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    private void writeManifest(GenerationManifest manifest) throws IOException {
        Path executionRoot = baseDir.resolve(executionId.toString());
        Files.createDirectories(executionRoot);
        Files.writeString(executionRoot.resolve("manifest.json"),
                objectMapper.writeValueAsString(manifest), StandardCharsets.UTF_8);
    }

    private GenerationManifest.GenerationManifestFile manifestFile(String path, String operation, String hash) {
        return new GenerationManifest.GenerationManifestFile(path, operation, "TEST", "GENERATED", hash, false);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando manifest é consistente com a geração")
    void deveRetornarListaVaziaQuandoConsistente() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash1")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar conflito quando manifest não existe")
    void deveRetornarConflitoQuandoManifestNaoExiste() {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).type()).isEqualTo(ApplyConflict.MANIFEST_MISMATCH);
    }

    @Test
    @DisplayName("Deve retornar conflito quando manifest está corrompido")
    void deveRetornarConflitoQuandoManifestCorrompido() throws IOException {
        Path executionRoot = baseDir.resolve(executionId.toString());
        Files.createDirectories(executionRoot);
        Files.writeString(executionRoot.resolve("manifest.json"), "{ isto não é json válido", StandardCharsets.UTF_8);
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).type()).isEqualTo(ApplyConflict.MANIFEST_MISMATCH);
    }

    @Test
    @DisplayName("Deve retornar conflito quando executionId do manifest diverge")
    void deveRetornarConflitoQuandoExecutionIdDiverge() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash1")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.message()).contains("executionId"));
    }

    @Test
    @DisplayName("Deve retornar conflito quando framework do manifest diverge")
    void deveRetornarConflitoQuandoFrameworkDiverge() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(executionId, "CYPRESS", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash1")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.message()).contains("framework"));
    }

    @Test
    @DisplayName("Deve retornar conflito para arquivo extra no manifest")
    void deveRetornarConflitoParaArquivoExtraNoManifest() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash1"),
                        manifestFile("src/Extra.spec.ts", "CREATE", "hashX")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.relativePath()).isEqualTo("src/Extra.spec.ts"));
    }

    @Test
    @DisplayName("Deve retornar conflito para arquivo omitido do manifest")
    void deveRetornarConflitoParaArquivoOmitidoDoManifest() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"), file("src/Bar.spec.ts", "hash2"));
        writeManifest(new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash1")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.relativePath()).isEqualTo("src/Bar.spec.ts"));
    }

    @Test
    @DisplayName("Deve retornar conflito quando hash do manifest diverge do gerado")
    void deveRetornarConflitoQuandoHashDiverge() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "CREATE", "hash-diferente")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.message()).contains("Hash"));
    }

    @Test
    @DisplayName("Deve retornar conflito quando operação do manifest diverge da geração")
    void deveRetornarConflitoQuandoOperacaoDiverge() throws IOException {
        GenerationResult generation = generation(file("src/Foo.spec.ts", "hash1"));
        writeManifest(new GenerationManifest(executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "now",
                List.of(manifestFile("src/Foo.spec.ts", "UPDATE", "hash1")), List.of()));

        List<ApplyConflict> conflicts = validator.validate(executionId, discovery, generation);

        assertThat(conflicts).anySatisfy(c -> assertThat(c.message()).contains("Operação"));
    }
}
