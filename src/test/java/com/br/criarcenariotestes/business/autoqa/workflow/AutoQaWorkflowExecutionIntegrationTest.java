package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.ApplyAgent;
import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ExecuteAgent;
import com.br.criarcenariotestes.business.autoqa.agent.GenerationAgent;
import com.br.criarcenariotestes.business.autoqa.agent.PlanningAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectKnowledgeAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ReviewAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ScenarioAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.apply.FileApplicationService;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
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
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningTestData;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integração dos 8 agentes do Auto QA em ordem, incluindo o ExecuteAgent.
 * ApplyApproval e ExecutionApproval são portas de aprovação humana: cada uma
 * só pode ser registrada depois que a fase anterior (CodeReview e Apply,
 * respectivamente) já existe no contexto — por isso o fluxo real é executado
 * em três chamadas encadeadas de AutoQaWorkflowService sobre o mesmo
 * AutoQaContext (Discovery..Review, depois Apply, depois Execute), nunca
 * reiniciando um agente já registrado. AutoQaWorkflowService continua
 * genérico, sem conhecer nenhum agente concreto.
 */
@DisplayName("AutoQaWorkflowService - Integração Execute (Fase 9)")
class AutoQaWorkflowExecutionIntegrationTest {

    @Test
    @DisplayName("Deve executar discovery primeiro")
    void deveExecutarDiscoveryPrimeiro() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.discoveryService).discover(any());
        assertThat(context.getAgentExecutions().get(0).message()).isNotNull();
    }

    @Test
    @DisplayName("Deve executar analysis em segundo")
    void deveExecutarAnalysisSegundo() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.scenarioService).analyze(any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar knowledge em terceiro")
    void deveExecutarKnowledgeTerceiro() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.knowledgeService).collect(any(), any());
    }

    @Test
    @DisplayName("Deve executar planning em quarto")
    void deveExecutarPlanningQuarto() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.planningService).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar generation em quinto")
    void deveExecutarGenerationQuinto() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.generationService).generate(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar review em sexto")
    void deveExecutarReviewSexto() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);

        runFase1a6(context, mocks);

        verify(mocks.reviewService).review(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar apply em sétimo")
    void deveExecutarApplySetimo() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());

        runApply(context, mocks);

        verify(mocks.applyService).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar execute em oitavo")
    void deveExecutarExecuteOitavo() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);
        context.registerExecutionApproval(executionApproval());
        stubExecutePassed(mocks);

        runExecute(context, mocks);

        verify(mocks.executionService).execute(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve finalizar com oito agentes executados")
    void deveFinalizarComOitoAgentes() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getAgentExecutions()).hasSize(8);
    }

    @Test
    @DisplayName("Deve interromper antes do Execute quando Apply não completa (BLOCKED)")
    void deveInterromperExecuteQuandoApplyFalhar() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        when(mocks.applyService.apply(any(), any(), any(), any(), any(), any()))
                .thenReturn(applyResult(ApplyStatus.BLOCKED));
        runApply(context, mocks);

        context.registerExecutionApproval(executionApproval());
        runExecute(context, mocks);

        verify(mocks.executionService, never()).execute(any(), any(), any(), any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve interromper Execute quando ExecutionApproval está ausente")
    void deveInterromperExecuteQuandoApprovalAusente() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);

        runExecute(context, mocks);

        verify(mocks.executionService, never()).execute(any(), any(), any(), any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getExecutionResult()).isNull();
    }

    @Test
    @DisplayName("Deve interromper o workflow quando Execute retorna ERROR")
    void deveInterromperWorkflowQuandoExecutionError() {
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

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve permitir workflow FINISHED mesmo com testes FAILED (não é erro técnico)")
    void devePermitirWorkflowComTestesFailed() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);
        context.registerExecutionApproval(executionApproval());
        when(mocks.executionService.execute(any(), any(), any(), any(), any()))
                .thenReturn(executionResult(ExecutionStatus.FAILED, 1));

        runExecute(context, mocks);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getExecutionResult().status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    @DisplayName("Deve manter oito resultados no contexto ao final do fluxo feliz")
    void deveManterOitoResultadosNoContexto() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isNotNull();
        assertThat(context.getGenerationResult()).isNotNull();
        assertThat(context.getCodeReviewResult()).isNotNull();
        assertThat(context.getApplyResult()).isNotNull();
        assertThat(context.getExecutionResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar oito AgentExecutionResults, todos bem sucedidos")
    void deveRegistrarOitoAgentExecutionResults() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        runFluxoFelizCompleto(context, mocks);

        assertThat(context.getAgentExecutions()).hasSize(8);
        assertThat(context.getAgentExecutions()).allMatch(r -> r.success());
    }

    @Test
    @DisplayName("Deve respeitar a ordem dos oito agentes (0,10,20,30,40,50,60,70)")
    void deveRespeitarOrderDosOitoAgentes() {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
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
    }

    @Test
    @DisplayName("Não deve alterar arquivos do projeto original — toda a cadeia usa apenas serviços mockados")
    void deveNaoAlterarArquivos() {
        AutoQaContext context = context();
        Mocks mocks = mocks();
        String originalProjectPath = context.getProjectPath();

        runFluxoFelizCompleto(context, mocks);

        assertThat(context.getProjectPath()).isEqualTo(originalProjectPath);
        assertThat(context.getApplyResult().projectRootReference()).doesNotContain(originalProjectPath);
    }

    @Test
    @DisplayName("Não deve existir nenhum FailureAgent na base de código")
    void deveNaoIniciarFailureAgent() {
        assertThat(List.of(
                ProjectDiscoveryAgent.class.getSimpleName(), ScenarioAnalysisAgent.class.getSimpleName(),
                ProjectKnowledgeAgent.class.getSimpleName(), PlanningAgent.class.getSimpleName(),
                GenerationAgent.class.getSimpleName(), ReviewAgent.class.getSimpleName(),
                ApplyAgent.class.getSimpleName(), ExecuteAgent.class.getSimpleName()
        )).noneMatch(name -> name.contains("Failure"));

        assertThatThrownByClassNotFound("com.br.criarcenariotestes.business.autoqa.agent.FailureAgent");
    }

    private void assertThatThrownByClassNotFound(String className) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // --- helpers de orquestração em estágios ---

    private void runFase1a6(AutoQaContext context, Mocks mocks) {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
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

    private void runFluxoFelizCompleto(AutoQaContext context, Mocks mocks) {
        stubHappyPathAte6(mocks);
        runFase1a6(context, mocks);
        context.registerApplyApproval(applyApproval());
        stubApplyCompleted(mocks);
        runApply(context, mocks);
        context.registerExecutionApproval(executionApproval());
        stubExecutePassed(mocks);
        runExecute(context, mocks);
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

    private void stubExecutePassed(Mocks mocks) {
        when(mocks.executionService.execute(any(), any(), any(), any(), any()))
                .thenReturn(executionResult(ExecutionStatus.PASSED, 0));
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
            TestExecutionService executionService
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
                Mockito.mock(TestExecutionService.class)
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
