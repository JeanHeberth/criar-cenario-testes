package com.br.criarcenariotestes.business.autoqa.knowledge.scanner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KnowledgeScanner - Testes Unitários")
class KnowledgeScannerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve catalogar arquivos permitidos")
    void deveCatalogarArquivosPermitidos() throws Exception {
        write("src/test/Login.spec.ts", "import x from 'y';");
        write("src/main/App.java", "package a; public class App {}");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/main/App.java", "src/test/Login.spec.ts");
    }

    @Test
    @DisplayName("Deve ignorar node_modules")
    void deveIgnorarNodeModules() throws Exception {
        write("node_modules/lib/index.ts", "export const a = 1;");
        write("src/App.ts", "export const a = 1;");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/App.ts");
    }

    @Test
    @DisplayName("Deve ignorar git")
    void deveIgnorarGit() throws Exception {
        write(".git/hooks/pre-commit.js", "console.log(1);");
        write("src/App.ts", "export const a = 1;");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/App.ts");
    }

    @Test
    @DisplayName("Deve ignorar build e target")
    void deveIgnorarBuildETarget() throws Exception {
        write("build/Skip.java", "class Skip {}");
        write("target/Skip.java", "class Skip {}");
        write("src/Keep.java", "class Keep {}");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/Keep.java");
    }

    @Test
    @DisplayName("Deve ignorar env e certificados")
    void deveIgnorarEnvECertificados() throws Exception {
        write(".env", "SECRET=1");
        write(".env.local", "SECRET=2");
        write("certs/private.key", "SECRET");
        write("certs/cert.crt", "CERT");
        write("src/Keep.java", "class Keep {}");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/Keep.java");
        assertThat(result.ignoredPaths()).contains(".env", ".env.local", "certs/private.key", "certs/cert.crt");
    }

    @Test
    @DisplayName("Deve ignorar arquivos binários")
    void deveIgnorarArquivosBinarios() throws Exception {
        write("images/screenshot.png", "binary");
        write("videos/demo.mp4", "binary");
        write("src/Keep.java", "class Keep {}");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("src/Keep.java");
    }

    @Test
    @DisplayName("Deve respeitar profundidade")
    void deveRespeitarProfundidade() throws Exception {
        write("a/b/c/d/e/F.java", "class F {}");
        write("a/B.java", "class B {}");

        KnowledgeScanPolicy policy = new KnowledgeScanPolicy(2, 10, 100, 1000, defaultPolicy().getAllowedExtensions(), defaultPolicy().getIgnoredDirectories(), defaultPolicy().getIgnoredExactNames(), defaultPolicy().getIgnoredPrefixes(), defaultPolicy().getIgnoredSuffixes());
        KnowledgeScanResult result = scanner(policy).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("a/B.java");
    }

    @Test
    @DisplayName("Deve respeitar limite de arquivos")
    void deveRespeitarLimiteDeArquivos() throws Exception {
        write("a/A.java", "class A {}");
        write("b/B.java", "class B {}");

        KnowledgeScanPolicy policy = new KnowledgeScanPolicy(10, 1, 100, 1000, defaultPolicy().getAllowedExtensions(), defaultPolicy().getIgnoredDirectories(), defaultPolicy().getIgnoredExactNames(), defaultPolicy().getIgnoredPrefixes(), defaultPolicy().getIgnoredSuffixes());
        KnowledgeScanResult result = scanner(policy).scan(tempDir);

        assertThat(result.files()).hasSize(1);
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve respeitar limite por arquivo")
    void deveRespeitarLimitePorArquivo() throws Exception {
        write("big/Big.java", "class Big { " + "x".repeat(200) + " }");
        write("small/Small.java", "class Small {}");

        KnowledgeScanPolicy policy = new KnowledgeScanPolicy(10, 10, 20, 1000, defaultPolicy().getAllowedExtensions(), defaultPolicy().getIgnoredDirectories(), defaultPolicy().getIgnoredExactNames(), defaultPolicy().getIgnoredPrefixes(), defaultPolicy().getIgnoredSuffixes());
        KnowledgeScanResult result = scanner(policy).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .containsExactly("small/Small.java");
    }

    @Test
    @DisplayName("Deve respeitar limite total")
    void deveRespeitarLimiteTotal() throws Exception {
        write("a/A.java", "class A {}");
        write("b/B.java", "class B {}");

        KnowledgeScanPolicy policy = new KnowledgeScanPolicy(10, 10, 100, 15, defaultPolicy().getAllowedExtensions(), defaultPolicy().getIgnoredDirectories(), defaultPolicy().getIgnoredExactNames(), defaultPolicy().getIgnoredPrefixes(), defaultPolicy().getIgnoredSuffixes());
        KnowledgeScanResult result = scanner(policy).scan(tempDir);

        assertThat(result.files()).hasSize(1);
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve não seguir symlink")
    void deveNaoSeguirSymlink() throws Exception {
        Path external = Files.createTempFile("external", ".ts");
        Files.writeString(external, "export const secret = 1;");
        Files.createDirectories(tempDir.resolve("src"));
        Files.createSymbolicLink(tempDir.resolve("src/link.ts"), external);

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).extracting(KnowledgeScanResult.KnowledgeFile::relativePath)
                .doesNotContain("src/link.ts");
    }

    @Test
    @DisplayName("Deve retornar caminhos relativos")
    void deveRetornarCaminhosRelativos() throws Exception {
        write("src/App.java", "class App {}");

        KnowledgeScanResult result = scanner(defaultPolicy()).scan(tempDir);

        assertThat(result.files()).allSatisfy(file -> assertThat(Path.of(file.relativePath()).isAbsolute()).isFalse());
    }

    @Test
    @DisplayName("Deve registrar warning quando limite for atingido")
    void deveRegistrarWarningQuandoLimiteForAtingido() throws Exception {
        write("a/A.java", "class A {}");
        write("b/B.java", "class B {}");

        KnowledgeScanPolicy policy = new KnowledgeScanPolicy(10, 1, 100, 1000, defaultPolicy().getAllowedExtensions(), defaultPolicy().getIgnoredDirectories(), defaultPolicy().getIgnoredExactNames(), defaultPolicy().getIgnoredPrefixes(), defaultPolicy().getIgnoredSuffixes());
        KnowledgeScanResult result = scanner(policy).scan(tempDir);

        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve não modificar arquivos")
    void deveNaoModificarArquivos() throws Exception {
        Path file = write("src/App.java", "class App {}");
        FileTime before = Files.getLastModifiedTime(file);
        scanner(defaultPolicy()).scan(tempDir);
        FileTime after = Files.getLastModifiedTime(file);

        assertThat(after).isEqualTo(before);
    }

    private KnowledgeScanner scanner(KnowledgeScanPolicy policy) {
        return new KnowledgeScanner(policy);
    }

    private KnowledgeScanPolicy defaultPolicy() {
        return new KnowledgeScanPolicy();
    }

    private Path write(String relativePath, String content) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
