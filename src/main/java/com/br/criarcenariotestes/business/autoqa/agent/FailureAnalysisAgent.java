package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.failure.FailureAnalysisService;
import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import org.springframework.core.annotation.Order;

import java.util.Objects;

@Order(80)
public class FailureAnalysisAgent implements com.br.criarcenariotestes.business.autoqa.agent.AutoQaAgent {

    private final FailureAnalysisService service;

    public FailureAnalysisAgent(FailureAnalysisService service) {
        this.service = service;
    }

    @Override
    public String getName() {
        return "failure-analysis";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        ExecutionResult execution = context.getExecutionResult();
        if (execution == null) throw new IllegalStateException("ExecutionResult must be present in context");

        try {
            FailureAnalysisResult result = service.analyze(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult(),
                    context.getProjectKnowledgeResult(),
                    context.getTechnicalPlanResult(),
                    context.getGenerationResult(),
                    context.getCodeReviewResult(),
                    context.getApplyResult(),
                    execution
            );
            context.registerFailureAnalysis(result);
            return AgentExecutionResult.success("Failure analysis: " + result.status());
        } catch (FailureAnalysisException ex) {
            context.addError(ex.getMessage());
            return AgentExecutionResult.failure(ex.getMessage());
        }
    }
}