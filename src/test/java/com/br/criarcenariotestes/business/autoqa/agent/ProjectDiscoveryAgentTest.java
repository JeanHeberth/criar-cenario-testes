package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
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
import org.springframework.core.annotation.Order;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectDiscoveryAgent - Testes Unitários")
class ProjectDiscoveryAgentTest {

    @Test
    @DisplayName("Deve possuir nome project-discovery")
    void devePossuirNomeProjectDiscovery() {
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(Mockito.mock(ProjectDiscoveryService.class));

        assertThat(agent.getName()).isEqualTo("project-discovery");
    }

    @Test
    @DisplayName("Deve executar descoberta e registrar no contexto")
    void deveExecutarDescobertaERegistrarNoContexto() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        ProjectDiscoveryResult result = sampleResult();

        when(service.discover(Path.of("/tmp/project"))).thenReturn(result);

        AgentExecutionResult executionResult = agent.execute(context);

        verify(service).discover(Path.of("/tmp/project"));
        assertThat(context.getProjectDiscoveryResult()).isEqualTo(result);
        assertThat(executionResult.success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar sucesso com resumo técnico")
    void deveRetornarSucessoComResumoTecnico() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        ProjectDiscoveryResult result = sampleResult();

        when(service.discover(Path.of("/tmp/project"))).thenReturn(result);

        AgentExecutionResult executionResult = agent.execute(context);

        assertThat(executionResult.message()).isEqualTo("Projeto descoberto: PLAYWRIGHT / TYPESCRIPT / NPM");
    }

    @Test
    @DisplayName("Deve retornar falha quando serviço falhar")
    void deveRetornarFalhaQuandoServicoFalhar() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        when(service.discover(Path.of("/tmp/project"))).thenThrow(new IllegalStateException("falha"));

        AgentExecutionResult executionResult = agent.execute(context);

        assertThat(executionResult.success()).isFalse();
        assertThat(executionResult.message()).contains("falha");
        assertThat(context.getProjectDiscoveryResult()).isNull();
    }

    @Test
    @DisplayName("Deve não chamar serviço com contexto inválido")
    void deveNaoChamarServicoComContextoInvalido() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(service);

        assertThatThrownBy(() -> agent.execute(null))
                .isInstanceOf(NullPointerException.class);

        verify(service, never()).discover(any());
    }

    @Test
    @DisplayName("Deve não registrar resultado quando descoberta falhar")
    void deveNaoRegistrarResultadoQuandoDescobertaFalhar() {
        ProjectDiscoveryService service = Mockito.mock(ProjectDiscoveryService.class);
        ProjectDiscoveryAgent agent = new ProjectDiscoveryAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        when(service.discover(Path.of("/tmp/project"))).thenThrow(new IllegalStateException("falha"));

        agent.execute(context);

        assertThat(context.getProjectDiscoveryResult()).isNull();
    }

    @Test
    @DisplayName("Deve ser primeiro agente na ordem do Spring")
    void deveSerPrimeiroAgenteNaOrdemDoSpring() {
        Order order = ProjectDiscoveryAgent.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isZero();
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
}
