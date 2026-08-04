package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
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
import org.springframework.core.annotation.Order;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ScenarioAnalysisAgent - Testes Unitários")
class ScenarioAnalysisAgentTest {

    @Test
    @DisplayName("Deve possuir nome scenario-analysis")
    void devePossuirNomeScenarioAnalysis() {
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(Mockito.mock(ScenarioAnalysisService.class));

        assertThat(agent.getName()).isEqualTo("scenario-analysis");
    }

    @Test
    @DisplayName("Deve possuir order dez")
    void devePossuirOrderDez() {
        Order order = ScenarioAnalysisAgent.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve exigir contexto")
    void deveExigirContexto() {
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(Mockito.mock(ScenarioAnalysisService.class));

        assertThatThrownBy(() -> agent.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir ProjectDiscoveryResult")
    void deveExigirProjectDiscoveryResult() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(service, never()).analyze(anyString(), any());
    }

    @Test
    @DisplayName("Deve chamar ScenarioAnalysisService")
    void deveChamarScenarioAnalysisService() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(discovery());
        ScenarioAnalysisResult analysis = analysis();

        when(service.analyze("Cenário", discovery())).thenReturn(analysis);

        agent.execute(context);

        verify(service).analyze("Cenário", discovery());
    }

    @Test
    @DisplayName("Deve registrar resultado no contexto")
    void deveRegistrarResultadoNoContexto() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(discovery());
        ScenarioAnalysisResult analysis = analysis();

        when(service.analyze("Cenário", discovery())).thenReturn(analysis);

        agent.execute(context);

        assertThat(context.getScenarioAnalysisResult()).isEqualTo(analysis);
    }

    @Test
    @DisplayName("Deve retornar resumo técnico")
    void deveRetornarResumoTecnico() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(discovery());
        ScenarioAnalysisResult analysis = analysis();

        when(service.analyze("Cenário", discovery())).thenReturn(analysis);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).contains("Cenário analisado: VALID / WEB_UI / 1 passos");
    }

    @Test
    @DisplayName("Deve retornar falha quando service falhar")
    void deveRetornarFalhaQuandoServiceFalhar() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(discovery());

        when(service.analyze("Cenário", discovery())).thenThrow(new ScenarioAnalysisTechnicalException("falha"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve não registrar resultado em falha")
    void deveNaoRegistrarResultadoEmFalha() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(discovery());

        when(service.analyze("Cenário", discovery())).thenThrow(new ScenarioAnalysisTechnicalException("falha"));

        agent.execute(context);

        assertThat(context.getScenarioAnalysisResult()).isNull();
    }

    @Test
    @DisplayName("Deve não incluir cenário completo na mensagem")
    void deveNaoIncluirCenarioCompletoNaMensagem() {
        ScenarioAnalysisService service = Mockito.mock(ScenarioAnalysisService.class);
        ScenarioAnalysisAgent agent = new ScenarioAnalysisAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário muito longo com detalhes", "/projeto");
        context.registerProjectDiscovery(discovery());
        ScenarioAnalysisResult analysis = analysis();

        when(service.analyze("Cenário muito longo com detalhes", discovery())).thenReturn(analysis);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).doesNotContain("Cenário muito longo com detalhes");
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
