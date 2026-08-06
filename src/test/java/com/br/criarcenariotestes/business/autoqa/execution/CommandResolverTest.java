package com.br.criarcenariotestes.business.autoqa.execution;

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
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommandResolver - Testes Unitários")
class CommandResolverTest {

    private CommandResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CommandResolver();
        resolver.setHostEnvironment(Map.of("PATH", "/usr/bin"));
    }

    private ProjectDiscoveryResult discovery(Path root, AutomationFramework framework, BuildTool buildTool,
                                              Set<TestingFramework> testingFrameworks) {
        return new ProjectDiscoveryResult(root, framework, AutomationLanguage.JAVA, PackageManager.UNKNOWN,
                buildTool, testingFrameworks, Set.of(framework), List.of(), null, List.of(), List.of(),
                DiscoveryConfidence.HIGH, true);
    }

    private ProjectKnowledgeResult knowledge(List<String> testDirectories) {
        return new ProjectKnowledgeResult(Path.of("/ignored"), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                testDirectories, List.of(), List.of(), List.of(), KnowledgeStatus.COMPLETE, true);
    }

    private ExecutionApproval approval(ExecutionCommandId... ids) {
        return new ExecutionApproval(true, "qa.lead", LocalDateTime.now(), Set.of(ids), true, false, true);
    }

    @Test
    @DisplayName("Deve priorizar o wrapper do Gradle quando presente")
    void devePriorizarGradleWrapper(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("gradlew"));
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.SELENIDE, BuildTool.GRADLE, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.GRADLE_WRAPPER_TEST, ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.GRADLE_WRAPPER_TEST);
        assertThat(spec.get().executable()).isEqualTo("./gradlew");
    }

    @Test
    @DisplayName("Deve priorizar o wrapper do Maven quando presente")
    void devePriorizarMavenWrapper(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("mvnw"));
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.REST_ASSURED, BuildTool.MAVEN, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.MAVEN_WRAPPER_TEST, ExecutionCommandId.MAVEN_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.MAVEN_WRAPPER_TEST);
        assertThat(spec.get().executable()).isEqualTo("./mvnw");
    }

    @Test
    @DisplayName("Deve resolver Playwright para npx playwright test")
    void deveResolverPlaywright(@TempDir Path root) {
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.PLAYWRIGHT, BuildTool.NPM, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.PLAYWRIGHT_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.PLAYWRIGHT_TEST);
        assertThat(spec.get().arguments()).containsExactly("playwright", "test");
    }

    @Test
    @DisplayName("Deve resolver Cypress para npx cypress run")
    void deveResolverCypress(@TempDir Path root) {
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.CYPRESS, BuildTool.NPM, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.CYPRESS_RUN));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.CYPRESS_RUN);
    }

    @Test
    @DisplayName("Deve resolver Robot com diretório de testes conhecido")
    void deveResolverRobot(@TempDir Path root) {
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.ROBOT_FRAMEWORK, BuildTool.ROBOT, Set.of()),
                knowledge(List.of("tests/robot")), approval(ExecutionCommandId.ROBOT_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.ROBOT_TEST);
        assertThat(spec.get().arguments()).containsExactly("-m", "robot", "tests/robot");
    }

    @Test
    @DisplayName("Deve resolver Pytest quando testingFrameworks contém PYTEST")
    void deveResolverPytest(@TempDir Path root) {
        resolver.setOsNameSupplier(() -> "Mac OS X");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.UNKNOWN, BuildTool.UNKNOWN, Set.of(TestingFramework.PYTEST)),
                knowledge(List.of()), approval(ExecutionCommandId.PYTEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().commandId()).isEqualTo(ExecutionCommandId.PYTEST);
    }

    @Test
    @DisplayName("Deve resolver executável de wrapper do Windows quando SO é Windows")
    void deveResolverWindowsWrapper(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("gradlew.bat"));
        resolver.setOsNameSupplier(() -> "Windows 11");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.SELENIDE, BuildTool.GRADLE, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.GRADLE_WRAPPER_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().executable()).isEqualTo("gradlew.bat");
    }

    @Test
    @DisplayName("Deve resolver executável de wrapper de macOS/Linux quando SO não é Windows")
    void deveResolverMacLinuxWrapper(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("gradlew"));
        resolver.setOsNameSupplier(() -> "Linux");

        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.SELENIDE, BuildTool.GRADLE, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.GRADLE_WRAPPER_TEST));

        assertThat(spec).isPresent();
        assertThat(spec.get().executable()).isEqualTo("./gradlew");
    }

    @Test
    @DisplayName("Deve bloquear (Optional vazio) quando framework é UNKNOWN e não há testingFramework conhecido")
    void deveBloquearFrameworkUnknown(@TempDir Path root) {
        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.UNKNOWN, BuildTool.UNKNOWN, Set.of()),
                knowledge(List.of()), approval());

        assertThat(spec).isEmpty();
    }

    @Test
    @DisplayName("Deve bloquear quando não há wrapper e nenhum comando de fallback foi aprovado")
    void deveBloquearSemComandoConhecido(@TempDir Path root) {
        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.SELENIDE, BuildTool.GRADLE, Set.of()),
                knowledge(List.of()), approval(ExecutionCommandId.GRADLE_WRAPPER_TEST));

        assertThat(spec).isEmpty();
    }

    @Test
    @DisplayName("Nunca deve resolver um comando de instalação de dependências")
    void deveNaoInstalarDependencias(@TempDir Path root) {
        Optional<CommandSpecification> spec = resolver.resolve(
                discovery(root, AutomationFramework.SELENIDE, BuildTool.GRADLE, Set.of()),
                knowledge(List.of()),
                approval(ExecutionCommandId.GRADLE_WRAPPER_TEST, ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST));

        assertThat(spec).isEmpty();
    }

    @Test
    @DisplayName("Deve ser determinístico: mesma entrada produz a mesma resolução")
    void deveSerDeterministico(@TempDir Path root) {
        resolver.setOsNameSupplier(() -> "Mac OS X");
        ProjectDiscoveryResult disco = discovery(root, AutomationFramework.PLAYWRIGHT, BuildTool.NPM, Set.of());
        ExecutionApproval approval = approval(ExecutionCommandId.PLAYWRIGHT_TEST);

        Optional<CommandSpecification> first = resolver.resolve(disco, knowledge(List.of()), approval);
        Optional<CommandSpecification> second = resolver.resolve(disco, knowledge(List.of()), approval);

        assertThat(first).isEqualTo(second);
    }
}
