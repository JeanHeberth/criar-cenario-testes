package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.exception.InvalidProjectPathException;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectValidationResponse;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProjectPathValidationService")
class ProjectPathValidationServiceTest {

    @TempDir
    Path tempDir;

    private AutoQaProperties properties;
    private ProjectPathValidationService service;

    @BeforeEach
    void setUp() {
        properties = new AutoQaProperties();
        properties.setAllowedRoots(List.of());
        service = new ProjectPathValidationService(properties);
    }

    // ─── Validação de caminho vazio ───────────────────────────────────────────

    @Nested
    @DisplayName("Caminho vazio ou nulo")
    class EmptyPath {

        @Test
        @DisplayName("deve retornar invalid quando path é nulo")
        void nullPath() {
            ProjectValidationResponse result = service.validate(null);
            assertThat(result.valid()).isFalse();
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("deve retornar invalid quando path é em branco")
        void blankPath() {
            ProjectValidationResponse result = service.validate("   ");
            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("resolveSafely deve lançar exceção para path nulo")
        void resolveSafelyNull() {
            assertThatThrownBy(() -> service.resolveSafely(null))
                    .isInstanceOf(InvalidProjectPathException.class);
        }
    }

    // ─── Validação de diretório existente ─────────────────────────────────────

    @Nested
    @DisplayName("Diretório existente")
    class ExistingDirectory {

        @Test
        @DisplayName("deve retornar valid para diretório acessível")
        void validDirectory() {
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.valid()).isTrue();
            assertThat(result.readable()).isTrue();
            assertThat(result.normalizedPath()).isNotNull();
        }

        @Test
        @DisplayName("deve retornar invalid para caminho inexistente")
        void nonExistentPath() {
            String nonExistent = tempDir.resolve("nao-existe").toString();
            ProjectValidationResponse result = service.validate(nonExistent);
            assertThat(result.valid()).isFalse();
        }
    }

    // ─── Bloqueio de raízes do sistema ────────────────────────────────────────

    @Nested
    @DisplayName("Raízes proibidas")
    class ForbiddenRoots {

        @ParameterizedTest
        @ValueSource(strings = {"/", "/root"})
        @DisplayName("deve lançar exceção para raízes do sistema Unix")
        void forbiddenUnixRoot(String path) {
            // Simulamos o comportamento via resolveSafely direto
            // O validate() pode retornar invalid caso o path não exista neste OS
            // Mas resolveSafely deve sempre bloquear
            assertThatThrownBy(() -> service.resolveSafely(path))
                    .isInstanceOf(InvalidProjectPathException.class)
                    .hasMessageContaining("protegido");
        }
    }

    // ─── Bloqueio de path traversal ───────────────────────────────────────────

    @Nested
    @DisplayName("Path traversal")
    class PathTraversal {

        @Test
        @DisplayName("deve retornar invalid quando caminho contém tentativa de traversal para raiz")
        void pathTraversalToRoot() {
            // Usa um caminho que, mesmo após normalização, resultaria em raiz proibida
            String traversal = "/some/dir/../../..";
            // Após normalize: /some/dir/../../.. → /
            // Deve ser bloqueado
            assertThatCode(() -> {
                Path resolved = Path.of(traversal).toAbsolutePath().normalize();
                // Se resolveu para raiz proibida, resolveSafely deve bloquear
                if (resolved.toString().equals("/") || resolved.getNameCount() == 0) {
                    assertThatThrownBy(() -> service.resolveSafely(traversal))
                            .isInstanceOf(InvalidProjectPathException.class);
                }
            }).doesNotThrowAnyException();
        }
    }

    // ─── Detecção de Playwright ───────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de framework Playwright")
    class PlaywrightDetection {

        @Test
        @DisplayName("deve detectar Playwright por playwright.config.ts")
        void detectByConfigTs(  ) throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.ts"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.valid()).isTrue();
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }

        @Test
        @DisplayName("deve detectar Playwright por playwright.config.js")
        void detectByConfigJs() throws IOException {
            Files.createFile(tempDir.resolve("playwright.config.js"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }

        @Test
        @DisplayName("deve detectar Playwright por @playwright/test no package.json")
        void detectByPackageJson() throws IOException {
            Files.writeString(tempDir.resolve("package.json"),
                    "{\"devDependencies\": {\"@playwright/test\": \"^1.0.0\"}}");
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        }
    }

    // ─── Detecção de Cypress ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de framework Cypress")
    class CypressDetection {

        @Test
        @DisplayName("deve detectar Cypress por cypress.config.ts")
        void detectByConfigTs() throws IOException {
            Files.createFile(tempDir.resolve("cypress.config.ts"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.valid()).isTrue();
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }

        @Test
        @DisplayName("deve detectar Cypress por cypress.config.js")
        void detectByConfigJs() throws IOException {
            Files.createFile(tempDir.resolve("cypress.config.js"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }

        @Test
        @DisplayName("deve detectar Cypress por dependência no package.json")
        void detectByPackageJson() throws IOException {
            Files.writeString(tempDir.resolve("package.json"),
                    "{\"devDependencies\": {\"cypress\": \"^13.0.0\"}}");
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.CYPRESS);
        }
    }

    // ─── Detecção de TypeScript ───────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de linguagem TypeScript")
    class TypeScriptDetection {

        @Test
        @DisplayName("deve detectar TypeScript por tsconfig.json")
        void detectByTsConfig() throws IOException {
            Files.createFile(tempDir.resolve("tsconfig.json"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        }

        @Test
        @DisplayName("deve detectar TypeScript por arquivo .ts na raiz")
        void detectByTsFile() throws IOException {
            Files.createFile(tempDir.resolve("test.spec.ts"));
            ProjectValidationResponse result = service.validate(tempDir.toString());
            assertThat(result.detectedLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        }
    }

    // ─── Playwright tem prioridade sobre Cypress ──────────────────────────────

    @Test
    @DisplayName("Playwright deve ter prioridade quando ambos os configs estão presentes")
    void playwrightPriorityOverCypress() throws IOException {
        Files.createFile(tempDir.resolve("playwright.config.ts"));
        Files.createFile(tempDir.resolve("cypress.config.ts"));
        ProjectValidationResponse result = service.validate(tempDir.toString());
        assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
    }

    // ─── Framework desconhecido ───────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar UNKNOWN quando nenhum framework é detectado")
    void unknownFramework() {
        ProjectValidationResponse result = service.validate(tempDir.toString());
        assertThat(result.detectedFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.warnings()).anyMatch(w -> w.contains("Framework não detectado"));
    }

    // ─── Raízes permitidas configuradas ──────────────────────────────────────

    @Test
    @DisplayName("deve retornar invalid quando projeto está fora das raízes permitidas")
    void outsideAllowedRoots() throws IOException {
        Path allowedRoot = Files.createTempDirectory("allowed-root");
        properties.setAllowedRoots(List.of(allowedRoot.toString()));

        // tempDir não está dentro de allowedRoot
        ProjectValidationResponse result = service.validate(tempDir.toString());
        assertThat(result.valid()).isFalse();
        assertThat(result.warnings()).anyMatch(w -> w.contains("raízes permitidas"));
    }

    @Test
    @DisplayName("deve aceitar projeto dentro da raiz permitida")
    void insideAllowedRoot() throws IOException {
        Path subProject = Files.createDirectory(tempDir.resolve("meu-projeto"));
        properties.setAllowedRoots(List.of(tempDir.toString()));

        ProjectValidationResponse result = service.validate(subProject.toString());
        assertThat(result.valid()).isTrue();
    }
}
