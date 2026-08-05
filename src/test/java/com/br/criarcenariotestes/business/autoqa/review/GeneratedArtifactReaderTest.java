package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GeneratedPathResolver;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationHashService;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewReadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("GeneratedArtifactReader - Testes Unitários")
class GeneratedArtifactReaderTest {

    private GeneratedArtifactReader reader;
    private GeneratedPathResolver pathResolver;
    private GenerationHashService hashService;

    @BeforeEach
    void setUp() {
        pathResolver = new GeneratedPathResolver();
        hashService = new GenerationHashService();
        reader = new GeneratedArtifactReader(pathResolver, hashService);
    }

    @Test
    @DisplayName("Deve ler arquivo gerado (CREATE)")
    void deveLerArquivoGerado(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        String content = "import { test } from '@playwright/test';";
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", content);

        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, content));

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve usar UTF-8 na leitura")
    void deveUsarUtf8(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        String content = "descrição com acentuação: ação, não, café";
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", content);

        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, content));

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results.get(0).content()).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve validar SHA-256 do conteúdo físico")
    void deveValidarSha256(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        String content = "conteudo";
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", content);

        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, content));

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results.get(0).hashMatches()).isTrue();
        assertThat(results.get(0).actualSha256()).isEqualTo(hashService.sha256(content).hex());
    }

    @Test
    @DisplayName("Deve sinalizar hash divergente sem lançar exceção (decisão fica com o Service)")
    void deveSinalizarHashDivergenteSemLancarExcecao(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", "conteudo alterado após a geração");

        GeneratedFile declared = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo original", "UTF-8", hashService.sha256("conteudo original").hex(), null, false, List.of(), List.of(), List.of());
        GenerationResult generation = generationWith(executionId, declared);

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results.get(0).hashMatches()).isFalse();
    }

    @Test
    @DisplayName("Deve ignorar arquivo não listado como CREATE/UPDATE (REUSE)")
    void deveRejeitarArquivoNaoListado(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();

        GenerationResult generation = generationWith(executionId,
                new GeneratedFile("pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                        null, "UTF-8", null, null, true, List.of(), List.of(), List.of()));

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto")
    void deveRejeitarPathAbsoluto(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        GenerationResult generation = generationWith(executionId, file("/etc/passwd", GeneratedFileOperation.CREATE, "x"));

        assertThatThrownBy(() -> reader.readAll(executionId, generation)).isInstanceOf(CodeReviewReadException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path traversal")
    void deveRejeitarTraversal(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        GenerationResult generation = generationWith(executionId, file("../fora.ts", GeneratedFileOperation.CREATE, "x"));

        assertThatThrownBy(() -> reader.readAll(executionId, generation)).isInstanceOf(CodeReviewReadException.class);
    }

    @Test
    @DisplayName("Deve rejeitar symlink")
    void deveRejeitarSymlink(@TempDir Path tempDir) throws Exception {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        Path filesRoot = tempDir.resolve(executionId.toString()).resolve("files");
        Files.createDirectories(filesRoot);
        Path outside = Files.createDirectories(tempDir.resolve("outside"));
        Path outsideFile = Files.writeString(outside.resolve("real.ts"), "conteudo");
        try {
            Files.createSymbolicLink(filesRoot.resolve("login.spec.ts"), outsideFile);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            assumeTrue(false, "symlinks não suportados neste ambiente");
        }

        GenerationResult generation = generationWith(executionId, file("login.spec.ts", GeneratedFileOperation.CREATE, "conteudo"));

        assertThatThrownBy(() -> reader.readAll(executionId, generation)).isInstanceOf(CodeReviewReadException.class);
    }

    @Test
    @DisplayName("Não deve ler o projeto original (usa somente generatedBaseDir configurado)")
    void deveNaoLerProjetoOriginal(@TempDir Path tempDir) throws Exception {
        Path fakeProject = Files.createDirectories(tempDir.resolve("fake-project"));
        Files.writeString(fakeProject.resolve("login.spec.ts"), "codigo do projeto original");
        Path isolated = Files.createDirectories(tempDir.resolve(".auto-qa/generated"));
        reader.setGeneratedBaseDir(isolated);

        UUID executionId = UUID.randomUUID();
        writePhysicalFile(isolated, executionId, "tests/login.spec.ts", "codigo gerado");
        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, "codigo gerado"));

        List<GeneratedArtifactReader.ReadArtifact> results = reader.readAll(executionId, generation);

        assertThat(results.get(0).content()).isEqualTo("codigo gerado");
        assertThat(results.get(0).content()).doesNotContain("projeto original");
    }

    @Test
    @DisplayName("Não deve ler arquivos REUSE")
    void deveNaoLerReuse(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        GenerationResult generation = generationWith(executionId,
                new GeneratedFile("pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                        null, "UTF-8", null, null, true, List.of(), List.of(), List.of()));

        assertThat(reader.readAll(executionId, generation)).isEmpty();
    }

    @Test
    @DisplayName("Não deve ler arquivos NONE")
    void deveNaoLerNone(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        GenerationResult generation = generationWith(executionId,
                new GeneratedFile("docs/nota.md", GeneratedFileOperation.NONE, PlanComponentType.UNKNOWN,
                        null, "UTF-8", null, null, true, List.of(), List.of(), List.of()));

        assertThat(reader.readAll(executionId, generation)).isEmpty();
    }

    @Test
    @DisplayName("Deve respeitar limite de tamanho por arquivo")
    void deveRespeitarLimitePorArquivo(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        String huge = "x".repeat(GeneratedArtifactReader.MAX_CONTENT_LENGTH + 1);
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", huge);
        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, huge));

        assertThatThrownBy(() -> reader.readAll(executionId, generation)).isInstanceOf(CodeReviewReadException.class);
    }

    @Test
    @DisplayName("Deve respeitar limite total de conteúdo")
    void deveRespeitarLimiteTotal(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        String big = "x".repeat(GeneratedArtifactReader.MAX_CONTENT_LENGTH);
        writePhysicalFile(tempDir, executionId, "tests/a.spec.ts", big);
        writePhysicalFile(tempDir, executionId, "tests/b.spec.ts", big);
        writePhysicalFile(tempDir, executionId, "tests/c.spec.ts", big);
        writePhysicalFile(tempDir, executionId, "tests/d.spec.ts", big);
        writePhysicalFile(tempDir, executionId, "tests/e.spec.ts", big);
        writePhysicalFile(tempDir, executionId, "tests/f.spec.ts", big);
        GenerationResult generation = generationWith(executionId,
                file("tests/a.spec.ts", GeneratedFileOperation.CREATE, big),
                file("tests/b.spec.ts", GeneratedFileOperation.CREATE, big),
                file("tests/c.spec.ts", GeneratedFileOperation.CREATE, big),
                file("tests/d.spec.ts", GeneratedFileOperation.CREATE, big),
                file("tests/e.spec.ts", GeneratedFileOperation.CREATE, big),
                file("tests/f.spec.ts", GeneratedFileOperation.CREATE, big)
        );

        assertThatThrownBy(() -> reader.readAll(executionId, generation)).isInstanceOf(CodeReviewReadException.class);
    }

    @Test
    @DisplayName("Deve ser determinístico para as mesmas entradas")
    void deveSerDeterministico(@TempDir Path tempDir) {
        reader.setGeneratedBaseDir(tempDir);
        UUID executionId = UUID.randomUUID();
        writePhysicalFile(tempDir, executionId, "tests/login.spec.ts", "conteudo");
        GenerationResult generation = generationWith(executionId, file("tests/login.spec.ts", GeneratedFileOperation.CREATE, "conteudo"));

        List<GeneratedArtifactReader.ReadArtifact> r1 = reader.readAll(executionId, generation);
        List<GeneratedArtifactReader.ReadArtifact> r2 = reader.readAll(executionId, generation);

        assertThat(r1).isEqualTo(r2);
    }

    // --- helpers ---

    private void writePhysicalFile(Path baseDir, UUID executionId, String relativePath, String content) {
        try {
            Path target = new GeneratedPathResolver().resolve(baseDir, executionId, relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GeneratedFile file(String path, GeneratedFileOperation operation, String content) {
        return new GeneratedFile(path, operation, PlanComponentType.TEST, content, "UTF-8",
                hashService.sha256(content).hex(), null, false, List.of(), List.of(), List.of());
    }

    private GenerationResult generationWith(UUID executionId, GeneratedFile... files) {
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(files), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);
    }
}
