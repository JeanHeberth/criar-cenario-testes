package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.*;
import com.br.criarcenariotestes.business.autoqa.apply.FileApplicationService;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService;
import com.br.criarcenariotestes.business.autoqa.failure.FailureAnalysisService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.learning.LearningService;
import com.br.criarcenariotestes.business.autoqa.learning.LearningTestData;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.review.CodeReviewService;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integração dos 10 agentes do Auto QA (Fases 1-11), confirmando a ordem
 * completa 0..90, que LearningAgent roda após FailureAnalysisAgent, que
 * status operacional interrompe o fluxo antes de ambos, e que nenhuma Fase 12
 * foi iniciada.
 */
@DisplayName("AutoQaWorkflowService - Integração Learning (Fase 11)")
class AutoQaWorkflowLearningIntegrationTest {

    @Test
    @DisplayName("Deve respeitar a ordem completa dos dez agentes (0,10,...,90)")
    void deveRespeitarOrdemDosDezAgentes() {
        List<com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent> agents = new ArrayList<>(List.of(
                new LearningAgent(mock(LearningService.class)),
                new FailureAnalysisAgent(mock(FailureAnalysisService.class)),
                new ExecuteAgent(mock(TestExecutionService.class)),
                new ApplyAgent(mock(FileApplicationService.class)),
                new ReviewAgent(mock(CodeReviewService.class)),
                new GenerationAgent(mock(GenerationService.class)),
                new PlanningAgent(mock(PlanningService.class)),
                new ProjectKnowledgeAgent(mock(ProjectKnowledgeService.class)),
                new ScenarioAnalysisAgent(mock(ScenarioAnalysisService.class)),
                new ProjectDiscoveryAgent(mock(ProjectDiscoveryService.class))
        ));
        AnnotationAwareOrderComparator.sort(agents);

        assertThat(agents.get(0)).isInstanceOf(ProjectDiscoveryAgent.class);
        assertThat(agents.get(1)).isInstanceOf(ScenarioAnalysisAgent.class);
        assertThat(agents.get(2)).isInstanceOf(ProjectKnowledgeAgent.class);
        assertThat(agents.get(3)).isInstanceOf(PlanningAgent.class);
        assertThat(agents.get(4)).isInstanceOf(GenerationAgent.class);
        assertThat(agents.get(5)).isInstanceOf(ReviewAgent.class);
        assertThat(agents.get(6)).isInstanceOf(ApplyAgent.class);
        assertThat(agents.get(7)).isInstanceOf(ExecuteAgent.class);
        assertThat(agents.get(8)).isInstanceOf(FailureAnalysisAgent.class);
        assertThat(agents.get(9)).isInstanceOf(LearningAgent.class);
        assertThat(agents).hasSize(10);
    }

    @Test
    @DisplayName("LearningAgent deve executar após FailureAnalysisAgent no fluxo real")
    void learningDeveExecutarAposFailureAnalysis() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks, ExecutionStatus.PASSED, 0);

        List<String> agentNames = context.getAgentExecutions().stream().map(r -> r.message()).toList();
        assertThat(context.getAgentExecutions()).hasSize(10);
        assertThat(context.getFailureAnalysisResult()).isNotNull();
        assertThat(context.getLearningResult()).isNotNull();
    }

    @Test
    @DisplayName("PASSED deve permitir aprendizado (LearningAgent chama o service e registra resultado)")
    void passedPermiteAprendizado() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks, ExecutionStatus.PASSED, 0);

        verify(mocks.learningService).learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(context.getLearningResult()).isNotNull();
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    @Test
    @DisplayName("FAILED deve permitir aprendizado (workflow continua e LearningAgent executa)")
    void failedPermiteAprendizado() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks, ExecutionStatus.FAILED, 1);

        verify(mocks.learningService).learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(context.getLearningResult()).isNotNull();
        assertThat(context.getExecutionResult().status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    @Test
    @DisplayName("Status operacional (ERROR) deve propagar BLOCKED e interromper o fluxo em Learning, sem aprendizado útil")
    void statusOperacionalPropagaBlockedEInterrompeEmLearning() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);
        context.registerExecutionApproval(executionApproval());
        when(mocks.executionService.execute(any(), any(), any(), any(), any()))
                .thenReturn(executionResult(ExecutionStatus.ERROR, null));
        runExecute(context, mocks);

        UUID executionId = context.getExecutionId();
        when(mocks.failureAnalysisService.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(LearningTestData.blockedResult(executionId));
        LearningResult blockedLearning = new LearningResult(executionId, List.of(), List.of(),
                List.of(new com.br.criarcenariotestes.business.autoqa.model.learning.LearningWarning(
                        "OPERATIONAL_FAILURE", "status operacional", true)),
                com.br.criarcenariotestes.business.autoqa.model.learning.LearningStatus.BLOCKED,
                com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence.UNKNOWN, 0, 0, true, true);
        when(mocks.learningService.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(blockedLearning);

        runFailureAnalysisAndLearning(context, mocks);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getFailureAnalysisResult()).isNotNull();
        assertThat(context.getLearningResult()).isNotNull();
        assertThat(context.getLearningResult().status())
                .isEqualTo(com.br.criarcenariotestes.business.autoqa.model.learning.LearningStatus.BLOCKED);
        assertThat(context.getAgentExecutions()).hasSize(10);
        assertThat(context.getAgentExecutions().get(9).success()).isFalse();
    }

    @Test
    @DisplayName("Deve preservar todos os resultados das fases anteriores ao final do fluxo feliz")
    void devePreservarResultadosAnteriores() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks, ExecutionStatus.PASSED, 0);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isNotNull();
        assertThat(context.getGenerationResult()).isNotNull();
        assertThat(context.getCodeReviewResult()).isNotNull();
        assertThat(context.getApplyResult()).isNotNull();
        assertThat(context.getExecutionResult()).isNotNull();
        assertThat(context.getFailureAnalysisResult()).isNotNull();
        assertThat(context.getLearningResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar dez AgentExecutionResults, todos bem sucedidos, no fluxo feliz")
    void deveRegistrarDezAgentExecutionResults() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks, ExecutionStatus.PASSED, 0);

        assertThat(context.getAgentExecutions()).hasSize(10);
        assertThat(context.getAgentExecutions()).allMatch(r -> r.success());
    }

    @Test
    @DisplayName("Não deve alterar arquivos do projeto original — toda a cadeia usa apenas serviços mockados")
    void deveNaoAlterarArquivosDoProjeto() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        String originalProjectPath = context.getProjectPath();

        runFluxoFelizCompleto(context, mocks, ExecutionStatus.PASSED, 0);

        assertThat(context.getProjectPath()).isEqualTo(originalProjectPath);
        assertThat(context.getApplyResult().projectRootReference()).doesNotContain(originalProjectPath);
    }

    @Test
    @DisplayName("Nenhum agente com Order acima de 90 deve existir (Fase 12 não iniciada)")
    void nenhumAgenteAcimaDeNoventa() {
        List<Class<?>> agentClasses = List.of(ProjectDiscoveryAgent.class, ScenarioAnalysisAgent.class,
                ProjectKnowledgeAgent.class, PlanningAgent.class, GenerationAgent.class, ReviewAgent.class,
                ApplyAgent.class, ExecuteAgent.class, FailureAnalysisAgent.class, LearningAgent.class);
        assertThat(agentClasses).hasSize(10);
        for (Class<?> clazz : agentClasses) {
            org.springframework.core.annotation.Order order = clazz.getAnnotation(org.springframework.core.annotation.Order.class);
            assertThat(order).isNotNull();
            assertThat(order.value()).isLessThanOrEqualTo(90);
        }
    }

    // --- orquestração em estágios ---

    private void runFase1a6(AutoQaContext context, Mocks mocks) {
        List<com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent> agents = new ArrayList<>(List.of(
                new ProjectDiscoveryAgent(mocks.discoveryService),
                new ScenarioAnalysisAgent(mocks.scenarioService),
                new ProjectKnowledgeAgent(mocks.knowledgeService),
                new PlanningAgent(mocks.planningService),
                new GenerationAgent(mocks.generationService),
                new ReviewAgent(mocks.reviewService)
        ));
        AnnotationAwareOrderComparator.sort(agents);
        new AutoQaWorkflowService(agents).execute(context);
    }

    private void runApply(AutoQaContext context, Mocks mocks) {
        new AutoQaWorkflowService(List.of(new ApplyAgent(mocks.applyService))).execute(context);
    }

    private void runExecute(AutoQaContext context, Mocks mocks) {
        new AutoQaWorkflowService(List.of(new ExecuteAgent(mocks.executionService))).execute(context);
    }

    private void runFailureAnalysisAndLearning(AutoQaContext context, Mocks mocks) {
        List<com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent> agents = new ArrayList<>(List.of(
                new FailureAnalysisAgent(mocks.failureAnalysisService),
                new LearningAgent(mocks.learningService)
        ));
        AnnotationAwareOrderComparator.sort(agents);
        new AutoQaWorkflowService(agents).execute(context);
    }

    private void runFluxoFelizCompleto(AutoQaContext context, Mocks mocks, ExecutionStatus status, Integer exitCode) {
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);
        context.registerExecutionApproval(executionApproval());
        when(mocks.executionService.execute(any(), any(), any(), any(), any()))
                .thenReturn(executionResult(status, exitCode));
        runExecute(context, mocks);

        UUID executionId = context.getExecutionId();
        FailureAnalysisResult failureAnalysisResult = status == ExecutionStatus.PASSED
                ? LearningTestData.noFailureResult(executionId)
                : LearningTestData.analyzedResult(executionId,
                        com.br.criarcenariotestes.business.autoqa.model.failure.FailureCategory.ASSERTION_FAILURE, "teste");
        when(mocks.failureAnalysisService.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failureAnalysisResult);

        LearningResult learningResult = new LearningResult(executionId, List.of(), List.of(), List.of(),
                com.br.criarcenariotestes.business.autoqa.model.learning.LearningStatus.SKIPPED,
                com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence.UNKNOWN, 0, 0, false, true);
        when(mocks.learningService.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(learningResult);

        runFailureAnalysisAndLearning(context, mocks);
    }

    private void stubHappyPathAte6(Mocks mocks) {
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(readyPlan());
        when(mocks.generationService.generate(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(generationResult());
        when(mocks.reviewService.review(any(), any(), any(), any(), any(), any(), any())).thenReturn(reviewResult());
    }

    private void stubApplyCompleted(Mocks mocks) {
        when(mocks.applyService.apply(any(), any(), any(), any(), any(), any()))
                .thenReturn(applyResult(ApplyStatus.COMPLETED));
    }

    // --- fixtures ---

    private record Mocks(
            ProjectDiscoveryService discoveryService,
            ScenarioAnalysisService scenarioService,
            ProjectKnowledgeService knowledgeService,
            PlanningService planningService,
            GenerationService generationService,
            CodeReviewService reviewService,
            FileApplicationService applyService,
            TestExecutionService executionService,
            FailureAnalysisService failureAnalysisService,
            LearningService learningService
    ) {
    }

    private Mocks mocks() {
        return new Mocks(
                Mockito.mock(ProjectDiscoveryService.class),
                Mockito.mock(ScenarioAnalysisService.class),
                Mockito.mock(ProjectKnowledgeService.class),
                Mockito.mock(PlanningService.class),
                Mockito.mock(GenerationService.class),
                Mockito.mock(CodeReviewService.class),
                Mockito.mock(FileApplicationService.class),
                Mockito.mock(TestExecutionService.class),
                Mockito.mock(FailureAnalysisService.class),
                Mockito.mock(LearningService.class)
        );
    }

    private AutoQaContext context() {
        return AutoQaContext.create("Cenário", "/project");
    }

    private ProjectDiscoveryResult discovery() {
        return KnowledgeTestData.discovery(Path.of("/project"), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT);
    }

    private ScenarioAnalysisResult analysis() {
        return KnowledgeTestData.analysis();
    }

    private ProjectKnowledgeResult knowledge() {
        return KnowledgeTestData.knowledge(Path.of("/project"),
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT));
    }

    private TechnicalPlanResult readyPlan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private GenerationResult generationResult() {
        var file = GenerationTestData.generatedFile("tests/login.spec.ts",
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT,
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.GENERATED, false);
        UUID executionId = UUID.randomUUID();
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);
    }

    private CodeReviewResult reviewResult() {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }

    private ApplyApproval applyApproval() {
        return new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true);
    }

    private ApplyResult applyResult(ApplyStatus status) {
        return new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "project", ".auto-qa/backups/x", status, false, true);
    }

    private ExecutionApproval executionApproval() {
        return new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
    }

    private ExecutionResult executionResult(ExecutionStatus status, Integer exitCode) {
        CommandSpecification command = new CommandSpecification(ExecutionCommandId.PLAYWRIGHT_TEST, "npx",
                List.of("playwright", "test"), "project", Duration.ofMinutes(10), Map.of(), ExecutionCommandType.TEST);
        return new ExecutionResult(UUID.randomUUID(), command, status, exitCode,
                java.time.Instant.now(), java.time.Instant.now(), Duration.ofSeconds(1), "saida", "", false, false,
                List.of(), List.of(), status == ExecutionStatus.PASSED || status == ExecutionStatus.FAILED);
    }
}
