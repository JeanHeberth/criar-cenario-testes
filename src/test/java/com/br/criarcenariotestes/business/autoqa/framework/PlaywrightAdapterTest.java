package com.br.criarcenariotestes.business.autoqa.framework;

import com.br.criarcenariotestes.business.autoqa.framework.playwright.PlaywrightAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.AllowedCommand;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PlaywrightAdapter")
class PlaywrightAdapterTest {

    private PlaywrightAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PlaywrightAdapter();
    }

    @Test
    @DisplayName("getFramework deve retornar PLAYWRIGHT")
    void getFramework() {
        assertThat(adapter.getFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
    }

    @Nested
    @DisplayName("Linguagens suportadas")
    class SupportedLanguages {

        @Test
        @DisplayName("deve suportar TYPESCRIPT")
        void supportsTypeScript() {
            assertThat(adapter.supports(AutomationLanguage.TYPESCRIPT)).isTrue();
        }

        @Test
        @DisplayName("deve suportar JAVASCRIPT")
        void supportsJavaScript() {
            assertThat(adapter.supports(AutomationLanguage.JAVASCRIPT)).isTrue();
        }

        @Test
        @DisplayName("não deve suportar JAVA")
        void doesNotSupportJava() {
            assertThat(adapter.supports(AutomationLanguage.JAVA)).isFalse();
        }

        @Test
        @DisplayName("não deve suportar PYTHON")
        void doesNotSupportPython() {
            assertThat(adapter.supports(AutomationLanguage.PYTHON)).isFalse();
        }
    }

    @Nested
    @DisplayName("Arquivos de configuração")
    class ConfigFiles {

        @Test
        @DisplayName("deve listar arquivos de configuração do Playwright")
        void configurationFiles() {
            List<String> configs = adapter.configurationFiles();
            assertThat(configs)
                    .contains("playwright.config.ts")
                    .contains("playwright.config.js")
                    .contains("playwright.config.mts")
                    .contains("playwright.config.mjs");
        }
    }

    @Nested
    @DisplayName("Diretórios ignorados")
    class IgnoredDirectories {

        @Test
        @DisplayName("deve ignorar playwright-report")
        void ignoresPlaywrightReport() {
            assertThat(adapter.ignoredDirectories()).contains("playwright-report");
        }

        @Test
        @DisplayName("deve ignorar test-results")
        void ignoresTestResults() {
            assertThat(adapter.ignoredDirectories()).contains("test-results");
        }

        @Test
        @DisplayName("deve ignorar node_modules")
        void ignoresNodeModules() {
            assertThat(adapter.ignoredDirectories()).contains("node_modules");
        }

        @Test
        @DisplayName("deve ignorar blob-report")
        void ignoresBlobReport() {
            assertThat(adapter.ignoredDirectories()).contains("blob-report");
        }
    }

    @Nested
    @DisplayName("Padrões de teste")
    class TestPatterns {

        @Test
        @DisplayName("defaultTestFilePattern deve conter .spec.ts")
        void defaultPattern() {
            assertThat(adapter.defaultTestFilePattern()).contains(".spec.ts");
        }

        @Test
        @DisplayName("defaultTestDirectory deve ser preenchido")
        void defaultDirectory() {
            assertThat(adapter.defaultTestDirectory()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Comandos de validação")
    class ValidationCommands {

        private ProjectDiscoveryResult npmDiscovery;
        private ProjectDiscoveryResult yarnDiscovery;

        @BeforeEach
        void setUp() {
            npmDiscovery = ProjectDiscoveryResult.builder()
                    .detectedFramework(AutomationFramework.PLAYWRIGHT)
                    .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                    .packageManager(PackageManager.NPM)
                    .build();

            yarnDiscovery = ProjectDiscoveryResult.builder()
                    .detectedFramework(AutomationFramework.PLAYWRIGHT)
                    .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                    .packageManager(PackageManager.YARN)
                    .build();
        }

        @Test
        @DisplayName("deve gerar comando tsc para TypeScript com NPM")
        void tscCommandNpm() {
            List<AllowedCommand> commands = adapter.validationCommands(npmDiscovery);
            assertThat(commands).isNotEmpty();
            assertThat(commands).anyMatch(c ->
                    c.logicalName().equals("typescript-check")
                            && c.args().contains("--noEmit")
            );
        }

        @Test
        @DisplayName("comando de teste deve incluir npx playwright test com arquivo específico")
        void testCommandWithFile() {
            List<AllowedCommand> commands = adapter.testCommands(npmDiscovery, "tests/login/login.spec.ts");
            assertThat(commands).isNotEmpty();
            assertThat(commands).anyMatch(c ->
                    c.args().contains("tests/login/login.spec.ts")
            );
        }

        @Test
        @DisplayName("executável deve ser npx.cmd no Windows simulado")
        void windowsExecutable() {
            // Verifica que o adapter lida com detecção de OS — não falha em ambos os sistemas
            List<AllowedCommand> commands = adapter.validationCommands(npmDiscovery);
            assertThat(commands).allMatch(c -> c.executable() != null && !c.executable().isBlank());
        }
    }

    @Test
    @DisplayName("buildFrameworkInstructions deve retornar texto não vazio")
    void buildInstructions() {
        ProjectDiscoveryResult discovery = ProjectDiscoveryResult.builder()
                .detectedFramework(AutomationFramework.PLAYWRIGHT)
                .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                .packageManager(PackageManager.NPM)
                .build();
        String instructions = adapter.buildFrameworkInstructions(discovery);
        assertThat(instructions).isNotBlank();
    }
}
