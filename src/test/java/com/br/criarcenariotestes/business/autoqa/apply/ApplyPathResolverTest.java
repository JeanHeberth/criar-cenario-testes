package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyConflictException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("ApplyPathResolver - Testes Unitários")
class ApplyPathResolverTest {

    private final ApplyPathResolver resolver = new ApplyPathResolver();

    @Test
    @DisplayName("Deve resolver caminho relativo válido dentro da raiz")
    void deveResolverCaminhoValido(@TempDir Path root) {
        Path resolved = resolver.resolve(root, "src/main/Foo.java");

        assertThat(resolved).isEqualTo(root.normalize().resolve("src/main/Foo.java").normalize());
    }

    @Test
    @DisplayName("Deve rejeitar relativePath nulo")
    void deveRejeitarRelativePathNulo(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, null))
                .isInstanceOf(ApplyConflictException.class)
                .satisfies(ex -> assertThat(((ApplyConflictException) ex).conflictType())
                        .isEqualTo(ApplyConflict.PATH_SECURITY_VIOLATION));
    }

    @Test
    @DisplayName("Deve rejeitar relativePath em branco")
    void deveRejeitarRelativePathEmBranco(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "   "))
                .isInstanceOf(ApplyConflictException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path traversal")
    void deveRejeitarPathTraversal(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "../fora/Foo.java"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    @DisplayName("Deve rejeitar path traversal disfarçado em segmento intermediário")
    void deveRejeitarPathTraversalDisfarcado(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "src/../../fora/Foo.java"))
                .isInstanceOf(ApplyConflictException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho absoluto Unix")
    void deveRejeitarCaminhoAbsolutoUnix(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "/etc/passwd"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining("absoluto");
    }

    @Test
    @DisplayName("Deve rejeitar caminho absoluto Windows")
    void deveRejeitarCaminhoAbsolutoWindows(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "C:\\Windows\\system32"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining("absoluto");
    }

    @Test
    @DisplayName("Deve rejeitar caminho UNC")
    void deveRejeitarCaminhoUnc(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "\\\\servidor\\share\\arquivo"))
                .isInstanceOf(ApplyConflictException.class);
    }

    @Test
    @DisplayName("Deve rejeitar file URI")
    void deveRejeitarFileUri(@TempDir Path root) {
        assertThatThrownBy(() -> resolver.resolve(root, "file:///etc/passwd"))
                .isInstanceOf(ApplyConflictException.class);
    }

    @Test
    @DisplayName("Deve rejeitar symlink em segmento ancestral existente")
    void deveRejeitarSymlinkAncestral(@TempDir Path root) throws IOException {
        Path realDir = Files.createDirectory(root.resolve("real"));
        Path linkDir;
        try {
            linkDir = Files.createSymbolicLink(root.resolve("link"), realDir);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "Symlinks não suportados neste ambiente de teste");
            return;
        }

        assertThatThrownBy(() -> resolver.resolve(root, "link/Foo.java"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining("Symlink");
    }

    @Test
    @DisplayName("Deve rejeitar quando o próprio alvo já existente é um symlink")
    void deveRejeitarQuandoAlvoESymlink(@TempDir Path root) throws IOException {
        Path realFile = Files.createFile(root.resolve("real.txt"));
        try {
            Files.createSymbolicLink(root.resolve("link.txt"), realFile);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "Symlinks não suportados neste ambiente de teste");
            return;
        }

        assertThatThrownBy(() -> resolver.resolve(root, "link.txt"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining("Symlink");
    }

    @Test
    @DisplayName("Não deve alterar nada em disco ao resolver")
    void naoDeveAlterarNadaEmDisco(@TempDir Path root) throws IOException {
        resolver.resolve(root, "novo/arquivo.txt");

        assertThat(Files.exists(root.resolve("novo"))).isFalse();
    }

    @Test
    void deveRejeitarGravacaoDentroDeDiretorioDeFerramenta(@TempDir Path root) {
        // Regressão do caso real: com tests/ vazio, o scanner "detectou" .claude
        // como padrão de diretórios e o apply gravou login.spec.ts lá dentro.
        assertThatThrownBy(() -> resolver.resolve(root, ".claude/api/auth/login.spec.ts"))
                .isInstanceOf(ApplyConflictException.class)
                .hasMessageContaining(".claude");

        assertThatThrownBy(() -> resolver.resolve(root, "node_modules/x/index.ts"))
                .isInstanceOf(ApplyConflictException.class);
    }

    @Test
    void devePermitirArquivoOcultoLegitimoNaRaiz(@TempDir Path root) {
        // A barreira vale para SEGMENTOS DE DIRETÓRIO: .env.example e .gitignore
        // são arquivos legítimos do projeto e não podem ser bloqueados.
        assertThat(resolver.resolve(root, ".env.example")).isEqualTo(root.resolve(".env.example"));
        assertThat(resolver.resolve(root, ".gitignore")).isEqualTo(root.resolve(".gitignore"));
    }
}
