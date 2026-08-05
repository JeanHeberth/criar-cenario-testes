package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningService;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@Order(30)
public class PlanningAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanningAgent.class);

    private final PlanningService planningService;

    public PlanningAgent(PlanningService planningService) {
        this.planningService = Objects.requireNonNull(planningService, "planningService must not be null");
    }

    @Override
    public String getName() {
        return "planning";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Planning agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null) {
            log.warn("Planning agent skipped. executionId={}, reason=missing-preconditions", context.getExecutionId());
            log.info("Planning agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha no planejamento técnico");
        }

        if (context.getScenarioAnalysisResult().status() ==
                com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus.INVALID) {
            log.warn("Planning agent skipped. executionId={}, reason=invalid-scenario", context.getExecutionId());
            log.info("Planning agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha no planejamento técnico");
        }

        if (context.getProjectKnowledgeResult().status() ==
                com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus.FAILED) {
            log.warn("Planning agent skipped. executionId={}, reason=failed-knowledge", context.getExecutionId());
            log.info("Planning agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha no planejamento técnico");
        }

        try {
            TechnicalPlanResult result = planningService.plan(
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult(),
                    context.getProjectKnowledgeResult()
            );
            context.registerTechnicalPlan(result);
            log.info("Planning agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (PlanningException | IllegalArgumentException exception) {
            log.warn("Planning agent failed. executionId={}, failureType={}",
                    context.getExecutionId(), exception.getClass().getSimpleName());
            log.info("Planning agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha no planejamento técnico");
        }
    }

    private String buildSummary(TechnicalPlanResult result) {
        long reuseCount = result.reuseDecisions().stream()
                .filter(com.br.criarcenariotestes.business.autoqa.model.planning.ReuseDecision::reuse)
                .count();
        return "Plano criado: " + result.status() + " / "
                + result.fileActions().size() + " arquivos / "
                + reuseCount + " reutilizações";
    }
}
