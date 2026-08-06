package com.br.criarcenariotestes.business.autoqa.executionapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enums de AutoQaExecutionApi - Testes Unitários")
class AutoQaExecutionApiEnumsTest {

    @Test
    @DisplayName("AutoQaWorkflowStatus deve conter todos os status esperados, nesta ordem")
    void workflowStatusDeveConterValoresEsperados() {
        assertThat(AutoQaWorkflowStatus.values()).containsExactly(
                AutoQaWorkflowStatus.CREATED,
                AutoQaWorkflowStatus.RUNNING,
                AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL,
                AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL,
                AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL,
                AutoQaWorkflowStatus.COMPLETED,
                AutoQaWorkflowStatus.FAILED,
                AutoQaWorkflowStatus.CANCELLED
        );
    }

    @Test
    @DisplayName("AutoQaWorkflowStatus deve conter exatamente 3 estados terminais")
    void workflowStatusDeveConterTresEstadosTerminais() {
        assertThat(AutoQaWorkflowStatus.values())
                .filteredOn(AutoQaWorkflowStatus::terminal)
                .containsExactlyInAnyOrder(
                        AutoQaWorkflowStatus.COMPLETED,
                        AutoQaWorkflowStatus.FAILED,
                        AutoQaWorkflowStatus.CANCELLED
                );
    }

    @Test
    @DisplayName("AutoQaStage deve conter as 10 etapas na ordem exata das Fases 1-11")
    void stageDeveConterValoresEsperadosNaOrdem() {
        assertThat(AutoQaStage.values()).containsExactly(
                AutoQaStage.DISCOVERY,
                AutoQaStage.SCENARIO_ANALYSIS,
                AutoQaStage.PROJECT_KNOWLEDGE,
                AutoQaStage.PLANNING,
                AutoQaStage.GENERATION,
                AutoQaStage.REVIEW,
                AutoQaStage.APPLY,
                AutoQaStage.EXECUTION,
                AutoQaStage.FAILURE_ANALYSIS,
                AutoQaStage.LEARNING
        );
    }

    @Test
    @DisplayName("AutoQaStage deve mapear o nome real de cada AutoQaAgent")
    void stageDeveMapearNomeDoAgente() {
        assertThat(AutoQaStage.DISCOVERY.agentName()).isEqualTo("project-discovery");
        assertThat(AutoQaStage.SCENARIO_ANALYSIS.agentName()).isEqualTo("scenario-analysis");
        assertThat(AutoQaStage.PROJECT_KNOWLEDGE.agentName()).isEqualTo("project-knowledge");
        assertThat(AutoQaStage.PLANNING.agentName()).isEqualTo("planning");
        assertThat(AutoQaStage.GENERATION.agentName()).isEqualTo("generation");
        assertThat(AutoQaStage.REVIEW.agentName()).isEqualTo("review");
        assertThat(AutoQaStage.APPLY.agentName()).isEqualTo("apply");
        assertThat(AutoQaStage.EXECUTION.agentName()).isEqualTo("execute");
        assertThat(AutoQaStage.FAILURE_ANALYSIS.agentName()).isEqualTo("failure-analysis");
        assertThat(AutoQaStage.LEARNING.agentName()).isEqualTo("learning");
    }

    @Test
    @DisplayName("AutoQaStage.fromAgentName deve resolver corretamente")
    void stageFromAgentNameDeveResolver() {
        assertThat(AutoQaStage.fromAgentName("apply")).isEqualTo(AutoQaStage.APPLY);
    }

    @Test
    @DisplayName("AutoQaStage.fromAgentName deve rejeitar nome desconhecido")
    void stageFromAgentNameDeveRejeitarDesconhecido() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AutoQaStage.fromAgentName("inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("AutoQaOperationStatus deve conter os 4 valores esperados")
    void operationStatusDeveConterValoresEsperados() {
        assertThat(AutoQaOperationStatus.values()).containsExactly(
                AutoQaOperationStatus.IDLE,
                AutoQaOperationStatus.IN_PROGRESS,
                AutoQaOperationStatus.SUCCEEDED,
                AutoQaOperationStatus.FAILED
        );
    }

    @Test
    @DisplayName("AutoQaAvailableAction deve conter os 14 valores esperados")
    void availableActionDeveConterValoresEsperados() {
        assertThat(AutoQaAvailableAction.values()).containsExactly(
                AutoQaAvailableAction.START,
                AutoQaAvailableAction.CONTINUE,
                AutoQaAvailableAction.GENERATE,
                AutoQaAvailableAction.APPROVE_FILE_UPDATE,
                AutoQaAvailableAction.APPLY,
                AutoQaAvailableAction.APPROVE_EXECUTION,
                AutoQaAvailableAction.EXECUTE,
                AutoQaAvailableAction.CANCEL,
                AutoQaAvailableAction.RETRY,
                AutoQaAvailableAction.VIEW_GENERATED_FILES,
                AutoQaAvailableAction.VIEW_DIFF,
                AutoQaAvailableAction.VIEW_LOGS,
                AutoQaAvailableAction.VIEW_LEARNING,
                AutoQaAvailableAction.NONE
        );
    }
}
