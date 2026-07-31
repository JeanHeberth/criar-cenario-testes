package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TestExecutionAgent")
class TestExecutionAgentTest {

    @TempDir
    Path projectDir;

    private TestExecutionAgent agent;

    @BeforeEach
    void setUp() {
        agent = new TestExecutionAgent();
    }

    // ─── Detecção de Framework ────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de framework")
    class FrameworkDetection {

        @Test
        @DisplayName("detecta package.json (Node.js)")
        void detectsNodeJs() throws Exception {
            Files.writeString(projectDir.resolve("package.json"), "{}");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.framework()).isEqualTo("node");
            assertThat(result.command()).contains("npm");
        }

        @Test
        @DisplayName("detecta build.gradle (Java/Gradle)")
        void detectsGradle() throws Exception {
            Files.writeString(projectDir.resolve("build.gradle"), "");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.framework()).isEqualTo("gradle");
            assertThat(result.command()).contains("./gradlew");
        }

        @Test
        @DisplayName("detecta pom.xml (Maven)")
        void detectsMaven() throws Exception {
            Files.writeString(projectDir.resolve("pom.xml"), "");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.framework()).isEqualTo("maven");
            assertThat(result.command()).contains("mvn");
        }

        @Test
        @DisplayName("retorna UNKNOWN quando não detecta framework")
        void returnsUnknownWhenNoFramework() {
            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.framework()).isEqualTo("unknown");
            assertThat(result.success()).isFalse();
        }
    }

    // ─── Construção de Comando ────────────────────────────────────────────────

    @Nested
    @DisplayName("Construção de comando")
    class CommandBuilding {

        @Test
        @DisplayName("npm run test para Node.js")
        void npmRunTest() throws Exception {
            Files.writeString(projectDir.resolve("package.json"), "{}");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.command()).contains("npm", "run", "test");
        }

        @Test
        @DisplayName("./gradlew test para Gradle")
        void gradlewTest() throws Exception {
            Files.writeString(projectDir.resolve("build.gradle"), "");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.command()).contains("./gradlew", "test");
        }

        @Test
        @DisplayName("mvn test para Maven")
        void mvnTest() throws Exception {
            Files.writeString(projectDir.resolve("pom.xml"), "");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, null);
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.command()).contains("mvn", "test");
        }
    }

    // ─── Metadados de Execução ────────────────────────────────────────────────

    @Nested
    @DisplayName("Metadados de execução")
    class ExecutionMetadata {

        @Test
        @DisplayName("preenchimento de executionId")
        void populatesExecutionId() throws Exception {
            Files.writeString(projectDir.resolve("package.json"), "{}");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.executionId()).isEqualTo("exec-123");
        }

        @Test
        @DisplayName("timestamp de execução é preenchido")
        void timestampIsPopulated() throws Exception {
            Files.writeString(projectDir.resolve("package.json"), "{}");

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setProjectPath(projectDir.toString());

            TestExecutionResult result = agent.executeTests(ctx);

            assertThat(result.executedAt()).isNotNull();
        }
    }
}
