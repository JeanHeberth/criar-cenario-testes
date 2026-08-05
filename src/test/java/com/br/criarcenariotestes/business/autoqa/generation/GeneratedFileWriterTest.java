package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
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

@DisplayName("GeneratedFileWriter - Testes Unitários")
class GeneratedFileWriterTest {

    private GeneratedFileWriter writer;

    @BeforeEach
    void setUp() {
        writer = new GeneratedFileWriter(new GeneratedPathResolver());
    }

    @Test
    @DisplayName("Deve escrever arquivo CREATE na área isolada")
    void deveEscreverCreate(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        Path result = writer.write(tempDir, executionId, file);

        assertThat(Files.exists(result)).isTrue();
        assertThat(Files.readString(result, StandardCharsets.UTF_8)).isEqualTo("conteudo");
    }

    @Test
    @DisplayName("Deve escrever arquivo UPDATE na área isolada (sem alterar o original)")
    void deveEscreverUpdateNaAreaIsolada(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("pages/LoginPage.ts", GeneratedFileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
                "novo conteudo", "UTF-8", null, null, true, List.of(), List.of(), List.of());

        Path result = writer.write(tempDir, executionId, file);

        assertThat(result.toString().replace('\\', '/')).contains(executionId.toString() + "/files/pages/LoginPage.ts");
        assertThat(Files.readString(result)).isEqualTo("novo conteudo");
    }

    @Test
    @DisplayName("Não deve escrever arquivo REUSE")
    void deveNaoEscreverReuse() {
        GeneratedFile file = new GeneratedFile("pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                null, "UTF-8", null, GeneratedFileStatus.SKIPPED, true, List.of(), List.of(), List.of());

        Path result = writer.write(Path.of("qualquer"), UUID.randomUUID(), file);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Não deve escrever arquivo NONE")
    void deveNaoEscreverNone() {
        GeneratedFile file = new GeneratedFile("qualquer.ts", GeneratedFileOperation.NONE, PlanComponentType.UNKNOWN,
                null, "UTF-8", null, GeneratedFileStatus.SKIPPED, true, List.of(), List.of(), List.of());

        Path result = writer.write(Path.of("qualquer"), UUID.randomUUID(), file);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve criar diretórios internos automaticamente")
    void deveCriarDiretoriosInternos(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("a/b/c/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        Path result = writer.write(tempDir, executionId, file);

        assertThat(Files.isDirectory(result.getParent())).isTrue();
    }

    @Test
    @DisplayName("Deve usar UTF-8 na escrita")
    void deveUsarUtf8(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        String conteudoComAcentos = "descrição com acentuação: ação, não, café";
        GeneratedFile file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                conteudoComAcentos, "UTF-8", null, null, false, List.of(), List.of(), List.of());

        Path result = writer.write(tempDir, executionId, file);

        assertThat(Files.readString(result, StandardCharsets.UTF_8)).isEqualTo(conteudoComAcentos);
    }

    @Test
    @DisplayName("Não deve escrever no projeto original (apenas dentro do diretório isolado informado)")
    void deveNaoEscreverNoProjetoOriginal(@TempDir Path tempDir) throws Exception {
        Path fakeProjectDir = Files.createDirectories(tempDir.resolve("fake-project"));
        Path isolatedDir = Files.createDirectories(tempDir.resolve(".auto-qa/generated"));
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        writer.write(isolatedDir, executionId, file);

        try (var stream = Files.walk(fakeProjectDir)) {
            assertThat(stream.filter(Files::isRegularFile)).isEmpty();
        }
    }

    @Test
    @DisplayName("Não deve sobrescrever arquivo já existente na área gerada")
    void deveNaoSobrescreverArquivoGerado(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        writer.write(tempDir, executionId, file);

        assertThatThrownBy(() -> writer.write(tempDir, executionId, file))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Deve falhar em path inválido (traversal)")
    void deveFalharEmPathInvalido(@TempDir Path tempDir) {
        GeneratedFile file = new GeneratedFile("../fora.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        assertThatThrownBy(() -> writer.write(tempDir, UUID.randomUUID(), file))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Não deve deixar arquivo temporário residual após escrita bem-sucedida")
    void deveLimparArquivoTemporarioEmFalha(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "conteudo", "UTF-8", null, null, false, List.of(), List.of(), List.of());

        Path result = writer.write(tempDir, executionId, file);

        try (var stream = Files.list(result.getParent())) {
            assertThat(stream.filter(p -> p.toString().endsWith(".tmp"))).isEmpty();
        }
    }
}
