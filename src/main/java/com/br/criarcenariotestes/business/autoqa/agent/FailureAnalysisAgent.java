package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.failure.FailureAnalysisService;
import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(80)
public class FailureAnalysisAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(FailureAnalysisAgent.class);

    private final FailureAnalysisService service;

    public FailureAnalysisAgent(FailureAnalysisService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @Override
    public String getName() {
        return "failure-analysis";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Failure analysis agent started. executionId={}", context.getExecutionId());
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
            log.info("Failure analysis agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success("Failure analysis: " + result.status());
        } catch (FailureAnalysisException ex) {
            log.warn("Failure analysis agent failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), ex.getClass().getSimpleName(), ex.getMessage());
            context.addError(ex.getMessage());
            return AgentExecutionResult.failure(ex.getMessage());
        }
    }
}