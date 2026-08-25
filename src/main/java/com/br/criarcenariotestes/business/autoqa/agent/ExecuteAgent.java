package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ExecutionValidationException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Executa os testes automatizados do projeto analisado após uma aplicação
 * válida (ApplyStatus COMPLETED ou COMPLETED_WITH_WARNINGS). Não usa IA, não
 * corrige testes, não altera arquivos, não cria FailureAgent e não avança
 * para a Fase 10. FAILED significa que o comando foi executado corretamente
 * e os testes falharam — não é uma falha técnica do agente.
 */
@Component
@Order(70)
public class ExecuteAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ExecuteAgent.class);

    private static final Set<ExecutionStatus> AGENT_SUCCESS_STATUSES = EnumSet.of(
            ExecutionStatus.PASSED, ExecutionStatus.FAILED);

    private final TestExecutionService testExecutionService;

    public ExecuteAgent(TestExecutionService testExecutionService) {
        this.testExecutionService = Objects.requireNonNull(testExecutionService, "testExecutionService must not be null");
    }

    @Override
    public String getName() {
        return "execute";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Execute agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null
                || context.getGenerationResult() == null
                || context.getCodeReviewResult() == null
                || context.getApplyResult() == null
                || context.getExecutionApproval() == null) {
            return failureSkip(context, "missing-preconditions");
        }

        if (context.getScenarioAnalysisResult().status() == ScenarioAnalysisStatus.INVALID) {
            return failureSkip(context, "invalid-scenario");
        }
        if (context.getProjectKnowledgeResult().status() == KnowledgeStatus.FAILED) {
            return failureSkip(context, "failed-knowledge");
        }
        PlanningStatus planStatus = context.getTechnicalPlanResult().status();
        if (planStatus != PlanningStatus.READY && planStatus != PlanningStatus.READY_WITH_WARNINGS) {
            return failureSkip(context, "plan-not-ready");
        }
        GenerationStatus generationStatus = context.getGenerationResult().status();
        if (generationStatus != GenerationStatus.COMPLETED && generationStatus != GenerationStatus.COMPLETED_WITH_WARNINGS) {
            return failureSkip(context, "generation-not-completed");
        }
        ReviewStatus reviewStatus = context.getCodeReviewResult().status();
        if (reviewStatus != ReviewStatus.APPROVED && reviewStatus != ReviewStatus.APPROVED_WITH_WARNINGS) {
            return failureSkip(context, "review-not-approved");
        }
        ApplyStatus applyStatus = context.getApplyResult().status();
        if (applyStatus != ApplyStatus.COMPLETED && applyStatus != ApplyStatus.COMPLETED_WITH_WARNINGS) {
            return failureSkip(context, "apply-not-completed");
        }

        try {
            ExecutionResult result = testExecutionService.execute(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getProjectKnowledgeResult(),
                    context.getApplyResult(),
                    context.getExecutionApproval()
            );
            context.registerExecutionResult(result);
            log.info("Execute agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return buildAgentResult(result);
        } catch (ExecutionValidationException | IllegalArgumentException exception) {
            log.warn("Execute agent failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), exception.getClass().getSimpleName(), exception.getMessage());
            log.info("Execute agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na execução dos testes do projeto: "
                    + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Execute agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Execute agent finished. executionId={}, status=FAILED", context.getExecutionId());
        return AgentExecutionResult.failure("Falha na execução dos testes do projeto: " + reason);
    }

    private AgentExecutionResult buildAgentResult(ExecutionResult result) {
        String summary = buildSummary(result);
        if (AGENT_SUCCESS_STATUSES.contains(result.status())) {
            return AgentExecutionResult.success(summary);
        }
        return AgentExecutionResult.failure(summary);
    }

    private String buildSummary(ExecutionResult result) {
        StringBuilder summary = new StringBuilder("Execução concluída: ").append(result.status());
        if (result.exitCode() != null) {
            summary.append(" / exitCode=").append(result.exitCode());
        }
        long failedTests = result.summaries().stream().mapToInt(TestExecutionSummary::failed).sum();
        if (failedTests > 0) {
            summary.append(" / ").append(failedTests).append(" testes falharam");
        }
        return summary.toString();
    }
}
