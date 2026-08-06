package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Executa apenas o subconjunto de AutoQaAgent correspondente aos AutoQaStage
 * pedidos, na mesma ordem em que a lista de agentes foi recebida (a ordem
 * real do Spring, via @Order — este componente nunca reordena, apenas
 * filtra). Para no primeiro AgentExecutionResult.failure()/exceção/retorno
 * nulo, exatamente como AutoQaWorkflowService faz para o workflow completo.
 * Não conhece Mongo, não persiste, não calcula ações disponíveis, não decide
 * transição de AutoQaWorkflowStatus, não duplica lógica interna de nenhum
 * agente — apenas orquestra a chamada. Não substitui nem instancia
 * AutoQaWorkflowService.
 */
@Component
public class AutoQaStageExecutor {

    private final List<AutoQaAgent> agents;

    public AutoQaStageExecutor(List<AutoQaAgent> agents) {
        this.agents = List.copyOf(Objects.requireNonNull(agents, "agents must not be null"));
    }

    public AutoQaStageExecutionResult executeStages(AutoQaContext context, List<AutoQaStage> stages) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(stages, "stages must not be null");
        Set<AutoQaStage> requested = new LinkedHashSet<>(stages);

        AutoQaStage lastStageStarted = null;
        AutoQaStage lastStageCompleted = null;

        for (AutoQaAgent agent : agents) {
            AutoQaStage stage = resolveStage(agent);
            if (stage == null || !requested.contains(stage)) {
                continue;
            }
            lastStageStarted = stage;
            context.startAgent(agent.getName());

            AgentExecutionResult result;
            try {
                result = agent.execute(context);
            } catch (RuntimeException ex) {
                String message = buildExceptionMessage(agent.getName(), ex);
                context.addAgentExecution(AgentExecutionResult.failure(message));
                return AutoQaStageExecutionResult.failure(lastStageStarted, lastStageCompleted, message);
            }

            if (result == null) {
                String message = agent.getName() + " retornou resultado nulo";
                context.addAgentExecution(AgentExecutionResult.failure(message));
                return AutoQaStageExecutionResult.failure(lastStageStarted, lastStageCompleted, message);
            }

            context.addAgentExecution(result);

            if (!result.success()) {
                return AutoQaStageExecutionResult.failure(lastStageStarted, lastStageCompleted, result.message());
            }
            lastStageCompleted = stage;
        }

        return AutoQaStageExecutionResult.success(lastStageStarted, lastStageCompleted);
    }

    private AutoQaStage resolveStage(AutoQaAgent agent) {
        try {
            return AutoQaStage.fromAgentName(agent.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildExceptionMessage(String agentName, RuntimeException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return agentName + " lançou exceção: " + detail.trim();
    }

    public record AutoQaStageExecutionResult(boolean success, AutoQaStage lastStageStarted,
                                              AutoQaStage lastStageCompleted, String message) {
        public static AutoQaStageExecutionResult success(AutoQaStage lastStageStarted, AutoQaStage lastStageCompleted) {
            return new AutoQaStageExecutionResult(true, lastStageStarted, lastStageCompleted, null);
        }

        public static AutoQaStageExecutionResult failure(AutoQaStage lastStageStarted, AutoQaStage lastStageCompleted, String message) {
            return new AutoQaStageExecutionResult(false, lastStageStarted, lastStageCompleted, message);
        }
    }
}
