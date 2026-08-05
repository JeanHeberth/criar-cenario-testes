package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.*;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningTestData;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningTechnicalException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AutoQaWorkflowService - Integração Planning")
class AutoQaWorkflowPlanningIntegrationTest {

    @Test
    @DisplayName("Deve executar planning após knowledge")
    void deveExecutarPlanningAposKnowledge() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve finalizar com quatro agentes")
    void deveFinalizarComQuatroAgentes() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getAgentExecutions()).hasSize(4);
    }

    @Test
    @DisplayName("Deve registrar technical plan no contexto")
    void deveRegistrarTechnicalPlanNoContexto() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getTechnicalPlanResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve não executar planning quando knowledge falhar")
    void naoDeveExecutarPlanningQuandoKnowledgeFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any()))
            .thenThrow(new com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeValidationException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService, never()).plan(any(), any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve não executar planning quando scenario falhar")
    void naoDeveExecutarPlanningQuandoScenarioFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any()))
            .thenThrow(new com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisTechnicalException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService, never()).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve não executar planning quando discovery falhar")
    void naoDeveExecutarPlanningQuandoDiscoveryFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenThrow(new IllegalStateException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService, never()).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve ter planning com Order 30 maior que knowledge com Order 20")
    void deveTerPlanningComOrdemMaiorQueKnowledge() {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
            new PlanningAgent(Mockito.mock(PlanningService.class)),
            new ProjectKnowledgeAgent(Mockito.mock(ProjectKnowledgeService.class)),
            new com.br.criarcenariotestes.business.autoqa.agent.ScenarioAnalysisAgent(Mockito.mock(ScenarioAnalysisService.class)),
            new ProjectDiscoveryAgent(Mockito.mock(ProjectDiscoveryService.class))
        ));
        AnnotationAwareOrderComparator.sort(agents);

        assertThat(agents.get(0)).isInstanceOf(ProjectDiscoveryAgent.class);
        assertThat(agents.get(1)).isInstanceOf(ScenarioAnalysisAgent.class);
        assertThat(agents.get(2)).isInstanceOf(ProjectKnowledgeAgent.class);
        assertThat(agents.get(3)).isInstanceOf(PlanningAgent.class);
    }

    @Test
    @DisplayName("Deve retornar ERROR quando planning retorna failure por ValidationException")
    void deveContinuarQuandoPlanningRetornaFailure() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any()))
            .thenThrow(new PlanningValidationException("plano inválido"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        // PlanningAgent catches exception and returns failure; workflow stops with ERROR
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve registrar quatro AgentExecutionResults quando todos bem sucedidos")
    void deveRegistrarQuatroAgentExecutionResults() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getAgentExecutions()).hasSize(4);
        assertThat(context.getAgentExecutions()).allMatch(r -> r.success());
    }

    @Test
    @DisplayName("Deve passar discovery ao planningService")
    void devePassarDiscoveryAoPlanningService() {
        AutoQaContext context = context();
        var mocks = mocks();
        ProjectDiscoveryResult d = discovery();
        when(mocks.discoveryService.discover(any())).thenReturn(d);
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService).plan(eq(d), any(), any());
    }

    @Test
    @DisplayName("Deve retornar ERROR quando planning lança TechnicalException")
    void deveRetornarErrorQuandoPlanningLancaRuntimeException() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        // PlanningAgent catches PlanningException, returns failure; workflow stops with ERROR
        when(mocks.planningService.plan(any(), any(), any()))
            .thenThrow(new PlanningTechnicalException("falha técnica"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve manter quatro resultados no contexto quando tudo ok")
    void deveManterQuatroResultadosNoContexto() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(PlanningTestData.readyPlan());

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isNotNull();
    }

    // --- helpers ---

    private record Mocks(
        ProjectDiscoveryService discoveryService,
        ScenarioAnalysisService scenarioService,
        ProjectKnowledgeService knowledgeService,
        PlanningService planningService
    ) {}

    private Mocks mocks() {
        return new Mocks(
            Mockito.mock(ProjectDiscoveryService.class),
            Mockito.mock(ScenarioAnalysisService.class),
            Mockito.mock(ProjectKnowledgeService.class),
            Mockito.mock(PlanningService.class)
        );
    }

    private List<AutoQaAgent> orderedAgents(Mocks mocks) {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
            new PlanningAgent(mocks.planningService),
            new ProjectKnowledgeAgent(mocks.knowledgeService),
            new ScenarioAnalysisAgent(mocks.scenarioService),
            new ProjectDiscoveryAgent(mocks.discoveryService)
        ));
        AnnotationAwareOrderComparator.sort(agents);
        return agents;
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
}
