package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ScenarioAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisTechnicalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaWorkflowService - Integração ScenarioAnalysis")
class AutoQaWorkflowScenarioAnalysisIntegrationTest {

    @Test
    @DisplayName("Deve executar discovery antes da scenario analysis")
    void deveExecutarDiscoveryAntesDaScenarioAnalysis() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        ProjectDiscoveryAgent discoveryAgent = new ProjectDiscoveryAgent(discoveryService);
        ScenarioAnalysisAgent scenarioAgent = new ScenarioAnalysisAgent(scenarioService);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());

        List<AutoQaAgent> agents = new ArrayList<>(List.of(scenarioAgent, discoveryAgent));
        AnnotationAwareOrderComparator.sort(agents);

        new AutoQaWorkflowService(agents).execute(context);

        verify(discoveryService).discover(Path.of("/projeto"));
        verify(scenarioService).analyze("Cenário", discovery());
    }

    @Test
    @DisplayName("Deve finalizar workflow com descoberta e análise")
    void deveFinalizarWorkflowComDescobertaEAnalise() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());

        new AutoQaWorkflowService(List.of(
                new ProjectDiscoveryAgent(discoveryService),
                new ScenarioAnalysisAgent(scenarioService)
        )).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    @Test
    @DisplayName("Deve interromper antes da análise quando discovery falhar")
    void deveInterromperAntesDaAnaliseQuandoDiscoveryFalhar() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenThrow(new IllegalStateException("falha"));

        new AutoQaWorkflowService(List.of(
                new ProjectDiscoveryAgent(discoveryService),
                new ScenarioAnalysisAgent(scenarioService)
        )).execute(context);

        verify(scenarioService, never()).analyze(any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve interromper quando scenario analysis falhar")
    void deveInterromperQuandoScenarioAnalysisFalhar() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenThrow(new ScenarioAnalysisTechnicalException("falha"));

        new AutoQaWorkflowService(List.of(
                new ProjectDiscoveryAgent(discoveryService),
                new ScenarioAnalysisAgent(scenarioService)
        )).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve manter discovery e analysis no contexto final")
    void deveManterDiscoveryEAnalysisNoContextoFinal() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());

        new AutoQaWorkflowService(List.of(
                new ProjectDiscoveryAgent(discoveryService),
                new ScenarioAnalysisAgent(scenarioService)
        )).execute(context);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar dois AgentExecutionResults")
    void deveRegistrarDoisAgentExecutionResults() {
        ProjectDiscoveryService discoveryService = Mockito.mock(ProjectDiscoveryService.class);
        ScenarioAnalysisService scenarioService = Mockito.mock(ScenarioAnalysisService.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        when(discoveryService.discover(Path.of("/projeto"))).thenReturn(discovery());
        when(scenarioService.analyze("Cenário", discovery())).thenReturn(analysis());

        new AutoQaWorkflowService(List.of(
                new ProjectDiscoveryAgent(discoveryService),
                new ScenarioAnalysisAgent(scenarioService)
        )).execute(context);

        assertThat(context.getAgentExecutions()).hasSize(2);
    }

    @Test
    @DisplayName("Deve respeitar ordem dos agentes")
    void deveRespeitarOrderDosAgentes() {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
                new ScenarioAnalysisAgent(Mockito.mock(ScenarioAnalysisService.class)),
                new ProjectDiscoveryAgent(Mockito.mock(ProjectDiscoveryService.class))
        ));
        AnnotationAwareOrderComparator.sort(agents);

        assertThat(agents.getFirst()).isInstanceOf(ProjectDiscoveryAgent.class);
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(AutomationFramework.PLAYWRIGHT),
                List.of("PLAYWRIGHT"),
                "playwright.config.ts",
                List.of("playwright.config.ts"),
                List.of(),
                DiscoveryConfidence.HIGH,
                true
        );
    }

    private ScenarioAnalysisResult analysis() {
        return new ScenarioAnalysisResult(
                "Login válido",
                "Validar acesso",
                List.of("Usuário cadastrado"),
                List.of(new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", List.of())),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Usuário"),
                List.of(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                List.of(),
                true
        );
    }
}
