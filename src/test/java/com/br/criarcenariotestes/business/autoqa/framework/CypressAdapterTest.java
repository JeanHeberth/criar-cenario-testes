package com.br.criarcenariotestes.business.autoqa.framework;

import com.br.criarcenariotestes.business.autoqa.framework.cypress.CypressAdapter;
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

@DisplayName("CypressAdapter")
class CypressAdapterTest {

    private CypressAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CypressAdapter();
    }

    @Test
    @DisplayName("getFramework deve retornar CYPRESS")
    void getFramework() {
        assertThat(adapter.getFramework()).isEqualTo(AutomationFramework.CYPRESS);
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
    }

    @Nested
    @DisplayName("Arquivos de configuração")
    class ConfigFiles {

        @Test
        @DisplayName("deve listar arquivos de configuração do Cypress")
        void configurationFiles() {
            List<String> configs = adapter.configurationFiles();
            assertThat(configs)
                    .contains("cypress.config.ts")
                    .contains("cypress.config.js");
        }
    }

    @Nested
    @DisplayName("Diretórios ignorados")
    class IgnoredDirectories {

        @Test
        @DisplayName("deve ignorar cypress/videos")
        void ignoresCypressVideos() {
            assertThat(adapter.ignoredDirectories()).contains("cypress/videos");
        }

        @Test
        @DisplayName("deve ignorar cypress/screenshots")
        void ignoresCypressScreenshots() {
            assertThat(adapter.ignoredDirectories()).contains("cypress/screenshots");
        }

        @Test
        @DisplayName("deve ignorar node_modules")
        void ignoresNodeModules() {
            assertThat(adapter.ignoredDirectories()).contains("node_modules");
        }
    }

    @Nested
    @DisplayName("Padrões de teste")
    class TestPatterns {

        @Test
        @DisplayName("defaultTestFilePattern deve conter .cy.ts")
        void defaultPattern() {
            assertThat(adapter.defaultTestFilePattern()).contains(".cy.ts");
        }

        @Test
        @DisplayName("defaultTestDirectory deve ser preenchido")
        void defaultDirectory() {
            assertThat(adapter.defaultTestDirectory()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Comandos de execução")
    class ExecutionCommands {

        private ProjectDiscoveryResult discovery;

        @BeforeEach
        void setUp() {
            discovery = ProjectDiscoveryResult.builder()
                    .detectedFramework(AutomationFramework.CYPRESS)
                    .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                    .packageManager(PackageManager.NPM)
                    .build();
        }

        @Test
        @DisplayName("deve gerar comando cypress run com --spec para arquivo específico")
        void testCommandWithSpec() {
            List<AllowedCommand> commands = adapter.testCommands(discovery, "cypress/e2e/login.cy.ts");
            assertThat(commands).isNotEmpty();
            assertThat(commands).anyMatch(c ->
                    c.args().contains("--spec") || c.args().stream().anyMatch(a -> a.contains("login.cy.ts"))
            );
        }

        @Test
        @DisplayName("executável não deve ser nulo ou vazio")
        void executableNotBlank() {
            List<AllowedCommand> commands = adapter.testCommands(discovery, "cypress/e2e/login.cy.ts");
            assertThat(commands).allMatch(c -> c.executable() != null && !c.executable().isBlank());
        }
    }
}
