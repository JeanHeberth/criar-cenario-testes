package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.review.CodeReviewService;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewTechnicalException;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ReviewAgent - Testes Unitários")
class ReviewAgentTest {

    private CodeReviewService codeReviewService;
    private ReviewAgent agent;

    @BeforeEach
    void setUp() {
        codeReviewService = Mockito.mock(CodeReviewService.class);
        agent = new ReviewAgent(codeReviewService);
    }

    @Test
    @DisplayName("Deve possuir nome 'review'")
    void devePossuirNomeReview() {
        assertThat(agent.getName()).isEqualTo("review");
    }

    @Test
    @DisplayName("Deve possuir Order(50)")
    void devePossuirOrderCinquenta() {
        Order order = ReviewAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(50);
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
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
        verify(codeReviewService, never()).review(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve exigir scenario")
    void deveExigirScenario() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve exigir knowledge")
    void deveExigirKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve exigir planning")
    void deveExigirPlanning() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve exigir generation")
    void deveExigirGeneration() {
        AutoQaContext context = contextoComPlano(onePlan());
        assertThat(agent.execute(context).success()).isFalse();
        verify(codeReviewService, never()).review(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar GenerationStatus.PARTIAL sem chamar service")
    void deveRejeitarGenerationPartial() {
        AutoQaContext context = contextoComPlano(onePlan());
        context.registerGeneration(generationComStatus(GenerationStatus.PARTIAL));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(codeReviewService, never()).review(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar GenerationStatus.FAILED sem chamar service")
    void deveRejeitarGenerationFailed() {
        AutoQaContext context = contextoComPlano(onePlan());
        context.registerGeneration(generationComStatus(GenerationStatus.FAILED));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(codeReviewService, never()).review(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve chamar CodeReviewService quando pré-condições OK")
    void deveChamarCodeReviewService() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any())).thenReturn(sampleResult());

        agent.execute(context);

        verify(codeReviewService).review(
                context.getExecutionId(), context.getProjectDiscoveryResult(), context.getScenarioAnalysisResult(),
                context.getProjectKnowledgeResult(), context.getTechnicalPlanResult(), context.getGenerationResult()
        );
    }

    @Test
    @DisplayName("Deve registrar CodeReviewResult no contexto")
    void deveRegistrarCodeReview() {
        AutoQaContext context = contextoCompleto();
        CodeReviewResult reviewResult = sampleResult();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any())).thenReturn(reviewResult);

        agent.execute(context);

        assertThat(context.getCodeReviewResult()).isEqualTo(reviewResult);
    }

    @Test
    @DisplayName("Deve retornar resumo técnico com status e contagens")
    void deveRetornarResumoTecnico() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any())).thenReturn(sampleResult());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("APPROVED");
    }

    @Test
    @DisplayName("Deve retornar falha quando service lançar CodeReviewTechnicalException")
    void deveRetornarFalhaQuandoServiceFalhar() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CodeReviewTechnicalException("falha técnica"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Falha na revisão de código gerado");
    }

    @Test
    @DisplayName("Deve retornar falha quando service lançar CodeReviewValidationException")
    void deveRetornarFalhaQuandoValidationException() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CodeReviewValidationException("inválido"));

        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Não deve registrar resultado em caso de falha")
    void deveNaoRegistrarResultadoEmFalha() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CodeReviewTechnicalException("falha técnica"));

        agent.execute(context);

        assertThat(context.getCodeReviewResult()).isNull();
    }

    @Test
    @DisplayName("Não deve expor projectPath na mensagem")
    void deveNaoExporProjectPath() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto/sensivel/secreto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.message()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("Não deve expor código na mensagem de sucesso")
    void deveNaoExporCodigo() {
        AutoQaContext context = contextoCompleto();
        when(codeReviewService.review(any(), any(), any(), any(), any(), any())).thenReturn(sampleResult());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).doesNotContain("import").doesNotContain("{");
    }

    @Test
    @DisplayName("Não deve chamar IA (service) sem pré-condições atendidas")
    void deveNaoChamarIaSemPreCondicoes() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        agent.execute(context);
        verifyNoInteractions(codeReviewService);
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

    private AutoQaContext contextoCompleto() {
        AutoQaContext context = contextoComPlano(onePlan());
        context.registerGeneration(generationComStatus(GenerationStatus.COMPLETED));
        return context;
    }

    private GenerationResult generationComStatus(GenerationStatus status) {
        UUID executionId = UUID.randomUUID();
        GeneratedFile file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT, GeneratedFileStatus.GENERATED, false);
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", status, GenerationConfidence.HIGH, status == GenerationStatus.COMPLETED);
    }

    private CodeReviewResult sampleResult() {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }
}
