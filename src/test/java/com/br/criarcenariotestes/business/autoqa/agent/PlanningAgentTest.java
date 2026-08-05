package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningTestData;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningTechnicalException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PlanningAgent - Testes Unitários")
class PlanningAgentTest {

    private PlanningService planningService;
    private PlanningAgent agent;

    @BeforeEach
    void setUp() {
        planningService = Mockito.mock(PlanningService.class);
        agent = new PlanningAgent(planningService);
    }

    @Test
    @DisplayName("Deve retornar nome 'planning'")
    void deveRetornarNome() {
        assertThat(agent.getName()).isEqualTo("planning");
    }

    @Test
    @DisplayName("Deve ter Order(30)")
    void deveTerOrder30() {
        Order order = PlanningAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(30);
    }

    @Test
    @DisplayName("Deve rejeitar context nulo")
    void deveRejeitarContextNulo() {
        assertThatThrownBy(() -> agent.execute(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar failure quando discovery está faltando")
    void deveRetornarFailureQuandoDiscoveryFaltando() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure quando scenario está faltando")
    void deveRetornarFailureQuandoScenarioFaltando() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(PlanningTestData.discovery());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure quando knowledge está faltando")
    void deveRetornarFailureQuandoKnowledgeFaltando() {
        AutoQaContext context = contextWithDiscoveryAndScenario();
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure para cenário INVALID")
    void deveRetornarFailureParaCenarioInvalido() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(PlanningTestData.discovery());
        context.registerScenarioAnalysis(PlanningTestData.invalidScenario());
        context.registerProjectKnowledge(PlanningTestData.completeKnowledge());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
        verify(planningService, never()).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve retornar failure para knowledge FAILED")
    void deveRetornarFailureParaKnowledgeFailed() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(PlanningTestData.discovery());
        context.registerScenarioAnalysis(PlanningTestData.validScenario());
        context.registerProjectKnowledge(PlanningTestData.failedKnowledge());
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
        verify(planningService, never()).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve retornar success quando planning bem sucedido")
    void deveRetornarSuccessQuandoPlanningBemSucedido() {
        AutoQaContext context = contextCompleto();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(planningService.plan(any(), any(), any())).thenReturn(planResult);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("Plano criado");
    }

    @Test
    @DisplayName("Deve registrar resultado no contexto")
    void deveRegistrarResultadoNoContexto() {
        AutoQaContext context = contextCompleto();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(planningService.plan(any(), any(), any())).thenReturn(planResult);

        agent.execute(context);

        assertThat(context.getTechnicalPlanResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isEqualTo(planResult);
    }

    @Test
    @DisplayName("Deve retornar failure quando PlanningTechnicalException")
    void deveRetornarFailureQuandoTechnicalException() {
        AutoQaContext context = contextCompleto();
        when(planningService.plan(any(), any(), any()))
            .thenThrow(new PlanningTechnicalException("falha técnica"));

        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Falha no planejamento técnico");
    }

    @Test
    @DisplayName("Deve retornar failure quando PlanningValidationException")
    void deveRetornarFailureQuandoValidationException() {
        AutoQaContext context = contextCompleto();
        when(planningService.plan(any(), any(), any()))
            .thenThrow(new PlanningValidationException("falha validação"));

        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve incluir status no summary")
    void deveIncluirStatusNoSummary() {
        AutoQaContext context = contextCompleto();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(planningService.plan(any(), any(), any())).thenReturn(planResult);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).contains("READY");
    }

    @Test
    @DisplayName("Deve incluir contagem de arquivos no summary")
    void deveIncluirContagemDeArquivosNoSummary() {
        AutoQaContext context = contextCompleto();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(planningService.plan(any(), any(), any())).thenReturn(planResult);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).contains("1 arquivos");
    }

    @Test
    @DisplayName("Deve contar apenas reuseDecisions com reuse=true")
    void deveContarApenasReuseDecisionsComReuseTrue() {
        AutoQaContext context = contextCompleto();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(planningService.plan(any(), any(), any())).thenReturn(planResult);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).contains("0 reutilizações");
    }

    // --- helpers ---

    private AutoQaContext contextWithDiscoveryAndScenario() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(PlanningTestData.discovery());
        context.registerScenarioAnalysis(PlanningTestData.validScenario());
        return context;
    }

    private AutoQaContext contextCompleto() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(PlanningTestData.discovery());
        context.registerScenarioAnalysis(PlanningTestData.validScenario());
        context.registerProjectKnowledge(PlanningTestData.completeKnowledge());
        return context;
    }
}
