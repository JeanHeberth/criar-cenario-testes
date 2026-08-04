package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.framework.cypress.CypressAdapter;
import com.br.criarcenariotestes.business.autoqa.framework.playwright.PlaywrightAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
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

@DisplayName("ProjectDiscoveryAgent")
class ProjectDiscoveryAgentTest {

    @TempDir
    Path tempDir;

    private ProjectDiscoveryAgent agent;

    @BeforeEach
    void setUp() {
        AutomationFrameworkResolver resolver = new AutomationFrameworkResolver(
                List.of(new PlaywrightAdapter(), new CypressAdapter())
        );
        agent = new ProjectDiscoveryAgent(resolver);
    }

    // ─── Detecção de Playwright ───────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de Playwright")
    class PlaywrightDetection {

        @Test
        @DisplayName("deve detectar Playwright por playwright.config.ts")
        void detectByConfigTs() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }

        @Test
        @DisplayName("deve detectar Playwright por playwright.config.js")
        void detectByConfigJs() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.js"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }

        @Test
        @DisplayName("deve detectar Playwright por @playwright/test no package.json")
        void detectByPackageJson() throws IOException {
            Files.writeString(tempDir.resolve("package.json"),
                    "{\"devDependencies\": {\"@playwright/test\": \"^1.45.0\"}}");
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }

        @Test
        @DisplayName("deve registrar evidência de detecção")
        void registersEvidence() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectionEvidences()).anyMatch(e ->
                    e.contains("playwright.config.ts"));
        }
    }

    // ─── Detecção de Cypress ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de Cypress")
    class CypressDetection {

        @Test
        @DisplayName("deve detectar Cypress por cypress.config.ts")
        void detectByConfigTs() throws IOException {
            Files.createFile(tempDir.resolve("cypress.config.ts"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }

        @Test
        @DisplayName("deve detectar Cypress por cypress.config.js")
        void detectByConfigJs() throws IOException {
            Files.createFile(tempDir.resolve("cypress.config.js"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }

        @Test
        @DisplayName("deve detectar Cypress por dependência no package.json")
        void detectByPackageJson() throws IOException {
            Files.writeString(tempDir.resolve("package.json"),
                    "{\"devDependencies\": {\"cypress\": \"^13.0.0\"}}");
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }
    }

    // ─── Detecção de TypeScript ───────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de TypeScript")
    class TypeScriptDetection {

        @Test
        @DisplayName("deve detectar TypeScript por tsconfig.json")
        void detectByTsConfig() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            Files.createFile(tempDir.resolve("tsconfig.json"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        }

        @Test
        @DisplayName("deve detectar TypeScript por arquivo .ts")
        void detectByTsFile() throws IOException {
            Files.createFile(tempDir.resolve("login.spec.ts"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getDetectedLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        }
    }

    // ─── Detecção de gerenciador de pacotes ───────────────────────────────────

    @Nested
    @DisplayName("Detecção de package manager")
    class PackageManagerDetection {

        @Test
        @DisplayName("deve detectar NPM por package-lock.json")
        void detectNpm() throws IOException {
            Files.createFile(tempDir.resolve("package-lock.json"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getPackageManager()).isEqualTo(PackageManager.NPM);
        }

        @Test
        @DisplayName("deve detectar YARN por yarn.lock")
        void detectYarn() throws IOException {
            Files.createFile(tempDir.resolve("yarn.lock"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getPackageManager()).isEqualTo(PackageManager.YARN);
        }

        @Test
        @DisplayName("deve detectar PNPM por pnpm-lock.yaml")
        void detectPnpm() throws IOException {
            Files.createFile(tempDir.resolve("pnpm-lock.yaml"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getPackageManager()).isEqualTo(PackageManager.PNPM);
        }

        @Test
        @DisplayName("deve retornar UNKNOWN quando nenhum lock file encontrado")
        void unknownPackageManager() {
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.getPackageManager()).isEqualTo(PackageManager.UNKNOWN);
        }
    }

    // ─── Divergência de framework ─────────────────────────────────────────────

    @Nested
    @DisplayName("Divergência de framework")
    class FrameworkDivergence {

        @Test
        @DisplayName("deve reportar divergência quando informado CYPRESS mas detectado PLAYWRIGHT")
        void divergenceDetected() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectDiscoveryResult result = agent.discover(
                    tempDir,
                    AutomationFramework.CYPRESS,  // informado
                    null
            );
            assertThat(result.hasFrameworkDivergence()).isTrue();
            assertThat(result.getDivergences()).isNotEmpty();
        }

        @Test
        @DisplayName("não deve reportar divergência quando informado e detectado são iguais")
        void noDivergenceWhenEqual() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectDiscoveryResult result = agent.discover(
                    tempDir,
                    AutomationFramework.PLAYWRIGHT,  // informado = detectado
                    null
            );
            assertThat(result.hasFrameworkDivergence()).isFalse();
        }

        @Test
        @DisplayName("não deve reportar divergência quando framework informado é null")
        void noDivergenceWhenInformedIsNull() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
            assertThat(result.hasFrameworkDivergence()).isFalse();
        }
    }

    // ─── Exclusão de node_modules ─────────────────────────────────────────────

    @Test
    @DisplayName("deve excluir node_modules da análise de estrutura")
    void excludesNodeModules() throws IOException {
        Path nodeModules = Files.createDirectory(tempDir.resolve("node_modules"));
        Files.createDirectory(nodeModules.resolve("some-lib"));
        Files.createFile(tempDir.resolve("playwright.config.ts"));

        ProjectDiscoveryResult result = agent.discover(tempDir, null, null);

        // O resultado deve ser bem-sucedido mesmo com node_modules presente
        assertThat(result).isNotNull();
        assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
    }

    // ─── Framework desconhecido ───────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar UNKNOWN quando nenhum framework é detectado")
    void unknownFramework() {
        ProjectDiscoveryResult result = agent.discover(tempDir, null, null);
        assertThat(result.getDetectedFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.getWarnings()).anyMatch(w -> w.toLowerCase().contains("framework"));
    }

    // ─── Framework informado é preservado ────────────────────────────────────

    @Test
    @DisplayName("deve preservar framework informado no resultado")
    void preservesInformedFramework() {
        ProjectDiscoveryResult result = agent.discover(
                tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT
        );
        assertThat(result.getInformedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getInformedLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
    }
}
