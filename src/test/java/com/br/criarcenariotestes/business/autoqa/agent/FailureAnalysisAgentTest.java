package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FailureAnalysisAgent")
class FailureAnalysisAgentTest {

    private FailureAnalysisAgent agent;

    @BeforeEach
    void setUp() {
        agent = new FailureAnalysisAgent();
    }

    // ─── Detecção de Tipo de Erro ─────────────────────────────────────────

    @Nested
    @DisplayName("Detecção de tipo de erro")
    class FailureTypeDetection {

        @Test
        @DisplayName("detecta MissingImport em JavaScript")
        void detectsMissingImportJS() {
            String stderr = "Error: Cannot find module 'lodash' at app.test.ts:5";
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

            assertThat(ctx.getFailureAnalyses()).isNotEmpty();
            assertThat(ctx.getFailureAnalyses().get(0).failureType())
                    .isEqualTo("MissingImport");
        }

        @Test
        @DisplayName("detecta AssertionFailed")
        void detectsAssertionFailed() {
            String stdout = "AssertionError: expected 'value' to be 'expected'\n  at Object.<anonymous>";
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    stdout,
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getFailureAnalyses())
                    .anySatisfy(f -> assertThat(f.failureType())
                            .isEqualTo("AssertionFailed"));
        }

        @Test
        @DisplayName("detecta TimeoutError")
        void detectsTimeoutError() {
            String stderr = "Timeout of 5000ms exceeded. Async callback was not invoked";
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

            assertThat(ctx.getFailureAnalyses())
                    .anySatisfy(f -> assertThat(f.failureType())
                            .isEqualTo("TimeoutError"));
        }
    }

    // ─── Extração de Informações ──────────────────────────────────────────

    @Nested
    @DisplayName("Extração de informações de erro")
    class ErrorInformationExtraction {

        @Test
        @DisplayName("extrai arquivo de origem")
        void extractsSourceFile() {
            String stderr = "Error: Cannot find module 'lodash' at src/app.test.ts:5";
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

            assertThat(ctx.getFailureAnalyses())
                    .anySatisfy(f -> assertThat(f.sourceFile())
                            .contains("app.test.ts"));
        }

        @Test
        @DisplayName("extrai número de linha")
        void extractsLineNumber() {
            String stderr = "AssertionError at line 42 in test.spec.js";
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

            assertThat(ctx.getFailureAnalyses())
                    .anySatisfy(f -> assertThat(f.lineNumber()).isGreaterThan(0));
        }

        @Test
        @DisplayName("armazena stack trace completo")
        void storesCompleteStackTrace() {
            String stackTrace = "Error: Timeout\n  at callback\n  at executeTest";
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    1,
                    stackTrace,
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getFailureAnalyses())
                    .anySatisfy(f -> assertThat(f.stackTrace())
                            .contains("Timeout"));
        }
    }

    // ─── Múltiplas Falhas ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Análise de múltiplas falhas")
    class MultipleFailures {

        @Test
        @DisplayName("detecta múltiplas falhas em um único resultado")
        void detectsMultipleFailures() {
            String stdout = "FAIL test1.js: AssertionError at line 10\n" +
                           "FAIL test2.js: TimeoutError at line 25";
            TestExecutionResult result = new TestExecutionResult(
                    "exec-123",
                    "npm",
                    "npm run test",
                    2,
                    stdout,
                    "",
                    LocalDateTime.now()
            );

            AutoQaContext ctx = new AutoQaContext(null, null, null, null, "exec-123");
            ctx.setTestExecutionResult(result);

            agent.execute(ctx);

            assertThat(ctx.getFailureAnalyses()).hasSize(2);
        }
    }
}
