package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationTechnicalException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GenerationAgent - Testes Unitários")
class GenerationAgentTest {

    private GenerationService generationService;
    private GenerationAgent agent;

    @BeforeEach
    void setUp() {
        generationService = Mockito.mock(GenerationService.class);
        agent = new GenerationAgent(generationService);
    }

    @Test
    @DisplayName("Deve possuir nome 'generation'")
    void devePossuirNomeGeneration() {
        assertThat(agent.getName()).isEqualTo("generation");
    }

    @Test
    @DisplayName("Deve possuir Order(40)")
    void devePossuirOrderQuarenta() {
        Order order = GenerationAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(40);
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
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve exigir scenario")
    void deveExigirScenario() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve exigir knowledge")
    void deveExigirKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve exigir planning")
    void deveExigirPlanning() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar scenario INVALID sem chamar service")
    void deveRejeitarScenarioInvalid() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.invalidScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(onePlan());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar knowledge FAILED sem chamar service")
    void deveRejeitarKnowledgeFailed() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.failedKnowledge());
        context.registerTechnicalPlan(onePlan());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar plan BLOCKED sem chamar service")
    void deveRejeitarPlanBlocked() {
        AutoQaContext context = contextoCompletoComPlano(GenerationTestData.blockedPlan());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar plan INVALID sem chamar service")
    void deveRejeitarPlanInvalid() {
        AutoQaContext context = contextoCompletoComPlano(GenerationTestData.invalidPlan());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar framework UNKNOWN sem chamar service")
    void deveRejeitarFrameworkUnknown() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.unknownDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(onePlan());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve chamar GenerationService quando pré-condições OK")
    void deveChamarGenerationService() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any())).thenReturn(sampleResult());

        agent.execute(context);

        verify(generationService).generate(
                context.getExecutionId(), context.getProjectDiscoveryResult(), context.getScenarioAnalysisResult(),
                context.getProjectKnowledgeResult(), context.getTechnicalPlanResult()
        );
    }

    @Test
    @DisplayName("Deve registrar GenerationResult no contexto")
    void deveRegistrarGenerationResult() {
        AutoQaContext context = contextoCompleto();
        GenerationResult generationResult = sampleResult();
        when(generationService.generate(any(), any(), any(), any(), any())).thenReturn(generationResult);

        agent.execute(context);

        assertThat(context.getGenerationResult()).isEqualTo(generationResult);
    }

    @Test
    @DisplayName("Deve retornar resumo técnico com status e contagens")
    void deveRetornarResumoTecnico() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any())).thenReturn(sampleResult());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("COMPLETED");
        assertThat(result.message()).contains("1 arquivos");
    }

    @Test
    @DisplayName("Deve retornar falha quando service lançar GenerationTechnicalException")
    void deveRetornarFalhaQuandoServiceFalhar() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any()))
                .thenThrow(new GenerationTechnicalException("falha técnica"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Falha na geração de automação");
    }

    @Test
    @DisplayName("Deve retornar falha quando service lançar GenerationValidationException")
    void deveRetornarFalhaQuandoValidationException() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any()))
                .thenThrow(new GenerationValidationException("inválido"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Não deve registrar resultado em caso de falha")
    void deveNaoRegistrarResultadoEmFalha() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any()))
                .thenThrow(new GenerationTechnicalException("falha técnica"));

        agent.execute(context);

        assertThat(context.getGenerationResult()).isNull();
    }

    @Test
    @DisplayName("Não deve expor projectPath na mensagem de falha")
    void deveNaoExporProjectPath() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto/sensivel/secreto");
        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("Não deve expor código gerado na mensagem de sucesso")
    void deveNaoExporCodigoNaMensagem() {
        AutoQaContext context = contextoCompleto();
        when(generationService.generate(any(), any(), any(), any(), any())).thenReturn(sampleResult());

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).doesNotContain("import");
        assertThat(result.message()).doesNotContain("{");
    }

    @Test
    @DisplayName("Não deve chamar IA (service) sem pré-condições atendidas")
    void deveNaoChamarIaSemPreCondicoes() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        agent.execute(context);

        verifyNoInteractions(generationService);
    }

    // --- helpers ---

    private com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult onePlan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private AutoQaContext contextoCompletoComPlano(com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult plan) {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(plan);
        return context;
    }

    private AutoQaContext contextoCompleto() {
        return contextoCompletoComPlano(onePlan());
    }

    private GenerationResult sampleResult() {
        GeneratedFile file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT, GeneratedFileStatus.GENERATED, false);
        return new GenerationResult(
                java.util.UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                java.util.List.of(file), java.util.List.of(), java.util.List.of(),
                "root", "manifest.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );
    }
}
