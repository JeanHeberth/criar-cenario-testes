package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationManifest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationManifestWriter - Testes Unitários")
class GenerationManifestWriterTest {

    private GenerationManifestWriter writer;

    @BeforeEach
    void setUp() {
        writer = new GenerationManifestWriter(new ObjectMapper());
    }

    @Test
    @DisplayName("Deve escrever manifest.json")
    void deveEscreverManifest(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(Files.exists(result)).isTrue();
        assertThat(result.getFileName().toString()).isEqualTo("manifest.json");
    }

    @Test
    @DisplayName("Não deve incluir conteúdo dos arquivos")
    void naoDeveIncluirConteudo() throws Exception {
        UUID executionId = UUID.randomUUID();
        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sampleManifest(executionId));

        assertThat(json).doesNotContain("import { test }");
    }

    @Test
    @DisplayName("Deve incluir hashes dos arquivos")
    void deveIncluirHashes(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(Files.readString(result)).contains("abc123");
    }

    @Test
    @DisplayName("Deve incluir operações dos arquivos")
    void deveIncluirOperacoes(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(Files.readString(result)).contains("CREATE");
    }

    @Test
    @DisplayName("Deve incluir status da geração")
    void deveIncluirStatus(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(Files.readString(result)).contains("COMPLETED");
    }

    @Test
    @DisplayName("Deve usar UTF-8 na escrita")
    void deveUsarUtf8(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        GenerationManifest manifest = new GenerationManifest(
                executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "2026-08-05T10:00:00",
                List.of(new GenerationManifest.GenerationManifestFile("tests/ação.spec.ts", "CREATE", "TEST", "GENERATED", "abc123", false)),
                List.of()
        );

        Path result = writer.write(tempDir, executionId, manifest);

        assertThat(Files.readString(result, java.nio.charset.StandardCharsets.UTF_8)).contains("ação");
    }

    @Test
    @DisplayName("Deve escrever dentro da raiz da execução")
    void deveEscreverDentroDaRaiz(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(result.startsWith(tempDir.resolve(executionId.toString()))).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar manifest nulo")
    void deveRejeitarPathInvalido(@TempDir Path tempDir) {
        assertThatThrownBy(() -> writer.write(tempDir, UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve gerar JSON válido e parseável")
    void deveSerJsonValido(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        GenerationManifest parsed = new ObjectMapper().readValue(result.toFile(), GenerationManifest.class);
        assertThat(parsed.executionId()).isEqualTo(executionId);
    }

    @Test
    @DisplayName("Não deve incluir projectPath")
    void deveNaoIncluirProjectPath(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path result = writer.write(tempDir, executionId, sampleManifest(executionId));

        assertThat(Files.readString(result)).doesNotContain("projectPath");
    }

    private GenerationManifest sampleManifest(UUID executionId) {
        return new GenerationManifest(
                executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "2026-08-05T10:00:00",
                List.of(new GenerationManifest.GenerationManifestFile("tests/login.spec.ts", "CREATE", "TEST", "GENERATED", "abc123", false)),
                List.of()
        );
    }
}
