package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.CommandNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommandPolicyService - Testes Unitários")
class CommandPolicyServiceTest {

    private final CommandPolicyService service = new CommandPolicyService();

    private CommandSpecification spec(ExecutionCommandId id, String executable, List<String> args) {
        return new CommandSpecification(id, executable, args, "projeto", Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST);
    }

    private ExecutionApproval approval(boolean allowBuildCommand, ExecutionCommandId... ids) {
        return new ExecutionApproval(true, "qa.lead", LocalDateTime.now(), Set.of(ids), true, false, allowBuildCommand);
    }

    private ProjectDiscoveryResult gradleDiscovery() {
        return new ProjectDiscoveryResult(Path.of("/project"), AutomationFramework.SELENIDE, AutomationLanguage.JAVA,
                PackageManager.UNKNOWN, BuildTool.GRADLE, Set.of(), Set.of(AutomationFramework.SELENIDE), List.of(),
                "build.gradle", List.of("build.gradle"), List.of(), DiscoveryConfidence.HIGH, true);
    }

    private ProjectDiscoveryResult mavenDiscovery() {
        return new ProjectDiscoveryResult(Path.of("/project"), AutomationFramework.REST_ASSURED, AutomationLanguage.JAVA,
                PackageManager.UNKNOWN, BuildTool.MAVEN, Set.of(), Set.of(AutomationFramework.REST_ASSURED), List.of(),
                "pom.xml", List.of("pom.xml"), List.of(), DiscoveryConfidence.HIGH, true);
    }

    private ProjectDiscoveryResult robotDiscovery() {
        return GenerationTestData.robotDiscovery();
    }

    private ProjectDiscoveryResult pytestDiscovery() {
        return new ProjectDiscoveryResult(Path.of("/project"), AutomationFramework.REST_ASSURED, AutomationLanguage.PYTHON,
                PackageManager.PIP, BuildTool.UNKNOWN, Set.of(TestingFramework.PYTEST), Set.of(), List.of(),
                null, List.of(), List.of(), DiscoveryConfidence.HIGH, true);
    }

    @Test
    @DisplayName("Deve permitir npm test para Playwright")
    void devePermitirNpmTest() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "npm", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir npx playwright test")
    void devePermitirNpxPlaywrightTest() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.PLAYWRIGHT_TEST, "npx", List.of("playwright", "test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.PLAYWRIGHT_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir npx cypress run")
    void devePermitirNpxCypressRun() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.CYPRESS_RUN, "npx", List.of("cypress", "run")),
                GenerationTestData.cypressDiscovery(), approval(false, ExecutionCommandId.CYPRESS_RUN)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir ./gradlew test")
    void devePermitirGradlewTest() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.GRADLE_WRAPPER_TEST, "./gradlew", List.of("test")),
                gradleDiscovery(), approval(false, ExecutionCommandId.GRADLE_WRAPPER_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir ./mvnw test")
    void devePermitirMvnwTest() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.MAVEN_WRAPPER_TEST, "./mvnw", List.of("test")),
                mavenDiscovery(), approval(false, ExecutionCommandId.MAVEN_WRAPPER_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir robot")
    void devePermitirRobot() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.ROBOT_TEST, "python3", List.of("-m", "robot", "tests/")),
                robotDiscovery(), approval(false, ExecutionCommandId.ROBOT_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve permitir pytest")
    void devePermitirPytest() {
        assertThatCode(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("-q")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar npm install disfarçado de NPM_TEST")
    void deveRejeitarNpmInstall() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "npm", List.of("install")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar npm ci disfarçado de NPM_TEST")
    void deveRejeitarNpmCi() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "npm", List.of("ci")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar mvn install disfarçado de MAVEN_TEST")
    void deveRejeitarMvnInstall() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.MAVEN_TEST, "mvn", List.of("install")),
                mavenDiscovery(), approval(false, ExecutionCommandId.MAVEN_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar gradle build disfarçado de GRADLE_WRAPPER_TEST")
    void deveRejeitarGradleBuild() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.GRADLE_WRAPPER_TEST, "./gradlew", List.of("build")),
                gradleDiscovery(), approval(false, ExecutionCommandId.GRADLE_WRAPPER_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable docker")
    void deveRejeitarDocker() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "docker", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable git")
    void deveRejeitarGit() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "git", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable curl")
    void deveRejeitarCurl() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "curl", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable wget")
    void deveRejeitarWget() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "wget", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable bash")
    void deveRejeitarBash() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "bash", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable sh")
    void deveRejeitarSh() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "sh", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable cmd")
    void deveRejeitarCmd() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "cmd", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable powershell")
    void deveRejeitarPowershell() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "powershell", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.NPM_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com pipe")
    void deveRejeitarPipe() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("|", "cat")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com redirecionamento")
    void deveRejeitarRedirect() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of(">out.txt")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com &&")
    void deveRejeitarESE() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("&& rm -rf /")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com ||")
    void deveRejeitarOuOu() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("|| echo x")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com ponto e vírgula")
    void deveRejeitarPontoEVirgula() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("-q; rm -rf /")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar argumento com backtick")
    void deveRejeitarArgumentoPerigoso() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "pytest", List.of("`whoami`")),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable desconhecido")
    void deveRejeitarExecutableDesconhecido() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PYTEST, "binario-desconhecido", List.of()),
                pytestDiscovery(), approval(false, ExecutionCommandId.PYTEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar commandId não aprovado")
    void deveRejeitarCommandIdNaoAprovado() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "npm", List.of("test")),
                GenerationTestData.playwrightDiscovery(), approval(false, ExecutionCommandId.CYPRESS_RUN)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar commandId incompatível com o framework detectado")
    void deveRejeitarQuandoFrameworkIncompativel() {
        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.PLAYWRIGHT_TEST, "npx", List.of("playwright", "test")),
                GenerationTestData.cypressDiscovery(), approval(false, ExecutionCommandId.PLAYWRIGHT_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar clean test sem allowBuildCommand")
    void deveRejeitarCleanTestSemAllowBuildCommand() {
        assertThatThrownBy(() -> service.validate(
                spec(ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST, "./gradlew", List.of("clean", "test")),
                gradleDiscovery(), approval(false, ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST)))
                .isInstanceOf(CommandNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve permitir clean test com allowBuildCommand=true")
    void devePermitirCleanTestComAllowBuildCommand() {
        assertThatCode(() -> service.validate(
                spec(ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST, "./gradlew", List.of("clean", "test")),
                gradleDiscovery(), approval(true, ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar quando approval.approved=false")
    void deveRejeitarQuandoApprovalNaoAprovada() {
        ExecutionApproval naoAprovada = new ExecutionApproval(false, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.NPM_TEST), true, false, false);

        assertThatThrownBy(() -> service.validate(spec(ExecutionCommandId.NPM_TEST, "npm", List.of("test")),
                GenerationTestData.playwrightDiscovery(), naoAprovada))
                .isInstanceOf(CommandNotAllowedException.class);
    }
}
