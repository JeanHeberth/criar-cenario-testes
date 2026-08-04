package com.br.criarcenariotestes.business.autoqa.discovery.scanner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectScanner - Testes Unitários")
class ProjectScannerTest {

    private final ProjectScanner scanner = new ProjectScanner(new ProjectScanPolicy());

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve ignorar diretórios proibidos e arquivos sensíveis")
    void deveIgnorarDiretoriosProibidosEArquivosSensiveis() throws Exception {
        Path nodeModules = Files.createDirectories(tempDir.resolve("node_modules"));
        Files.writeString(nodeModules.resolve("package.json"), "{\"name\":\"x\"}");
        Files.writeString(tempDir.resolve(".env"), "SECRET=1");
        Files.writeString(tempDir.resolve("package.json"), "{\"dependencies\":{}}");

        ProjectScanResult result = scanner.scan(tempDir);

        assertThat(result.relativeFiles()).containsExactly("package.json");
    }

    @Test
    @DisplayName("Deve respeitar profundidade máxima")
    void deveRespeitarProfundidadeMaxima() throws Exception {
        Path depth5 = tempDir.resolve("a/b/c/d/e");
        Files.createDirectories(depth5);
        Files.writeString(depth5.resolve("package.json"), "{\"dependencies\":{}}");
        Files.writeString(tempDir.resolve("root.txt"), "ok");

        ProjectScanResult result = scanner.scan(tempDir);

        assertThat(result.relativeFiles()).contains("root.txt");
        assertThat(result.relativeFiles()).doesNotContain("a/b/c/d/e/package.json");
    }

    @Test
    @DisplayName("Deve retornar caminhos relativos")
    void deveRetornarCaminhosRelativos() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/test.robot"), "*** Test Cases ***");

        ProjectScanResult result = scanner.scan(tempDir);

        assertThat(result.relativeFiles()).containsExactly("src/test.robot");
        assertThat(result.files().getFirst().isAbsolute()).isTrue();
    }
}
