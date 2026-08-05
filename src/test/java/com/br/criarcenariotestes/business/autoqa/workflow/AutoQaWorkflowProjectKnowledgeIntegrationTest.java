package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectKnowledgeAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ScenarioAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeValidationException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaWorkflowService - Integração ProjectKnowledge")
class AutoQaWorkflowProjectKnowledgeIntegrationTest {

    @Test
    @DisplayName("Deve executar discovery antes de analysis")
    void deveExecutarDiscoveryAntesDeAnalysis() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenReturn(knowledge());

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        verify(discoveryService).discover(Path.of("/tmp/project"));
        verify(scenarioService).analyze("Cenário", discovery());
        verify(knowledgeService).collect(discovery(), analysis());
    }

    @Test
    @DisplayName("Deve executar analysis antes de knowledge")
    void deveExecutarAnalysisAntesDeKnowledge() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenReturn(knowledge());

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        verify(knowledgeService).collect(discovery(), analysis());
    }

    @Test
    @DisplayName("Deve finalizar com três agentes")
    void deveFinalizarComTresAgentes() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenReturn(knowledge());

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    @Test
    @DisplayName("Deve interromper knowledge quando discovery falhar")
    void deveInterromperKnowledgeQuandoDiscoveryFalhar() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenThrow(new IllegalStateException("falha"));

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        verify(scenarioService, never()).analyze(any(), any());
        verify(knowledgeService, never()).collect(any(), any());
    }

    @Test
    @DisplayName("Deve interromper knowledge quando analysis falhar")
    void deveInterromperKnowledgeQuandoAnalysisFalhar() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenThrow(new com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisTechnicalException("falha"));

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        verify(knowledgeService, never()).collect(any(), any());
    }

    @Test
    @DisplayName("Deve interromper workflow quando knowledge falhar")
    void deveInterromperWorkflowQuandoKnowledgeFalhar() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenThrow(new ProjectKnowledgeValidationException("falha"));

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve manter três resultados no contexto")
    void deveManterTresResultadosNoContexto() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenReturn(knowledge());

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar três AgentExecutionResults")
    void deveRegistrarTresAgentExecutionResults() {
        AutoQaContext context = context();
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService = Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class);
        ProjectKnowledgeService knowledgeService = Mockito.mock(ProjectKnowledgeService.class);
        when(discoveryService.discover(Path.of("/tmp/project"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());
        when(knowledgeService.collect(discovery(), analysis())).thenReturn(knowledge());

        new AutoQaWorkflowService(orderedAgents(discoveryService, scenarioService, knowledgeService)).execute(context);

        assertThat(context.getAgentExecutions()).hasSize(3);
    }

    @Test
    @DisplayName("Deve respeitar order dos três agentes")
    void deveRespeitarOrderDosTresAgentes() {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
                new ProjectKnowledgeAgent(Mockito.mock(ProjectKnowledgeService.class)),
                new ScenarioAnalysisAgent(Mockito.mock(com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService.class)),
                new ProjectDiscoveryAgent(Mockito.mock(ProjectDiscoveryService.class))
        ));
        AnnotationAwareOrderComparator.sort(agents);

        assertThat(agents.get(0)).isInstanceOf(ProjectDiscoveryAgent.class);
        assertThat(agents.get(1)).isInstanceOf(ScenarioAnalysisAgent.class);
        assertThat(agents.get(2)).isInstanceOf(ProjectKnowledgeAgent.class);
    }

    private List<AutoQaAgent> orderedAgents(ProjectDiscoveryService discoveryService,
                                            com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService scenarioService,
                                            ProjectKnowledgeService knowledgeService) {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
                new ProjectKnowledgeAgent(knowledgeService),
                new ScenarioAnalysisAgent(scenarioService),
                new ProjectDiscoveryAgent(discoveryService)
        ));
        AnnotationAwareOrderComparator.sort(agents);
        return agents;
    }

    private AutoQaContext context() {
        return AutoQaContext.create("Cenário", "/tmp/project");
    }

    private ProjectDiscoveryResult discovery() {
        return KnowledgeTestData.discovery(Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT);
    }

    private ScenarioAnalysisResult analysis() {
        return KnowledgeTestData.analysis();
    }

    private ProjectKnowledgeResult knowledge() {
        return KnowledgeTestData.knowledge(Path.of("/tmp/project"), KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.PAGE_OBJECT, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT));
    }
}
