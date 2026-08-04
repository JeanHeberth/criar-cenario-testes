package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaWorkflowService - Testes Unitários")
class AutoQaWorkflowServiceTest {

    @Test
    @DisplayName("Deve executar os agentes na ordem configurada")
    void deveExecutarOsAgentesNaOrdemConfigurada() {
        AutoQaAgent agente1 = mock(AutoQaAgent.class);
        AutoQaAgent agente2 = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente1, agente2));

        when(agente1.getName()).thenReturn("Agente 1");
        when(agente2.getName()).thenReturn("Agente 2");
        when(agente1.execute(context)).thenReturn(AgentExecutionResult.success("ok 1"));
        when(agente2.execute(context)).thenReturn(AgentExecutionResult.success("ok 2"));

        service.execute(context);

        var order = inOrder(agente1, agente2);
        order.verify(agente1).execute(context);
        order.verify(agente2).execute(context);
    }

    @Test
    @DisplayName("Deve interromper o workflow quando um agente falhar")
    void deveInterromperOWorkflowQuandoUmAgenteFalhar() {
        AutoQaAgent agente1 = mock(AutoQaAgent.class);
        AutoQaAgent agente2 = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente1, agente2));

        when(agente1.getName()).thenReturn("Agente 1");
        when(agente2.getName()).thenReturn("Agente 2");
        when(agente1.execute(context)).thenReturn(AgentExecutionResult.failure("Falhou"));

        service.execute(context);

        verify(agente1).execute(context);
        verify(agente2, never()).execute(any());
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getErrors()).containsExactly("Agente 1: Falhou");
    }

    @Test
    @DisplayName("Deve finalizar workflow sem agentes")
    void deveFinalizarWorkflowSemAgentes() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of());

        service.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getAgentExecutions()).isEmpty();
        assertThat(context.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Deve interromper quando agente lançar exceção")
    void deveInterromperQuandoAgenteLancarExcecao() {
        AutoQaAgent agente = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente));

        when(agente.getName()).thenReturn("Agente 1");
        when(agente.execute(context)).thenThrow(new RuntimeException("boom"));

        service.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getErrors()).containsExactly("Agente 1 lançou exceção: boom");
    }

    @Test
    @DisplayName("Deve interromper quando agente retornar null")
    void deveInterromperQuandoAgenteRetornarNull() {
        AutoQaAgent agente = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente));

        when(agente.getName()).thenReturn("Agente 1");
        when(agente.execute(context)).thenReturn(null);

        service.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getErrors()).containsExactly("Agente 1 retornou resultado nulo");
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of());

        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    @DisplayName("Deve rejeitar lista de agentes nula")
    void deveRejeitarListaDeAgentesNula() {
        assertThatThrownBy(() -> new AutoQaWorkflowService(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar agente nulo na lista")
    void deveRejeitarAgenteNuloNaLista() {
        List<AutoQaAgent> agents = new ArrayList<>();
        agents.add(mock(AutoQaAgent.class));
        agents.add(null);

        assertThatThrownBy(() -> new AutoQaWorkflowService(agents))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar agente sem nome")
    void deveRejeitarAgenteSemNome() {
        AutoQaAgent agente = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente));

        when(agente.getName()).thenReturn("   ");

        assertThatThrownBy(() -> service.execute(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentName");
    }

    @Test
    @DisplayName("Deve registrar resultado de cada agente")
    void deveRegistrarResultadoDeCadaAgente() {
        AutoQaAgent agente1 = mock(AutoQaAgent.class);
        AutoQaAgent agente2 = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente1, agente2));

        when(agente1.getName()).thenReturn("Agente 1");
        when(agente2.getName()).thenReturn("Agente 2");
        when(agente1.execute(context)).thenReturn(AgentExecutionResult.success("resultado 1"));
        when(agente2.execute(context)).thenReturn(AgentExecutionResult.success("resultado 2"));

        service.execute(context);

        assertThat(context.getAgentExecutions())
                .extracting(AgentExecutionResult::message)
                .containsExactly("resultado 1", "resultado 2");
    }

    @Test
    @DisplayName("Deve definir status RUNNING durante execução")
    void deveDefinirStatusRunningDuranteExecucao() {
        AutoQaAgent agente = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente));
        AtomicReference<AutoQaStatus> statusDentroDoAgente = new AtomicReference<>();

        when(agente.getName()).thenReturn("Agente 1");
        when(agente.execute(context)).thenAnswer(invocation -> {
            statusDentroDoAgente.set(context.getStatus());
            return AgentExecutionResult.success("ok");
        });

        service.execute(context);

        assertThat(statusDentroDoAgente.get()).isEqualTo(AutoQaStatus.RUNNING);
    }

    @Test
    @DisplayName("Deve finalizar com status FINISHED")
    void deveFinalizarComStatusFinished() {
        AutoQaAgent agente = mock(AutoQaAgent.class);
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AutoQaWorkflowService service = new AutoQaWorkflowService(List.of(agente));

        when(agente.getName()).thenReturn("Agente 1");
        when(agente.execute(context)).thenReturn(AgentExecutionResult.success("ok"));

        service.execute(context);

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getFinishedAt()).isNotNull();
    }
}
