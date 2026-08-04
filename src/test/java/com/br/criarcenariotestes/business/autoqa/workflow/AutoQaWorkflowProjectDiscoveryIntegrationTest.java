package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
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

@DisplayName("AutoQaWorkflowService - Integração ProjectDiscovery")
class AutoQaWorkflowProjectDiscoveryIntegrationTest {

    @Test
    @DisplayName("Deve executar ProjectDiscovery como primeiro agente")
    void deveExecutarProjectDiscoveryComoPrimeiroAgente() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent projectDiscoveryAgent = new ProjectDiscoveryAgent(service);
        RecordingAgent otherAgent = new RecordingAgent();
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        when(service.discover(Path.of("/tmp/project"))).thenReturn(sampleResult());

        List<AutoQaAgent> agents = new ArrayList<>(List.of(otherAgent, projectDiscoveryAgent));
        AnnotationAwareOrderComparator.sort(agents);

        AutoQaWorkflowService workflowService = new AutoQaWorkflowService(agents);
        workflowService.execute(context);

        assertThat(otherAgent.invocations.get()).isEqualTo(1);
        assertThat(context.getProjectDiscoveryResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve finalizar workflow quando descoberta for válida")
    void deveFinalizarWorkflowQuandoDescobertaForValida() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent projectDiscoveryAgent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        when(service.discover(Path.of("/tmp/project"))).thenReturn(sampleResult());

        AutoQaWorkflowService workflowService = new AutoQaWorkflowService(List.of(projectDiscoveryAgent));
        workflowService.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    @Test
    @DisplayName("Deve interromper workflow quando descoberta falhar")
    void deveInterromperWorkflowQuandoDescobertaFalhar() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent projectDiscoveryAgent = new ProjectDiscoveryAgent(service);
        RecordingAgent otherAgent = new RecordingAgent();
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        when(service.discover(Path.of("/tmp/project"))).thenThrow(new IllegalStateException("falha"));

        AutoQaWorkflowService workflowService = new AutoQaWorkflowService(List.of(projectDiscoveryAgent, otherAgent));
        workflowService.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(otherAgent.invocations.get()).isZero();
    }

    @Test
    @DisplayName("Deve manter resultado da descoberta no contexto final")
    void deveManterResultadoDaDescobertaNoContextoFinal() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent projectDiscoveryAgent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        ProjectDiscoveryResult result = sampleResult();

        when(service.discover(Path.of("/tmp/project"))).thenReturn(result);

        AutoQaWorkflowService workflowService = new AutoQaWorkflowService(List.of(projectDiscoveryAgent));
        workflowService.execute(context);

        assertThat(context.getProjectDiscoveryResult()).isEqualTo(result);
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
    }

    private ProjectDiscoveryResult sampleResult() {
        return new ProjectDiscoveryResult(
                Path.of("/tmp/project"),
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

    @Order(1)
    static class RecordingAgent implements AutoQaAgent {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public String getName() {
            return "recording-agent";
        }

        @Override
        public AgentExecutionResult execute(AutoQaContext context) {
            invocations.incrementAndGet();
            return AgentExecutionResult.success("ok");
        }
    }
}
