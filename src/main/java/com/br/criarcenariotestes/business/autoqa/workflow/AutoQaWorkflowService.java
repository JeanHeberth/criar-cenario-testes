package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AutoQaWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AutoQaWorkflowService.class);

    private final List<AutoQaAgent> agents;

    public AutoQaWorkflowService(List<AutoQaAgent> agents) {
        this.agents = List.copyOf(Objects.requireNonNull(agents, "agents must not be null"));
    }

    public AutoQaContext execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");

        context.startWorkflow();
        log.info("Auto QA started. executionId={}, agents={}", context.getExecutionId(), agents.size());

        for (AutoQaAgent agent : agents) {
            AutoQaAgent currentAgent = Objects.requireNonNull(agent, "agent must not be null");
            String agentName = requireAgentName(currentAgent.getName());

            context.startAgent(agentName);
            log.info("Executing agent. executionId={}, agent={}", context.getExecutionId(), agentName);

            AgentExecutionResult result;
            try {
                result = currentAgent.execute(context);
            } catch (RuntimeException exception) {
                String failureMessage = buildExceptionMessage(agentName, exception);
                log.info(
                        "Agent failed. executionId={}, agent={}, message={}",
                        context.getExecutionId(),
                        agentName,
                        failureMessage
                );
                context.addAgentExecution(AgentExecutionResult.failure(failureMessage));
                context.finishWithError(failureMessage);
                log.info("Auto QA finished. executionId={}, status={}", context.getExecutionId(), context.getStatus());
                return context;
            }

            if (result == null) {
                String failureMessage = agentName + " retornou resultado nulo";
                log.info(
                        "Agent failed. executionId={}, agent={}, message={}",
                        context.getExecutionId(),
                        agentName,
                        failureMessage
                );
                context.addAgentExecution(AgentExecutionResult.failure(failureMessage));
                context.finishWithError(failureMessage);
                log.info("Auto QA finished. executionId={}, status={}", context.getExecutionId(), context.getStatus());
                return context;
            }

            context.addAgentExecution(result);

            if (!result.success()) {
                String failureMessage = agentName + ": " + result.message();
                log.info(
                        "Agent failed. executionId={}, agent={}, message={}",
                        context.getExecutionId(),
                        agentName,
                        failureMessage
                );
                context.finishWithError(failureMessage);
                log.info("Auto QA finished. executionId={}, status={}", context.getExecutionId(), context.getStatus());
                return context;
            }
        }

        context.finishSuccessfully();
        log.info("Auto QA finished. executionId={}, status={}", context.getExecutionId(), context.getStatus());
        return context;
    }

    private String requireAgentName(String agentName) {
        if (agentName == null) {
            throw new IllegalArgumentException("agentName must not be null");
        }
        String trimmed = agentName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("agentName must not be blank");
        }
        return trimmed;
    }

    private String buildExceptionMessage(String agentName, RuntimeException exception) {
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getClass().getSimpleName();
        }
        return agentName + " lançou exceção: " + detail.trim();
    }
}
