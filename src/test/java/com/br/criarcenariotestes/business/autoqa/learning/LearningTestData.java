package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.apply.*;
import com.br.criarcenariotestes.business.autoqa.model.execution.*;
import com.br.criarcenariotestes.business.autoqa.model.failure.*;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.review.*;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fixtures compartilhadas para os testes da Fase 11 (Learning). Espelha
 * o estilo de GenerationTestData: apenas construção de dados de teste, sem
 * lógica de produção.
 */
public final class LearningTestData {
    private LearningTestData() {}

    public static ProjectKnowledgeResult knowledgeWithReuseCandidates(String... paths) {
        List<ReuseCandidate> candidates = List.of(paths).stream()
                .map(p -> new ReuseCandidate(p, ComponentType.PAGE_OBJECT, "Componente compatível com o cenário",
                        ReuseConfidence.HIGH, List.of("login")))
                .toList();
        return new ProjectKnowledgeResult(
                Path.of("/project"), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                candidates,
                new NamingConvention("*.spec.ts", "*Page.ts", "PascalCase", "camelCase", "tests", List.of(), ReuseConfidence.HIGH),
                List.of("tests"), List.of("src"), List.of("node_modules"), List.of(), KnowledgeStatus.COMPLETE, true
        );
    }

    public static GenerationResult generationWithReusedFiles(UUID executionId, String... reusedFiles) {
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(), List.of(reusedFiles), List.of(),
                "root", "manifest.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    public static CodeReviewResult approvedReview(UUID executionId, String... passedRules) {
        return new CodeReviewResult(executionId, List.of(), List.of(), List.of(), List.of(passedRules), List.of(),
                List.of(), ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }

    public static ApplyResult completedApply(UUID executionId, String... appliedPaths) {
        List<AppliedFile> files = List.of(appliedPaths).stream()
                .map(p -> new AppliedFile(p, ApplyOperation.CREATE, ApplyFileStatus.APPLIED, "sha-src", "sha-applied",
                        false, null, List.of()))
                .toList();
        return new ApplyResult(executionId, files, List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/" + executionId, ApplyStatus.COMPLETED, false, true);
    }

    public static CommandSpecification command() {
        return new CommandSpecification(ExecutionCommandId.PLAYWRIGHT_TEST, "npx", List.of("playwright", "test"),
                "projeto", Duration.ofMinutes(10), Map.of(), ExecutionCommandType.TEST);
    }

    public static ExecutionResult passedExecution(UUID executionId, String framework, int total) {
        TestExecutionSummary summary = new TestExecutionSummary(framework, total, total, 0, 0, 0, List.of(), List.of());
        return new ExecutionResult(executionId, command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(5), "saida ok", "", false, false,
                List.of(summary), List.of(), true);
    }

    public static ExecutionResult failedExecution(UUID executionId, String framework, int total, int failed, String... failedTests) {
        TestExecutionSummary summary = new TestExecutionSummary(framework, total, total - failed, failed, 0, 0,
                List.of(failedTests), List.of());
        return new ExecutionResult(executionId, command(), ExecutionStatus.FAILED, 1,
                Instant.now(), Instant.now(), Duration.ofSeconds(5), "saida", "AssertionError: expected true", false, false,
                List.of(summary), List.of(), true);
    }

    public static ExecutionResult operationalExecution(UUID executionId, ExecutionStatus status) {
        return new ExecutionResult(executionId, null, status, null,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), null, null, false, false,
                List.of(), List.of(), true);
    }

    public static FailureAnalysisResult noFailureResult(UUID executionId) {
        return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(),
                FailureAnalysisStatus.NO_FAILURE, FailureConfidence.HIGH, false, false, false, true);
    }

    public static FailureAnalysisResult analyzedResult(UUID executionId, FailureCategory category, String testName) {
        FailureEvidence evidence = new FailureEvidence("test-summary", null, null, testName, "test failed", testName, true);
        FailureFinding finding = new FailureFinding("DF-1", category, FailureOrigin.TEST_CODE, FailureSeverity.HIGH,
                FailureConfidence.HIGH, "Falha determinística", "Falha detectada deterministicamente",
                "Causa provável", List.of(testName), List.of(), List.of(evidence), true, false);
        return new FailureAnalysisResult(executionId, List.of(finding), List.of(), List.of(evidence), List.of(),
                FailureAnalysisStatus.ANALYZED, FailureConfidence.HIGH, true, false, false, true);
    }

    public static FailureAnalysisResult blockedResult(UUID executionId) {
        FailureWarning warning = new FailureWarning("OPERATIONAL", "Execution status ERROR");
        return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(warning),
                FailureAnalysisStatus.BLOCKED, FailureConfidence.UNKNOWN, true, false, false, true);
    }

    public static FailureAnalysisResult invalidResult(UUID executionId) {
        return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(),
                FailureAnalysisStatus.INVALID, FailureConfidence.UNKNOWN, true, false, false, false);
    }

    /**
     * Monta um AutoQaContext com as 9 fases anteriores registradas, terminando em
     * ExecutionStatus.PASSED com reuso de componentes/arquivos e review/apply aprovados.
     */
    public static AutoQaContext passedContext() {
        AutoQaContext context = AutoQaContext.create("Login válido", "/projeto");
        UUID executionId = context.getExecutionId();
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(knowledgeWithReuseCandidates("tests/support/loginPage.ts"));
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)));
        context.registerGeneration(generationWithReusedFiles(executionId, "tests/support/loginPage.ts"));
        context.registerCodeReview(approvedReview(executionId, "no-hardcoded-wait", "assertion-present"));
        context.registerApplyApproval(new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true));
        context.registerApplyResult(completedApply(executionId, "tests/login.spec.ts"));
        context.registerExecutionApproval(new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false));
        context.registerExecutionResult(passedExecution(executionId, "PLAYWRIGHT", 3));
        context.registerFailureAnalysis(noFailureResult(executionId));
        return context;
    }

    /** Mesma cadeia, terminando em ExecutionStatus.FAILED com FailureAnalysisResult ANALYZED. */
    public static AutoQaContext failedContext() {
        AutoQaContext context = AutoQaContext.create("Login válido", "/projeto");
        UUID executionId = context.getExecutionId();
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(knowledgeWithReuseCandidates("tests/support/loginPage.ts"));
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)));
        context.registerGeneration(generationWithReusedFiles(executionId, "tests/support/loginPage.ts"));
        context.registerCodeReview(approvedReview(executionId, "no-hardcoded-wait"));
        context.registerApplyApproval(new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true));
        context.registerApplyResult(completedApply(executionId, "tests/login.spec.ts"));
        context.registerExecutionApproval(new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false));
        context.registerExecutionResult(failedExecution(executionId, "PLAYWRIGHT", 3, 1, "login deve funcionar"));
        context.registerFailureAnalysis(analyzedResult(executionId, FailureCategory.ASSERTION_FAILURE, "login deve funcionar"));
        return context;
    }

    /** Cadeia terminando em ExecutionStatus.ERROR (operacional) — bloqueia aprendizado. */
    public static AutoQaContext operationalErrorContext() {
        AutoQaContext context = AutoQaContext.create("Login válido", "/projeto");
        UUID executionId = context.getExecutionId();
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(knowledgeWithReuseCandidates("tests/support/loginPage.ts"));
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)));
        context.registerGeneration(generationWithReusedFiles(executionId, "tests/support/loginPage.ts"));
        context.registerCodeReview(approvedReview(executionId, "no-hardcoded-wait"));
        context.registerApplyApproval(new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true));
        context.registerApplyResult(completedApply(executionId, "tests/login.spec.ts"));
        context.registerExecutionApproval(new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false));
        context.registerExecutionResult(operationalExecution(executionId, ExecutionStatus.ERROR));
        context.registerFailureAnalysis(blockedResult(executionId));
        return context;
    }
}
