package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TestResultAnalysisAgent")
class TestResultAnalysisAgentTest {

    private TestResultAnalysisAgent agent;

    @BeforeEach
    void setUp() {
        agent = new TestResultAnalysisAgent();
    }

    // ─── Análise de Sucesso ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Análise de sucesso")
    class SuccessAnalysis {

        @Test
        @DisplayName("marca como sucesso quando exit code = 0")
        void marksSuccessWhenExitCodeZero() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    0,
                    "All tests passed",
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("não adiciona issues quando testes passam")
        void noIssuesWhenSuccess() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    0,
                    "All tests passed",
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues()).isEmpty();
        }
    }

    // ─── Detecção de Falhas ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de falhas")
    class FailureDetection {

        @Test
        @DisplayName("detecta falha quando exit code != 0")
        void detectsFailureWhenExitCodeNonZero() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    "",
                    "Some tests failed",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues()).isNotEmpty();
        }

        @Test
        @DisplayName("adiciona issue de tipo ERROR")
        void addsTestFailureIssue() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    "",
                    "FAIL: src/app.test.ts",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues())
                    .anySatisfy(issue -> assertThat(issue.severity())
                            .isEqualTo(WorkflowIssue.IssueSeverity.ERROR));
        }

        @Test
        @DisplayName("resume de falha contém stderr")
        void summaryContainsStderr() {
            String stderr = "FAIL: spec/test.ts - Cannot find module";
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    "",
                    stderr,
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues())
                    .anySatisfy(issue -> assertThat(issue.message())
                            .contains("Cannot find module"));
        }
    }

    // ─── Contagem de Falhas ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Contagem de falhas")
    class FailureCount {

        @Test
        @DisplayName("conta múltiplas falhas em stdout")
        void countsMultipleFailures() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    2,
                    "FAIL spec/a.test.ts\nFAIL spec/b.test.ts\nFAIL spec/c.test.ts",
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues()).hasSize(3);
        }

        @Test
        @DisplayName("conta múltiplas falhas em stderr")
        void countsMultipleFailuresInStderr() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    "",
                    "Error: Test 1 failed\nError: Test 2 failed\nError: Test 3 failed",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getIssues()).isNotEmpty();
        }
    }

    // ─── Contexto Adicionado ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Contexto adicionado à execução")
    class ContextAddition {

        @Test
        @DisplayName("armazena TestExecutionResult no context")
        void storesResultInContext() {
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    0,
                    "Success",
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getTestExecutionResult()).isEqualTo(result);
        }
    }
}
