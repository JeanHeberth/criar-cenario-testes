package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("GeneratedPathResolver - Testes Unitários")
class GeneratedPathResolverTest {

    private GeneratedPathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GeneratedPathResolver();
    }

    @Test
    @DisplayName("Deve resolver path isolado dentro da raiz de execução")
    void deveResolverPathIsolado(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path resolved = resolver.resolve(tempDir, executionId, "tests/login.spec.ts");

        assertThat(resolved.startsWith(tempDir.resolve(executionId.toString()).resolve("files"))).isTrue();
    }

    @Test
    @DisplayName("Deve criar path com executionId no caminho")
    void deveCriarPathComExecutionId(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path resolved = resolver.resolve(tempDir, executionId, "tests/login.spec.ts");

        assertThat(resolved.toString()).contains(executionId.toString());
    }

    @Test
    @DisplayName("Deve preservar o relativePath planejado")
    void devePreservarRelativePath(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path resolved = resolver.resolve(tempDir, executionId, "tests/login.spec.ts");

        assertThat(resolved.toString().replace('\\', '/')).endsWith("tests/login.spec.ts");
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto")
    void deveRejeitarAbsoluto(@TempDir Path tempDir) {
        assertThatThrownBy(() -> resolver.resolve(tempDir, UUID.randomUUID(), "/etc/passwd"))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path traversal")
    void deveRejeitarTraversal(@TempDir Path tempDir) {
        assertThatThrownBy(() -> resolver.resolve(tempDir, UUID.randomUUID(), "../../etc/passwd"))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Deve rejeitar tentativa de sair da raiz mesmo sem '..' explícito no final")
    void deveRejeitarSaidaDaRaiz(@TempDir Path tempDir) {
        assertThatThrownBy(() -> resolver.resolve(tempDir, UUID.randomUUID(), "tests/../../../fora.ts"))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Não deve seguir symlink em diretório ancestral")
    void deveNaoSeguirSymlink(@TempDir Path tempDir) throws Exception {
        UUID executionId = UUID.randomUUID();
        Path filesRoot = tempDir.resolve(executionId.toString()).resolve("files");
        Files.createDirectories(filesRoot);
        Path outsideTarget = Files.createDirectories(tempDir.resolve("outside"));
        Path symlink = filesRoot.resolve("linked");
        try {
            Files.createSymbolicLink(symlink, outsideTarget);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            assumeTrue(false, "symlinks não suportados neste ambiente");
        }

        assertThatThrownBy(() -> resolver.resolve(tempDir, executionId, "linked/arquivo.ts"))
                .isInstanceOf(GenerationWriteException.class);
    }

    @Test
    @DisplayName("Deve ser determinístico para as mesmas entradas")
    void deveSerDeterministico(@TempDir Path tempDir) {
        UUID executionId = UUID.randomUUID();
        Path r1 = resolver.resolve(tempDir, executionId, "tests/login.spec.ts");
        Path r2 = resolver.resolve(tempDir, executionId, "tests/login.spec.ts");

        assertThat(r1).isEqualTo(r2);
    }
}
