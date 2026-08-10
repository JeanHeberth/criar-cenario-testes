package com.br.criarcenariotestes.business.autoqa.discovery;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.CypressDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.PlaywrightDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.RestAssuredDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.RobotFrameworkDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.SeleniumDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.SelenideDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.builder.ProjectDiscoveryResultBuilder;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.GradleBuildParser;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.MavenPomParser;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.PackageJsonParser;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ProjectFilesParser;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.PythonManifestParser;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.AutomationLanguageResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.BuildToolResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.DiscoveryConfidenceResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.PackageManagerResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.TestingFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanPolicy;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanner;
import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaProjectPathNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectDiscoveryService - Testes Unitários")
class ProjectDiscoveryServiceTest {

    /**
     * Fase 13.1A: ProjectDiscoveryService agora exige ProjectPathSecurityValidator
     * (validação autoritativa de allowedRoots). Os testes desta classe testam
     * detecção de framework, não segurança de path — por isso usam uma allowlist
     * permissiva apontando para a raiz temporária real da JVM (java.io.tmpdir),
     * onde @TempDir sempre cria seus diretórios. Isso NÃO é um bypass: é uma
     * allowedRoots legítima e explícita, exercitando a mesma política real
     * (inclusive canonicalização) usada em produção — só mais ampla, porque aqui
     * o alvo é testar o scanner, não a allowlist (essa tem sua própria suíte
     * dedicada em ProjectPathSecurityValidatorTest, e casos de rejeição
     * específicos do Discovery estão nos testes @Test desta classe abaixo).
     */
    private static AutoQaProperties permissiveProperties() {
        AutoQaProperties properties = new AutoQaProperties();
        properties.setAllowedRoots(List.of(System.getProperty("java.io.tmpdir")));
        return properties;
    }

    private final ProjectDiscoveryService service = new ProjectDiscoveryService(
            new ProjectScanner(new ProjectScanPolicy()),
            new ProjectFilesParser(
                    new PackageJsonParser(new ObjectMapper()),
                    new MavenPomParser(),
                    new GradleBuildParser(),
                    new PythonManifestParser()
            ),
            List.of(
                    new PlaywrightDetector(),
                    new CypressDetector(),
                    new RobotFrameworkDetector(),
                    new SelenideDetector(),
                    new SeleniumDetector(),
                    new RestAssuredDetector()
            ),
            new ProjectDiscoveryResultBuilder(
                    new AutomationFrameworkResolver(),
                    new AutomationLanguageResolver(),
                    new PackageManagerResolver(),
                    new BuildToolResolver(),
                    new TestingFrameworkResolver(),
                    new DiscoveryConfidenceResolver()
            ),
            new ProjectPathSecurityValidator(permissiveProperties())
    );
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve detectar Playwright TypeScript com NPM")
    void deveDetectarPlaywrightTypescriptComNpm() throws Exception {
        createPackageJson(List.of("@playwright/test"), List.of());
        write("package-lock.json", "{}");
        write("tsconfig.json", "{}");
        write("playwright.config.ts", "export default {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.NPM);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.NPM);
        assertThat(result.getTestingFrameworks()).containsExactly(TestingFramework.PLAYWRIGHT_TEST);
        assertThat(result.getDetectedFrameworks()).containsExactly(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getLibraries()).containsExactly("PLAYWRIGHT");
        assertThat(result.getConfigurationFile()).isEqualTo("playwright.config.ts");
        assertThat(result.getEvidenceFiles()).contains("package.json", "package-lock.json", "tsconfig.json", "playwright.config.ts");
        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.HIGH);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve detectar Playwright JavaScript")
    void deveDetectarPlaywrightJavascript() throws Exception {
        createPackageJson(List.of("@playwright/test"), List.of());
        write("package-lock.json", "{}");
        write("playwright.config.js", "module.exports = {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVASCRIPT);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.NPM);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.NPM);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.PLAYWRIGHT_TEST);
        assertThat(result.getDetectedFrameworks()).contains(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getConfigurationFile()).isEqualTo("playwright.config.js");
    }

    @Test
    @DisplayName("Deve detectar Playwright por dependência")
    void deveDetectarPlaywrightPorDependencia() throws Exception {
        createPackageJson(List.of("@playwright/test"), List.of());

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.PLAYWRIGHT_TEST);
        assertThat(result.getConfigurationFile()).isEqualTo("package.json");
        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Deve detectar Cypress TypeScript com NPM")
    void deveDetectarCypressTypescriptComNpm() throws Exception {
        createPackageJson(List.of("cypress"), List.of());
        write("package-lock.json", "{}");
        write("tsconfig.json", "{}");
        write("cypress.config.ts", "export default {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.CYPRESS);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.TYPESCRIPT);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.NPM);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.NPM);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.CYPRESS);
        assertThat(result.getDetectedFrameworks()).contains(AutomationFramework.CYPRESS);
        assertThat(result.getConfigurationFile()).isEqualTo("cypress.config.ts");
    }

    @Test
    @DisplayName("Deve detectar Cypress JavaScript")
    void deveDetectarCypressJavascript() throws Exception {
        createPackageJson(List.of("cypress"), List.of());
        write("package-lock.json", "{}");
        write("cypress.config.js", "module.exports = {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.CYPRESS);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVASCRIPT);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.NPM);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.NPM);
    }

    @Test
    @DisplayName("Deve detectar Robot Framework por arquivo robot")
    void deveDetectarRobotFrameworkPorArquivoRobot() throws Exception {
        write("tests/login.robot", "*** Test Cases ***");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.ROBOT_FRAMEWORK);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.ROBOT);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.ROBOT);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.ROBOT);
        assertThat(result.getEvidenceFiles()).contains("tests/login.robot");
        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.LOW);
    }

    @Test
    @DisplayName("Deve detectar Robot Framework por requirements")
    void deveDetectarRobotFrameworkPorRequirements() throws Exception {
        write("requirements.txt", "robotframework==7.0.1");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.ROBOT_FRAMEWORK);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.PIP);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.ROBOT);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.ROBOT);
        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Deve detectar Selenide com Gradle")
    void deveDetectarSelenideComGradle() throws Exception {
        write("build.gradle", """
                plugins {
                    id 'java'
                }
                dependencies {
                    testImplementation 'com.codeborne:selenide:7.3.4'
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
                }
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.SELENIDE);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVA);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.GRADLE);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.JUNIT_5);
        assertThat(result.getDetectedFrameworks()).contains(AutomationFramework.SELENIDE);
        assertThat(result.getLibraries()).contains("SELENIDE");
        assertThat(result.getConfigurationFile()).isEqualTo("build.gradle");
    }

    @Test
    @DisplayName("Deve detectar Selenide com Maven")
    void deveDetectarSelenideComMaven() throws Exception {
        write("pom.xml", pomXml("""
                <dependency>
                    <groupId>com.codeborne</groupId>
                    <artifactId>selenide</artifactId>
                    <version>7.3.4</version>
                </dependency>
                <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter</artifactId>
                    <version>5.10.0</version>
                </dependency>
                """));

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.SELENIDE);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.MAVEN);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVA);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.JUNIT_5);
    }

    @Test
    @DisplayName("Deve detectar Selenium com Java e Gradle")
    void deveDetectarSeleniumComJavaEGradle() throws Exception {
        write("build.gradle", """
                dependencies {
                    testImplementation 'org.seleniumhq.selenium:selenium-java:4.23.0'
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
                }
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.SELENIUM);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVA);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.GRADLE);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.JUNIT_5);
        assertThat(result.getLibraries()).contains("SELENIUM");
        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.HIGH);
    }

    @Test
    @DisplayName("Deve detectar Selenium com Python")
    void deveDetectarSeleniumComPython() throws Exception {
        write("requirements.txt", """
                selenium==4.23.0
                pytest==8.3.2
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.SELENIUM);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.PYTHON);
        assertThat(result.getPackageManager()).isEqualTo(PackageManager.PIP);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.PYTEST);
        assertThat(result.getLibraries()).contains("SELENIUM");
    }

    @Test
    @DisplayName("Deve detectar Rest Assured com Gradle")
    void deveDetectarRestAssuredComGradle() throws Exception {
        write("build.gradle", """
                dependencies {
                    testImplementation 'io.rest-assured:rest-assured:5.5.0'
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
                }
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.REST_ASSURED);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVA);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.GRADLE);
        assertThat(result.getTestingFrameworks()).contains(TestingFramework.JUNIT_5);
    }

    @Test
    @DisplayName("Deve detectar Rest Assured com Maven")
    void deveDetectarRestAssuredComMaven() throws Exception {
        write("pom.xml", pomXml("""
                <dependency>
                    <groupId>io.rest-assured</groupId>
                    <artifactId>rest-assured</artifactId>
                    <version>5.5.0</version>
                </dependency>
                <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter</artifactId>
                    <version>5.10.0</version>
                </dependency>
                """));

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.REST_ASSURED);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.MAVEN);
        assertThat(result.getLanguage()).isEqualTo(AutomationLanguage.JAVA);
    }

    @Test
    @DisplayName("Deve detectar JUnit 5")
    void deveDetectarJUnit5() throws Exception {
        write("build.gradle", """
                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
                }
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getTestingFrameworks()).contains(TestingFramework.JUNIT_5);
    }

    @Test
    @DisplayName("Deve detectar TestNG")
    void deveDetectarTestNG() throws Exception {
        write("build.gradle", """
                dependencies {
                    testImplementation 'org.testng:testng:7.10.2'
                }
                """);

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getTestingFrameworks()).contains(TestingFramework.TESTNG);
    }

    @Test
    @DisplayName("Deve detectar Yarn")
    void deveDetectarYarn() throws Exception {
        write("yarn.lock", "# yarn");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getPackageManager()).isEqualTo(PackageManager.YARN);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.YARN);
    }

    @Test
    @DisplayName("Deve detectar Pnpm")
    void deveDetectarPnpm() throws Exception {
        write("pnpm-lock.yaml", "lockfileVersion: 5.4");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getPackageManager()).isEqualTo(PackageManager.PNPM);
        assertThat(result.getBuildTool()).isEqualTo(BuildTool.PNPM);
    }

    @Test
    @DisplayName("Deve detectar Poetry")
    void deveDetectarPoetry() throws Exception {
        write("pyproject.toml", """
                [tool.poetry]
                name = "demo"
                version = "0.1.0"
                dependencies = { python = "^3.12" }
                """);
        write("poetry.lock", "# poetry");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getPackageManager()).isEqualTo(PackageManager.POETRY);
        assertThat(result.getEvidenceFiles()).contains("pyproject.toml", "poetry.lock");
    }

    @Test
    @DisplayName("Deve alertar quando existirem múltiplos lockfiles")
    void deveAlertarQuandoExistiremMultiplosLockfiles() throws Exception {
        write("package-lock.json", "{}");
        write("yarn.lock", "# yarn");
        write("pnpm-lock.yaml", "lockfileVersion: 5.4");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getPackageManager()).isEqualTo(PackageManager.UNKNOWN);
        assertThat(result.getWarnings()).anyMatch(warning -> warning.toLowerCase().contains("lockfile"));
    }

    @Test
    @DisplayName("Deve alertar quando Playwright e Cypress forem detectados")
    void deveAlertarQuandoPlaywrightECypressForemDetectados() throws Exception {
        createPackageJson(List.of("@playwright/test", "cypress"), List.of());
        write("package-lock.json", "{}");
        write("playwright.config.ts", "export default {};");
        write("cypress.config.ts", "export default {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getDetectedFrameworks())
                .contains(AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS);
        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.getWarnings()).anyMatch(warning -> warning.toLowerCase().contains("ambig"));
        assertThat(result.getConfidence()).isNotEqualTo(DiscoveryConfidence.HIGH);
    }

    @Test
    @DisplayName("Deve priorizar Selenide sobre Selenium")
    void devePriorizarSelenideSobreSelenium() throws Exception {
        write("pom.xml", pomXml("""
                <dependency>
                    <groupId>com.codeborne</groupId>
                    <artifactId>selenide</artifactId>
                    <version>7.3.4</version>
                </dependency>
                <dependency>
                    <groupId>org.seleniumhq.selenium</groupId>
                    <artifactId>selenium-java</artifactId>
                    <version>4.23.0</version>
                </dependency>
                """));

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.SELENIDE);
        assertThat(result.getDetectedFrameworks())
                .contains(AutomationFramework.SELENIDE, AutomationFramework.SELENIUM);
    }

    @Test
    @DisplayName("Deve sinalizar projeto híbrido web e API")
    void deveSinalizarProjetoHibridoWebEApi() throws Exception {
        write("pom.xml", pomXml("""
                <dependency>
                    <groupId>com.codeborne</groupId>
                    <artifactId>selenide</artifactId>
                    <version>7.3.4</version>
                </dependency>
                <dependency>
                    <groupId>io.rest-assured</groupId>
                    <artifactId>rest-assured</artifactId>
                    <version>5.5.0</version>
                </dependency>
                """));

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.getDetectedFrameworks())
                .contains(AutomationFramework.SELENIDE, AutomationFramework.REST_ASSURED);
        assertThat(result.getWarnings()).anyMatch(warning -> warning.toLowerCase().contains("híbr"));
    }

    @Test
    @DisplayName("Deve retornar UNKNOWN quando framework não for detectado")
    void deveRetornarUnknownQuandoFrameworkNaoForDetectado() throws Exception {
        write("README.md", "# demo");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.getDetectedFrameworks()).isEmpty();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar caminho nulo")
    void deveRejeitarCaminhoNulo() {
        assertThatThrownBy(() -> service.discover(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho vazio")
    void deveRejeitarCaminhoVazio() {
        assertThatThrownBy(() -> service.discover(Path.of("   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar pasta inexistente")
    void deveRejeitarPastaInexistente() {
        Path missing = tempDir.resolve("missing");

        assertThatThrownBy(() -> service.discover(missing))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar caminho que não seja diretório")
    void deveRejeitarCaminhoQueNaoSejaDiretorio() throws Exception {
        Path file = write("pom.xml", "<project/>");

        assertThatThrownBy(() -> service.discover(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Fase 13.1A: validação autoritativa de allowedRoots, independente de create() ---

    @Test
    @DisplayName("Deve rejeitar projectPath fora de auto-qa.allowed-roots mesmo sendo um diretório válido e legível")
    void deveRejeitarProjectPathForaDeAllowedRoots() throws Exception {
        AutoQaProperties restrictedElsewhere = new AutoQaProperties();
        restrictedElsewhere.setAllowedRoots(List.of(Files.createTempDirectory("outra-raiz-autorizada").toString()));
        ProjectDiscoveryService restrictedService = new ProjectDiscoveryService(
                new ProjectScanner(new ProjectScanPolicy()),
                new ProjectFilesParser(new PackageJsonParser(new ObjectMapper()), new MavenPomParser(),
                        new GradleBuildParser(), new PythonManifestParser()),
                List.of(new PlaywrightDetector()),
                new ProjectDiscoveryResultBuilder(new AutomationFrameworkResolver(), new AutomationLanguageResolver(),
                        new PackageManagerResolver(), new BuildToolResolver(), new TestingFrameworkResolver(),
                        new DiscoveryConfidenceResolver()),
                new ProjectPathSecurityValidator(restrictedElsewhere));

        assertThatThrownBy(() -> restrictedService.discover(tempDir))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar qualquer projectPath quando auto-qa.allowed-roots estiver vazia (fail-closed)")
    void deveRejeitarQuandoAllowedRootsVazia() {
        ProjectDiscoveryService failClosedService = new ProjectDiscoveryService(
                new ProjectScanner(new ProjectScanPolicy()),
                new ProjectFilesParser(new PackageJsonParser(new ObjectMapper()), new MavenPomParser(),
                        new GradleBuildParser(), new PythonManifestParser()),
                List.of(new PlaywrightDetector()),
                new ProjectDiscoveryResultBuilder(new AutomationFrameworkResolver(), new AutomationLanguageResolver(),
                        new PackageManagerResolver(), new BuildToolResolver(), new TestingFrameworkResolver(),
                        new DiscoveryConfidenceResolver()),
                new ProjectPathSecurityValidator(new AutoQaProperties()));

        assertThatThrownBy(() -> failClosedService.discover(tempDir))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar symlink dentro de uma root autorizada que aponte para fora dela (defesa em profundidade)")
    void deveRejeitarSymlinkEscapandoDaRootAutorizada() throws Exception {
        Path allowedRoot = Files.createTempDirectory("root-autorizada-symlink");
        Path outsideTarget = Files.createTempDirectory("fora-da-root-symlink");
        Path escapeLink = allowedRoot.resolve("external");
        Files.createSymbolicLink(escapeLink, outsideTarget);

        AutoQaProperties restricted = new AutoQaProperties();
        restricted.setAllowedRoots(List.of(allowedRoot.toString()));
        ProjectDiscoveryService restrictedService = new ProjectDiscoveryService(
                new ProjectScanner(new ProjectScanPolicy()),
                new ProjectFilesParser(new PackageJsonParser(new ObjectMapper()), new MavenPomParser(),
                        new GradleBuildParser(), new PythonManifestParser()),
                List.of(new PlaywrightDetector()),
                new ProjectDiscoveryResultBuilder(new AutomationFrameworkResolver(), new AutomationLanguageResolver(),
                        new PackageManagerResolver(), new BuildToolResolver(), new TestingFrameworkResolver(),
                        new DiscoveryConfidenceResolver()),
                new ProjectPathSecurityValidator(restricted));

        assertThatThrownBy(() -> restrictedService.discover(escapeLink))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve aceitar projectPath dentro de auto-qa.allowed-roots (caminho feliz da validação autoritativa)")
    void deveAceitarProjectPathDentroDeAllowedRoots() throws Exception {
        write("playwright.config.ts", "export default {};");
        createPackageJson(List.of("@playwright/test"), List.of());

        AutoQaProperties restricted = new AutoQaProperties();
        restricted.setAllowedRoots(List.of(tempDir.getParent().toString()));
        ProjectDiscoveryService restrictedService = new ProjectDiscoveryService(
                new ProjectScanner(new ProjectScanPolicy()),
                new ProjectFilesParser(new PackageJsonParser(new ObjectMapper()), new MavenPomParser(),
                        new GradleBuildParser(), new PythonManifestParser()),
                List.of(new PlaywrightDetector()),
                new ProjectDiscoveryResultBuilder(new AutomationFrameworkResolver(), new AutomationLanguageResolver(),
                        new PackageManagerResolver(), new BuildToolResolver(), new TestingFrameworkResolver(),
                        new DiscoveryConfidenceResolver()),
                new ProjectPathSecurityValidator(restricted));

        ProjectDiscoveryResult result = restrictedService.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
    }

    @Test
    @DisplayName("Deve tratar package.json inválido")
    void deveTratarPackageJsonInvalido() throws Exception {
        write("package.json", "{ invalid json");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.toLowerCase().contains("package.json"));
    }

    @Test
    @DisplayName("Deve ignorar node_modules")
    void deveIgnorarNodeModules() throws Exception {
        write("node_modules/playwright.config.ts", "export default {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getAutomationFramework()).isEqualTo(AutomationFramework.UNKNOWN);
        assertThat(result.getEvidenceFiles()).doesNotContain("node_modules/playwright.config.ts");
    }

    @Test
    @DisplayName("Deve ignorar arquivo env")
    void deveIgnorarArquivoEnv() throws Exception {
        write(".env", "@playwright/test=cannot-read");
        write("playwright.config.ts", "export default {};");
        createPackageJson(List.of("@playwright/test"), List.of());

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getEvidenceFiles()).doesNotContain(".env");
    }

    @Test
    @DisplayName("Deve retornar evidências com caminhos relativos")
    void deveRetornarEvidenciasComCaminhosRelativos() throws Exception {
        write("src/tests/playwright.config.ts", "export default {};");
        createPackageJson(List.of("@playwright/test"), List.of());

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getEvidenceFiles())
                .allSatisfy(evidence -> assertThat(Path.of(evidence).isAbsolute()).isFalse());
        assertThat(result.getEvidenceFiles()).contains("src/tests/playwright.config.ts");
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() throws Exception {
        write("playwright.config.ts", "export default {};");
        createPackageJson(List.of("@playwright/test"), List.of());

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThatThrownBy(() -> result.getTestingFrameworks().add(TestingFramework.UNKNOWN))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getDetectedFrameworks().add(AutomationFramework.UNKNOWN))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getLibraries().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.getEvidenceFiles().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve calcular confiança alta com config e dependência")
    void deveCalcularConfiancaAltaComConfigEDependencia() throws Exception {
        createPackageJson(List.of("@playwright/test"), List.of());
        write("playwright.config.ts", "export default {};");

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.HIGH);
    }

    @Test
    @DisplayName("Deve calcular confiança média com apenas uma evidência")
    void deveCalcularConfiancaMediaComApenasUmaEvidencia() throws Exception {
        createPackageJson(List.of("@playwright/test"), List.of());

        ProjectDiscoveryResult result = service.discover(tempDir);

        assertThat(result.getConfidence()).isEqualTo(DiscoveryConfidence.MEDIUM);
    }

    private Path write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent() == null ? tempDir : file.getParent());
        return Files.writeString(file, content);
    }

    private void createPackageJson(List<String> dependencies, List<String> devDependencies) throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("name", "demo");
        node.put("version", "1.0.0");
        if (!dependencies.isEmpty()) {
            ObjectNode deps = node.putObject("dependencies");
            for (String dependency : dependencies) {
                deps.put(dependency, "1.0.0");
            }
        }
        if (!devDependencies.isEmpty()) {
            ObjectNode deps = node.putObject("devDependencies");
            for (String dependency : devDependencies) {
                deps.put(dependency, "1.0.0");
            }
        }
        write("package.json", mapper.writeValueAsString(node));
    }

    private String pomXml(String dependencies) {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                %s
                  </dependencies>
                </project>
                """.formatted(dependencies);
    }
}
