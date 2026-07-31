package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalogEntry;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProjectScannerService")
class ProjectScannerServiceTest {

    @TempDir
    Path projectDir;

    private ProjectScannerService scanner;

    @BeforeEach
    void setUp() {
        AutoQaProperties props = new AutoQaProperties();
        props.setMaxFiles(500);
        props.setMaxFileSizeKb(500);
        props.setMaxTotalContentKb(5000);
        scanner = new ProjectScannerService(props);
    }

    // ─── Escaneamento básico ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Escaneamento básico")
    class BasicScan {

        @Test
        @DisplayName("deve retornar catálogo não nulo para diretório válido")
        void returnsNonNull() {
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog).isNotNull();
        }

        @Test
        @DisplayName("deve encontrar arquivo TypeScript criado na raiz")
        void findsTypeScriptFile() throws IOException {
            Files.createFile(projectDir.resolve("login.spec.ts"));
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .anyMatch(e -> e.relativePath().endsWith("login.spec.ts"));
        }

        @Test
        @DisplayName("deve carregar conteúdo do arquivo TypeScript")
        void loadsTypeScriptContent() throws IOException {
            Files.writeString(projectDir.resolve("login.spec.ts"),
                    "export class LoginPage {}");
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            ProjectCatalogEntry entry = catalog.getEntries().stream()
                    .filter(e -> e.relativePath().endsWith("login.spec.ts"))
                    .findFirst().orElseThrow();
            assertThat(entry.contentLoaded()).isTrue();
            assertThat(entry.content()).contains("LoginPage");
        }

        @Test
        @DisplayName("deve retornar projectRoot correto")
        void hasCorrectProjectRoot() {
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getProjectRoot()).isEqualTo(projectDir);
        }

        @Test
        @DisplayName("deve preencher scannedAt")
        void hasScannedAt() {
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getScannedAt()).isNotNull();
        }
    }

    // ─── Exclusão de diretórios ignorados ────────────────────────────────────

    @Nested
    @DisplayName("Exclusão de diretórios ignorados")
    class IgnoredDirectories {

        @Test
        @DisplayName("deve excluir node_modules")
        void excludesNodeModules() throws IOException {
            Path nm = Files.createDirectory(projectDir.resolve("node_modules"));
            Files.writeString(nm.resolve("lib.ts"), "// library");
            Files.createFile(projectDir.resolve("playwright.config.ts"));

            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().contains("node_modules"));
        }

        @Test
        @DisplayName("deve excluir .git")
        void excludesGit() throws IOException {
            Path git = Files.createDirectory(projectDir.resolve(".git"));
            Files.writeString(git.resolve("config"), "[core]");
            Files.createFile(projectDir.resolve("tsconfig.json"));

            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().contains(".git"));
        }

        @Test
        @DisplayName("deve excluir playwright-report")
        void excludesPlaywrightReport() throws IOException {
            Path report = Files.createDirectory(projectDir.resolve("playwright-report"));
            Files.writeString(report.resolve("index.html"), "<html></html>");

            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().contains("playwright-report"));
        }

        @Test
        @DisplayName("deve excluir diretório adicional informado via parâmetro")
        void excludesAdditionalIgnoredDir() throws IOException {
            Path custom = Files.createDirectory(projectDir.resolve("allure-report"));
            Files.writeString(custom.resolve("data.json"), "{}");

            ProjectCatalog catalog = scanner.scan(projectDir, List.of("allure-report"));
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().contains("allure-report"));
        }
    }

    // ─── Exclusão de arquivos sensíveis ──────────────────────────────────────

    @Nested
    @DisplayName("Exclusão de arquivos sensíveis")
    class SensitiveFiles {

        @Test
        @DisplayName("deve excluir .env da raiz")
        void excludesEnvFile() throws IOException {
            Files.writeString(projectDir.resolve(".env"), "API_KEY=secret");
            Files.createFile(projectDir.resolve("playwright.config.ts"));

            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.fileName().equals(".env"));
        }

        @Test
        @DisplayName("deve excluir .env.local")
        void excludesEnvLocal() throws IOException {
            Files.writeString(projectDir.resolve(".env.local"), "TOKEN=abc");
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.fileName().startsWith(".env"));
        }

        @Test
        @DisplayName("deve excluir chave privada .pem")
        void excludesPem() throws IOException {
            Files.writeString(projectDir.resolve("cert.pem"), "-----BEGIN CERTIFICATE-----");
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().endsWith(".pem"));
        }
    }

    // ─── Exclusão de arquivos binários ───────────────────────────────────────

    @Nested
    @DisplayName("Exclusão de binários")
    class BinaryFiles {

        @Test
        @DisplayName("deve excluir .png")
        void excludesPng() throws IOException {
            Files.write(projectDir.resolve("logo.png"), new byte[]{(byte) 0x89, 0x50});
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().endsWith(".png"));
        }

        @Test
        @DisplayName("deve excluir .zip")
        void excludesZip() throws IOException {
            Files.write(projectDir.resolve("archive.zip"), new byte[]{0x50, 0x4B});
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .noneMatch(e -> e.relativePath().endsWith(".zip"));
        }
    }

    // ─── Limites ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Limites de escaneamento")
    class Limits {

        @Test
        @DisplayName("deve respeitar limite de arquivos e adicionar aviso")
        void respectsMaxFilesLimit() throws IOException {
            AutoQaProperties limitedProps = new AutoQaProperties();
            limitedProps.setMaxFiles(3);
            limitedProps.setMaxFileSizeKb(500);
            limitedProps.setMaxTotalContentKb(5000);
            ProjectScannerService limitedScanner = new ProjectScannerService(limitedProps);

            for (int i = 0; i < 10; i++) {
                Files.createFile(projectDir.resolve("file" + i + ".ts"));
            }
            ProjectCatalog catalog = limitedScanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries().size()).isLessThanOrEqualTo(3);
            assertThat(catalog.getWarnings()).anyMatch(w -> w.toLowerCase().contains("limite"));
        }

        @Test
        @DisplayName("deve pular arquivo grande e adicionar aviso")
        void skipsLargeFile() throws IOException {
            AutoQaProperties limitedProps = new AutoQaProperties();
            limitedProps.setMaxFiles(500);
            limitedProps.setMaxFileSizeKb(1);
            limitedProps.setMaxTotalContentKb(5000);
            ProjectScannerService limitedScanner = new ProjectScannerService(limitedProps);

            // Cria arquivo de 2 KB (maior que limite de 1 KB)
            Files.writeString(projectDir.resolve("big.ts"), "x".repeat(2048));
            Files.createFile(projectDir.resolve("small.ts"));

            ProjectCatalog catalog = limitedScanner.scan(projectDir, List.of());
            ProjectCatalogEntry big = catalog.getEntries().stream()
                    .filter(e -> e.relativePath().endsWith("big.ts"))
                    .findFirst().orElse(null);

            if (big != null) {
                assertThat(big.contentLoaded()).isFalse();
            }
            assertThat(catalog.getWarnings()).anyMatch(w -> w.contains("big.ts") || w.contains("tamanho"));
        }
    }

    // ─── Priorização de arquivos ──────────────────────────────────────────────

    @Nested
    @DisplayName("Priorização de arquivos")
    class Prioritization {

        @Test
        @DisplayName("package.json deve ser marcado como prioritário")
        void packageJsonIsPrioritized() throws IOException {
            Files.writeString(projectDir.resolve("package.json"), "{}");
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .filteredOn(e -> e.fileName().equals("package.json"))
                    .allMatch(ProjectCatalogEntry::prioritized);
        }

        @Test
        @DisplayName("playwright.config.ts deve ser marcado como prioritário")
        void playwrightConfigIsPrioritized() throws IOException {
            Files.createFile(projectDir.resolve("playwright.config.ts"));
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .filteredOn(e -> e.fileName().equals("playwright.config.ts"))
                    .allMatch(ProjectCatalogEntry::prioritized);
        }

        @Test
        @DisplayName("tsconfig.json deve ser marcado como prioritário")
        void tsconfigIsPrioritized() throws IOException {
            Files.createFile(projectDir.resolve("tsconfig.json"));
            ProjectCatalog catalog = scanner.scan(projectDir, List.of());
            assertThat(catalog.getEntries())
                    .filteredOn(e -> e.fileName().equals("tsconfig.json"))
                    .allMatch(ProjectCatalogEntry::prioritized);
        }
    }

    // ─── Caminhos relativos ───────────────────────────────────────────────────

    @Test
    @DisplayName("relativePath deve ser relativo à raiz do projeto, nunca absoluto")
    void usesRelativePaths() throws IOException {
        Files.createFile(projectDir.resolve("login.spec.ts"));
        ProjectCatalog catalog = scanner.scan(projectDir, List.of());
        catalog.getEntries().forEach(e ->
                assertThat(e.relativePath()).doesNotStartWith("/").doesNotStartWith("C:\\")
        );
    }

    // ─── Subdiretórios ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve escanear subdiretórios")
    void scansSubDirectories() throws IOException {
        Path testsDir = Files.createDirectory(projectDir.resolve("tests"));
        Files.writeString(testsDir.resolve("login.spec.ts"), "test('login', () => {});");
        ProjectCatalog catalog = scanner.scan(projectDir, List.of());
        assertThat(catalog.getEntries())
                .anyMatch(e -> e.relativePath().contains("login.spec.ts"));
    }
}
