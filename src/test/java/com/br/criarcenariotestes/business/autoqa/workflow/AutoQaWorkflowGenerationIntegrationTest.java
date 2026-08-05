package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.*;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationTechnicalException;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningTestData;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AutoQaWorkflowService - Integração Generation")
class AutoQaWorkflowGenerationIntegrationTest {

    @Test
    @DisplayName("Deve executar discovery primeiro")
    void deveExecutarDiscoveryPrimeiro() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.discoveryService).discover(any());
        assertThat(context.getAgentExecutions().get(0).message()).isNotNull();
    }

    @Test
    @DisplayName("Deve executar analysis em segundo")
    void deveExecutarAnalysisSegundo() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.scenarioService).analyze(any(), any());
    }

    @Test
    @DisplayName("Deve executar knowledge em terceiro")
    void deveExecutarKnowledgeTerceiro() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.knowledgeService).collect(any(), any());
    }

    @Test
    @DisplayName("Deve executar planning em quarto")
    void deveExecutarPlanningQuarto() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.planningService).plan(any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar generation em quinto")
    void deveExecutarGenerationQuinto() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.generationService).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve finalizar com cinco agentes")
    void deveFinalizarComCincoAgentes() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getAgentExecutions()).hasSize(5);
    }

    @Test
    @DisplayName("Deve interromper generation quando discovery falhar")
    void deveInterromperGenerationQuandoDiscoveryFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenThrow(new IllegalStateException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.generationService, never()).generate(any(), any(), any(), any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve interromper generation quando analysis falhar")
    void deveInterromperGenerationQuandoAnalysisFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any()))
                .thenThrow(new com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisTechnicalException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve interromper generation quando knowledge falhar")
    void deveInterromperGenerationQuandoKnowledgeFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any()))
                .thenThrow(new com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeValidationException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.generationService, never()).generate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve interromper generation quando planning falhar")
    void deveInterromperGenerationQuandoPlanningFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any()))
                .thenThrow(new com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningTechnicalException("falha"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        verify(mocks.generationService, never()).generate(any(), any(), any(), any(), any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
    }

    @Test
    @DisplayName("Deve interromper o workflow quando generation falhar")
    void deveInterromperWorkflowQuandoGenerationFalhar() {
        AutoQaContext context = context();
        var mocks = mocks();
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(readyPlan());
        when(mocks.generationService.generate(any(), any(), any(), any(), any()))
                .thenThrow(new GenerationTechnicalException("falha técnica"));

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getGenerationResult()).isNull();
    }

    @Test
    @DisplayName("Deve manter cinco resultados no contexto quando tudo ok")
    void deveManterCincoResultadosNoContexto() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isNotNull();
        assertThat(context.getGenerationResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve registrar cinco AgentExecutionResults quando todos bem sucedidos")
    void deveRegistrarCincoAgentExecutionResults() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getAgentExecutions()).hasSize(5);
        assertThat(context.getAgentExecutions()).allMatch(r -> r.success());
    }

    @Test
    @DisplayName("Deve respeitar a ordem dos cinco agentes (0,10,20,30,40)")
    void deveRespeitarOrderDosCincoAgentes() {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
                new GenerationAgent(Mockito.mock(GenerationService.class)),
                new PlanningAgent(Mockito.mock(PlanningService.class)),
                new ProjectKnowledgeAgent(Mockito.mock(ProjectKnowledgeService.class)),
                new ScenarioAnalysisAgent(Mockito.mock(ScenarioAnalysisService.class)),
                new ProjectDiscoveryAgent(Mockito.mock(ProjectDiscoveryService.class))
        ));
        AnnotationAwareOrderComparator.sort(agents);

        assertThat(agents.get(0)).isInstanceOf(ProjectDiscoveryAgent.class);
        assertThat(agents.get(1)).isInstanceOf(ScenarioAnalysisAgent.class);
        assertThat(agents.get(2)).isInstanceOf(ProjectKnowledgeAgent.class);
        assertThat(agents.get(3)).isInstanceOf(PlanningAgent.class);
        assertThat(agents.get(4)).isInstanceOf(GenerationAgent.class);
    }

    @Test
    @DisplayName("Não deve aplicar arquivos no projeto original (projectPath permanece inalterado)")
    void deveNaoAplicarArquivosNoProjetoOriginal() {
        AutoQaContext context = context();
        var mocks = mocks();
        stubHappyPath(mocks);
        String originalProjectPath = context.getProjectPath();

        new AutoQaWorkflowService(orderedAgents(mocks)).execute(context);

        assertThat(context.getProjectPath()).isEqualTo(originalProjectPath);
        assertThat(context.getGenerationResult().generatedRoot()).doesNotContain(originalProjectPath);
    }

    // --- helpers ---

    private record Mocks(
            ProjectDiscoveryService discoveryService,
            ScenarioAnalysisService scenarioService,
            ProjectKnowledgeService knowledgeService,
            PlanningService planningService,
            GenerationService generationService
    ) {}

    private Mocks mocks() {
        return new Mocks(
                Mockito.mock(ProjectDiscoveryService.class),
                Mockito.mock(ScenarioAnalysisService.class),
                Mockito.mock(ProjectKnowledgeService.class),
                Mockito.mock(PlanningService.class),
                Mockito.mock(GenerationService.class)
        );
    }

    private void stubHappyPath(Mocks mocks) {
        when(mocks.discoveryService.discover(any())).thenReturn(discovery());
        when(mocks.scenarioService.analyze(any(), any())).thenReturn(analysis());
        when(mocks.knowledgeService.collect(any(), any())).thenReturn(knowledge());
        when(mocks.planningService.plan(any(), any(), any())).thenReturn(readyPlan());
        when(mocks.generationService.generate(any(), any(), any(), any(), any())).thenReturn(generationResult());
    }

    private List<AutoQaAgent> orderedAgents(Mocks mocks) {
        List<AutoQaAgent> agents = new ArrayList<>(List.of(
                new GenerationAgent(mocks.generationService),
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

    private TechnicalPlanResult readyPlan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private GenerationResult generationResult() {
        GeneratedFile file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, GenerationTestData.PLAYWRIGHT_CONTENT, GeneratedFileStatus.GENERATED, false);
        UUID executionId = UUID.randomUUID();
        return new GenerationResult(
                executionId, "PLAYWRIGHT", "TYPESCRIPT",
                List.of(file), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );
    }
}
