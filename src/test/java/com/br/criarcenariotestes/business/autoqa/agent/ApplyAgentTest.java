package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.apply.FileApplicationService;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyValidationException;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ApplyAgent - Testes Unitários")
class ApplyAgentTest {

    private FileApplicationService fileApplicationService;
    private ApplyAgent agent;

    @BeforeEach
    void setUp() {
        fileApplicationService = Mockito.mock(FileApplicationService.class);
        agent = new ApplyAgent(fileApplicationService);
    }

    @Test
    @DisplayName("Deve possuir nome 'apply'")
    void devePossuirNomeApply() {
        assertThat(agent.getName()).isEqualTo("apply");
    }

    @Test
    @DisplayName("Deve possuir Order(60)")
    void devePossuirOrderSessenta() {
        Order order = ApplyAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(60);
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        assertThatThrownBy(() -> agent.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir discovery")
    void deveExigirDiscovery() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThat(agent.execute(context).success()).isFalse();
        verify(fileApplicationService, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve exigir code review")
    void deveExigirCodeReview() {
        AutoQaContext context = contextoComGeneration();
        assertThat(agent.execute(context).success()).isFalse();
        verify(fileApplicationService, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve exigir ApplyApproval")
    void deveExigirApplyApproval() {
        AutoQaContext context = contextoComCodeReview();
        assertThat(agent.execute(context).success()).isFalse();
        verify(fileApplicationService, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar quando ReviewStatus é CHANGES_REQUIRED, sem chamar o service")
    void deveRejeitarReviewChangesRequired() {
        AutoQaContext context = contextoComGeneration();
        context.registerCodeReview(reviewComStatus(ReviewStatus.CHANGES_REQUIRED));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(fileApplicationService, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar GenerationStatus.FAILED sem chamar o service")
    void deveRejeitarGenerationFailed() {
        AutoQaContext context = contextoComPlano(onePlan());
        context.registerGeneration(generationComStatus(GenerationStatus.FAILED));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(fileApplicationService, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve chamar FileApplicationService quando pré-condições OK")
    void deveChamarFileApplicationService() {
        AutoQaContext context = contextoComAprovacao();
        when(fileApplicationService.apply(any(), any(), any(), any(), any(), any())).thenReturn(sampleResult(context.getExecutionId()));

        agent.execute(context);

        verify(fileApplicationService).apply(
                context.getExecutionId(), context.getProjectDiscoveryResult(), context.getTechnicalPlanResult(),
                context.getGenerationResult(), context.getCodeReviewResult(), context.getApplyApproval()
        );
    }

    @Test
    @DisplayName("Deve registrar ApplyResult no contexto")
    void deveRegistrarApplyResult() {
        AutoQaContext context = contextoComAprovacao();
        ApplyResult result = sampleResult(context.getExecutionId());
        when(fileApplicationService.apply(any(), any(), any(), any(), any(), any())).thenReturn(result);

        agent.execute(context);

        assertThat(context.getApplyResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve retornar sucesso com resumo contendo o status do ApplyResult")
    void deveRetornarResumoComStatus() {
        AutoQaContext context = contextoComAprovacao();
        when(fileApplicationService.apply(any(), any(), any(), any(), any(), any())).thenReturn(sampleResult(context.getExecutionId()));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("COMPLETED");
    }

    @Test
    @DisplayName("Deve retornar falha quando o service lançar ApplyValidationException")
    void deveRetornarFalhaQuandoServiceFalhar() {
        AutoQaContext context = contextoComAprovacao();
        when(fileApplicationService.apply(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ApplyValidationException("inválido"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).startsWith("Falha na aplicação de arquivos no projeto:")
                .as("a mensagem precisa carregar o motivo: sem ele o diagnóstico só existe no console do IntelliJ");
    }

    @Test
    @DisplayName("Não deve registrar resultado em caso de falha")
    void deveNaoRegistrarResultadoEmFalha() {
        AutoQaContext context = contextoComAprovacao();
        when(fileApplicationService.apply(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ApplyValidationException("inválido"));

        agent.execute(context);

        assertThat(context.getApplyResult()).isNull();
    }

    @Test
    @DisplayName("Não deve expor projectPath na mensagem")
    void deveNaoExporProjectPath() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto/sensivel/secreto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.message()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("Não deve chamar o service sem pré-condições atendidas")
    void deveNaoChamarServiceSemPreCondicoes() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        agent.execute(context);
        verifyNoInteractions(fileApplicationService);
    }

    // --- helpers ---

    private TechnicalPlanResult onePlan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private AutoQaContext contextoComPlano(TechnicalPlanResult plan) {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(plan);
        return context;
    }

    private AutoQaContext contextoComGeneration() {
        AutoQaContext context = contextoComPlano(onePlan());
        context.registerGeneration(generationComStatus(GenerationStatus.COMPLETED));
        return context;
    }

    private AutoQaContext contextoComCodeReview() {
        AutoQaContext context = contextoComGeneration();
        context.registerCodeReview(reviewComStatus(ReviewStatus.APPROVED));
        return context;
    }

    private AutoQaContext contextoComAprovacao() {
        AutoQaContext context = contextoComCodeReview();
        context.registerApplyApproval(sampleApproval());
        return context;
    }

    private GenerationResult generationComStatus(GenerationStatus status) {
        UUID executionId = UUID.randomUUID();
        var file = GenerationTestData.generatedFile("tests/login.spec.ts",
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT,
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.GENERATED, false);
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", status, GenerationConfidence.HIGH, status == GenerationStatus.COMPLETED);
    }

    private CodeReviewResult reviewComStatus(ReviewStatus status) {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                status, ReviewConfidence.HIGH, false,
                status == ReviewStatus.APPROVED || status == ReviewStatus.APPROVED_WITH_WARNINGS);
    }

    private ApplyApproval sampleApproval() {
        return new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE, ApplyOperation.UPDATE), true, true);
    }

    private ApplyResult sampleResult(UUID executionId) {
        return new ApplyResult(executionId, List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/" + executionId, ApplyStatus.COMPLETED, false, true);
    }
}
