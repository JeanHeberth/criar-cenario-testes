package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeValidationException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectKnowledgeAgent - Testes Unitários")
class ProjectKnowledgeAgentTest {

    @Test
    @DisplayName("Deve possuir nome project-knowledge")
    void devePossuirNomeProjectKnowledge() {
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(Mockito.mock(ProjectKnowledgeService.class));

        assertThat(agent.getName()).isEqualTo("project-knowledge");
    }

    @Test
    @DisplayName("Deve possuir order vinte")
    void devePossuirOrderVinte() {
        Order order = ProjectKnowledgeAgent.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(20);
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(Mockito.mock(ProjectKnowledgeService.class));

        assertThatThrownBy(() -> agent.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir discovery")
    void deveExigirDiscovery() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(service, never()).collect(any(), any());
    }

    @Test
    @DisplayName("Deve exigir scenario analysis")
    void deveExigirScenarioAnalysis() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        verify(service, never()).collect(any(), any());
    }

    @Test
    @DisplayName("Deve chamar ProjectKnowledgeService")
    void deveChamarProjectKnowledgeService() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());
        ProjectKnowledgeResult knowledge = KnowledgeTestData.knowledge(java.nio.file.Path.of("/tmp/project"), KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.PAGE_OBJECT, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT));

        when(service.collect(any(), any())).thenReturn(knowledge);

        agent.execute(context);

        verify(service).collect(any(), any());
    }

    @Test
    @DisplayName("Deve registrar resultado no contexto")
    void deveRegistrarResultadoNoContexto() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());
        ProjectKnowledgeResult knowledge = KnowledgeTestData.knowledge(java.nio.file.Path.of("/tmp/project"), KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.PAGE_OBJECT, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT));

        when(service.collect(any(), any())).thenReturn(knowledge);

        agent.execute(context);

        assertThat(context.getProjectKnowledgeResult()).isEqualTo(knowledge);
    }

    @Test
    @DisplayName("Deve retornar resumo técnico")
    void deveRetornarResumoTecnico() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());
        ProjectKnowledgeResult knowledge = KnowledgeTestData.knowledge(java.nio.file.Path.of("/tmp/project"), KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.PAGE_OBJECT, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT));

        when(service.collect(any(), any())).thenReturn(knowledge);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).contains("Conhecimento coletado:");
        assertThat(result.message()).doesNotContain("/tmp/project");
    }

    @Test
    @DisplayName("Deve retornar falha quando service falhar")
    void deveRetornarFalhaQuandoServiceFalhar() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());

        when(service.collect(any(), any())).thenThrow(new ProjectKnowledgeValidationException("invalid"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve não registrar resultado em falha")
    void deveNaoRegistrarResultadoEmFalha() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());

        when(service.collect(any(), any())).thenThrow(new ProjectKnowledgeValidationException("invalid"));

        agent.execute(context);

        assertThat(context.getProjectKnowledgeResult()).isNull();
    }

    @Test
    @DisplayName("Deve não expor projectPath na mensagem")
    void deveNaoExporProjectPathNaMensagem() {
        ProjectKnowledgeService service = Mockito.mock(ProjectKnowledgeService.class);
        ProjectKnowledgeAgent agent = new ProjectKnowledgeAgent(service);
        AutoQaContext context = AutoQaContext.create("Cenário", "/tmp/project");
        context.registerProjectDiscovery(KnowledgeTestData.discovery(java.nio.file.Path.of("/tmp/project"), com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT));
        context.registerScenarioAnalysis(KnowledgeTestData.analysis());
        ProjectKnowledgeResult knowledge = KnowledgeTestData.knowledge(java.nio.file.Path.of("/tmp/project"), KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.PAGE_OBJECT, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT));

        when(service.collect(any(), any())).thenReturn(knowledge);

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.message()).doesNotContain("/tmp/project");
    }
}
