package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisException;
import com.br.criarcenariotestes.business.autoqa.scenario.ScenarioAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(10)
public class ScenarioAnalysisAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ScenarioAnalysisAgent.class);

    private final ScenarioAnalysisService scenarioAnalysisService;

    public ScenarioAnalysisAgent(ScenarioAnalysisService scenarioAnalysisService) {
        this.scenarioAnalysisService = Objects.requireNonNull(scenarioAnalysisService, "scenarioAnalysisService must not be null");
    }

    @Override
    public String getName() {
        return "scenario-analysis";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Scenario analysis started. executionId={}", context.getExecutionId());
        if (context.getProjectDiscoveryResult() == null) {
            log.warn("Scenario analysis skipped. executionId={}, status=missing-discovery", context.getExecutionId());
            log.info("Scenario analysis finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Scenario analysis requires project discovery result");
        }

        try {
            ScenarioAnalysisResult result = scenarioAnalysisService.analyze(
                    context.getScenario(),
                    context.getProjectDiscoveryResult()
            );
            context.registerScenarioAnalysis(result);
            log.info("Scenario analysis finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (ScenarioAnalysisException exception) {
            log.warn("Scenario analysis failed. executionId={}, failureType={}",
                    context.getExecutionId(), exception.getClass().getSimpleName());
            log.info("Scenario analysis finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na análise do cenário");
        }
    }

    private String buildSummary(ScenarioAnalysisResult result) {
        return "Cenário analisado: "
                + result.status() + " / "
                + result.automationType() + " / "
                + result.steps().size() + " passos";
    }
}
