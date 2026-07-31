package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaywrightProjectLayoutService")
class PlaywrightProjectLayoutServiceTest {

    private final PlaywrightProjectLayoutService service = new PlaywrightProjectLayoutService();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("deve ler testDir do playwright.config")
    void shouldReadTestDirFromPlaywrightConfig() throws Exception {
        Files.writeString(tempDir.resolve("playwright.config.ts"),
                "export default defineConfig({ testDir: 'e2e/tests' });");

        ProjectDiscoveryResult discovery = ProjectDiscoveryResult.builder()
                .configurationFile("playwright.config.ts")
                .build();

        String testDir = service.resolvePreferredTestDirectory(tempDir, discovery, emptyAnalysis());
        assertThat(testDir).isEqualTo("e2e/tests");
    }

    @Test
    @DisplayName("deve normalizar arquivos de teste para diretório detectado")
    void shouldNormalizeTestFilesToDetectedDirectory() {
        GeneratedCodeResponse generated = new GeneratedCodeResponse(
                List.of(new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, "x", null, null)),
                List.of(), List.of(), List.of(), "ok", false, null
        );

        GeneratedCodeResponse normalized = service.normalizeGeneratedPaths(
                generated, "e2e", "pages"
        );

        assertThat(normalized.files()).hasSize(1);
        assertThat(normalized.files().get(0).relativePath()).isEqualTo("e2e/login.spec.ts");
    }

    @Test
    @DisplayName("deve normalizar page objects para diretório preferido")
    void shouldNormalizePageObjectsToPreferredDirectory() {
        GeneratedCodeResponse generated = new GeneratedCodeResponse(
                List.of(new GeneratedFile("tests/pages/login.page.ts", GeneratedFileOperation.CREATE, "x", null, null)),
                List.of(), List.of(), List.of(), "ok", false, null
        );

        GeneratedCodeResponse normalized = service.normalizeGeneratedPaths(
                generated, "e2e", "src/pages"
        );

        assertThat(normalized.files().get(0).relativePath()).isEqualTo("src/pages/login.page.ts");
    }

    @Test
    @DisplayName("deve detectar ausência de page object")
    void shouldDetectMissingPageObject() {
        GeneratedCodeResponse generated = new GeneratedCodeResponse(
                List.of(new GeneratedFile("e2e/login.spec.ts", GeneratedFileOperation.CREATE, "x", null, null)),
                List.of(), List.of(), List.of(), "ok", false, null
        );
        assertThat(service.hasPageObjectFile(generated)).isFalse();
    }

    private ProjectAnalysisResult emptyAnalysis() {
        return ProjectAnalysisResult.builder()
                .classes(List.of())
                .pageObjects(List.of())
                .testFiles(List.of())
                .fixtureFiles(List.of())
                .helperFiles(List.of())
                .customCommands(List.of())
                .describeBlocks(List.of())
                .testCases(List.of())
                .conventions(List.of())
                .gaps(List.of())
                .warnings(List.of())
                .metadata(Map.of())
                .analyzedAt(LocalDateTime.now())
                .build();
    }
}
