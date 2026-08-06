package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AutoQaStageExecutor - Testes Unitários")
class AutoQaStageExecutorTest {

    private AutoQaAgent discovery, scenarioAnalysis, projectKnowledge, planning, generation, review, apply, execute,
            failureAnalysis, learning;
    private List<AutoQaAgent> allAgentsInOrder;
    private AutoQaContext context;

    @BeforeEach
    void setUp() {
        discovery = mockAgent("project-discovery");
        scenarioAnalysis = mockAgent("scenario-analysis");
        projectKnowledge = mockAgent("project-knowledge");
        planning = mockAgent("planning");
        generation = mockAgent("generation");
        review = mockAgent("review");
        apply = mockAgent("apply");
        execute = mockAgent("execute");
        failureAnalysis = mockAgent("failure-analysis");
        learning = mockAgent("learning");
        allAgentsInOrder = List.of(discovery, scenarioAnalysis, projectKnowledge, planning, generation, review,
                apply, execute, failureAnalysis, learning);
        context = AutoQaContext.create("Cenário", "/projeto");
    }

    @Test
    @DisplayName("Deve executar apenas o bloco Discovery-Planning")
    void deveExecutarApenasBlocoDiscoveryPlanning() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS,
                AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING));

        verify(discovery).execute(context);
        verify(scenarioAnalysis).execute(context);
        verify(projectKnowledge).execute(context);
        verify(planning).execute(context);
        verifyNeverExecuted(generation, review, apply, execute, failureAnalysis, learning);
    }

    @Test
    @DisplayName("Deve executar apenas o bloco Generation-Review")
    void deveExecutarApenasBlocoGenerationReview() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW));

        verify(generation).execute(context);
        verify(review).execute(context);
        verifyNeverExecuted(discovery, scenarioAnalysis, projectKnowledge, planning, apply, execute, failureAnalysis, learning);
    }

    @Test
    @DisplayName("Deve executar apenas o bloco Apply")
    void deveExecutarApenasBlocoApply() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.APPLY));

        verify(apply).execute(context);
        verifyNeverExecuted(discovery, scenarioAnalysis, projectKnowledge, planning, generation, review, execute, failureAnalysis, learning);
    }

    @Test
    @DisplayName("Deve executar apenas o bloco Execution-FailureAnalysis-Learning")
    void deveExecutarApenasBlocoExecutionLearning() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING));

        verify(execute).execute(context);
        verify(failureAnalysis).execute(context);
        verify(learning).execute(context);
        verifyNeverExecuted(discovery, scenarioAnalysis, projectKnowledge, planning, generation, review, apply);
    }

    @Test
    @DisplayName("Não deve executar agentes anteriores ao estágio inicial pedido")
    void naoDeveExecutarAgentesAnterioresAoEstagioInicial() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.APPLY));

        verifyNeverExecuted(discovery, scenarioAnalysis, projectKnowledge, planning, generation, review);
    }

    @Test
    @DisplayName("Não deve executar agentes posteriores ao estágio final pedido")
    void naoDeveExecutarAgentesPosterioresAoEstagioFinal() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS,
                AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING));

        verifyNeverExecuted(generation, review, apply, execute, failureAnalysis, learning);
    }

    @Test
    @DisplayName("Deve executar os agentes do bloco na mesma ordem em que a lista foi fornecida (ordem Spring/@Order)")
    void deveExecutarNaOrdemFornecida() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context,
                List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING));

        InOrder inOrder = inOrder(discovery, scenarioAnalysis, projectKnowledge, planning);
        inOrder.verify(discovery).execute(context);
        inOrder.verify(scenarioAnalysis).execute(context);
        inOrder.verify(projectKnowledge).execute(context);
        inOrder.verify(planning).execute(context);
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("Ordem dos estágios pedidos não deve importar — a ordem real é sempre a da lista de agentes injetada")
    void ordemDosEstagiosPedidosNaoDeveImportar() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        executor.executeStages(context, List.of(AutoQaStage.PLANNING, AutoQaStage.DISCOVERY,
                AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE));

        InOrder inOrder = inOrder(discovery, scenarioAnalysis, projectKnowledge, planning);
        inOrder.verify(discovery).execute(context);
        inOrder.verify(scenarioAnalysis).execute(context);
        inOrder.verify(projectKnowledge).execute(context);
        inOrder.verify(planning).execute(context);
    }

    @Test
    @DisplayName("Deve interromper no primeiro AgentExecutionResult.failure()")
    void deveInterromperNoPrimeiroFailure() {
        when(scenarioAnalysis.execute(context)).thenReturn(AgentExecutionResult.failure("falhou"));
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context,
                List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING));

        assertThat(result.success()).isFalse();
        assertThat(result.lastStageStarted()).isEqualTo(AutoQaStage.SCENARIO_ANALYSIS);
        assertThat(result.lastStageCompleted()).isEqualTo(AutoQaStage.DISCOVERY);
        verify(projectKnowledge, never()).execute(any());
        verify(planning, never()).execute(any());
    }

    @Test
    @DisplayName("Deve interromper quando um agente lança RuntimeException")
    void deveInterromperQuandoAgenteLancaExcecao() {
        when(generation.execute(context)).thenThrow(new RuntimeException("erro técnico"));
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context,
                List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW));

        assertThat(result.success()).isFalse();
        verify(review, never()).execute(any());
    }

    @Test
    @DisplayName("Deve tratar retorno nulo de agente como falha")
    void deveTratarRetornoNuloComoFalha() {
        when(apply.execute(context)).thenReturn(null);
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context, List.of(AutoQaStage.APPLY));

        assertThat(result.success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar sucesso com lastStageCompleted correto quando todo o bloco roda bem")
    void deveRetornarSucessoComLastStageCompletedCorreto() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);

        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context,
                List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING));

        assertThat(result.success()).isTrue();
        assertThat(result.lastStageStarted()).isEqualTo(AutoQaStage.LEARNING);
        assertThat(result.lastStageCompleted()).isEqualTo(AutoQaStage.LEARNING);
    }

    @Test
    @DisplayName("Não deve conhecer nem referenciar nenhum tipo Mongo/Spring Data")
    void naoDeveReferenciarMongo() {
        for (var field : AutoQaStageExecutor.class.getDeclaredFields()) {
            assertThat(field.getType().getPackageName()).doesNotContain("mongodb").doesNotContain("mongo");
        }
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);
        assertThatThrownBy(() -> executor.executeStages(null, List.of(AutoQaStage.DISCOVERY)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar lista de estágios nula")
    void deveRejeitarListaDeEstagiosNula() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);
        assertThatThrownBy(() -> executor.executeStages(context, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Lista de estágios vazia não deve executar nenhum agente")
    void listaVaziaNaoExecutaNenhumAgente() {
        AutoQaStageExecutor executor = new AutoQaStageExecutor(allAgentsInOrder);
        AutoQaStageExecutor.AutoQaStageExecutionResult result = executor.executeStages(context, List.of());
        assertThat(result.success()).isTrue();
        verifyNeverExecuted(discovery, scenarioAnalysis, projectKnowledge, planning, generation, review, apply, execute, failureAnalysis, learning);
    }

    // --- helpers ---

    private AutoQaAgent mockAgent(String name) {
        AutoQaAgent agent = mock(AutoQaAgent.class);
        when(agent.getName()).thenReturn(name);
        when(agent.execute(any())).thenReturn(AgentExecutionResult.success(name + " ok"));
        return agent;
    }

    private void verifyNeverExecuted(AutoQaAgent... agentsToCheck) {
        for (AutoQaAgent agent : agentsToCheck) {
            verify(agent, never()).execute(any());
        }
    }
}
