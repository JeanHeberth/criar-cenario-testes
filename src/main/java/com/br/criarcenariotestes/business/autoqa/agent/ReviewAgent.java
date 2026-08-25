package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.review.CodeReviewService;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(50)
public class ReviewAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ReviewAgent.class);

    private final CodeReviewService codeReviewService;

    public ReviewAgent(CodeReviewService codeReviewService) {
        this.codeReviewService = Objects.requireNonNull(codeReviewService, "codeReviewService must not be null");
    }

    @Override
    public String getName() {
        return "review";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Review agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null
                || context.getGenerationResult() == null) {
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
        if (generationStatus == GenerationStatus.PARTIAL || generationStatus == GenerationStatus.FAILED) {
            return failureSkip(context, "generation-partial-or-failed");
        }

        try {
            CodeReviewResult result = codeReviewService.review(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult(),
                    context.getProjectKnowledgeResult(),
                    context.getTechnicalPlanResult(),
                    context.getGenerationResult(),
                    context.getScenario()
            );
            context.registerCodeReview(result);
            // Com CHANGES_REQUIRED o apply é bloqueado. Sem os achados no log,
            // o usuário só vê "falha na aplicação de arquivos" e não tem como
            // saber o que o revisor pediu para mudar.
            if (result.status() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus.CHANGES_REQUIRED) {
                log.warn("Revisão pediu mudanças. executionId={}, issuesGlobais={}, arquivos={}",
                        context.getExecutionId(), result.globalIssues(), result.files());
            }
            log.info("Review agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (CodeReviewException | IllegalArgumentException exception) {
            log.warn("Review agent failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), exception.getClass().getSimpleName(), exception.getMessage());
            log.info("Review agent finished. executionId={}, status=FAILED", context.getExecutionId());
            // O tipo e a mensagem dizem QUAL revisão quebrou (leitura de
            // artefato, parse da resposta da IA, validação). Sem eles a API
            // devolve um texto genérico e só o console do IntelliJ sabe o
            // motivo — quem usa o front não tem esse console.
            return AgentExecutionResult.failure("Falha na revisão de código gerado: "
                    + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Review agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Review agent finished. executionId={}, status=FAILED", context.getExecutionId());
        return AgentExecutionResult.failure("Revisão não executada: " + reason);
    }

    private String buildSummary(CodeReviewResult result) {
        long totalIssues = result.globalIssues().size()
                + result.files().stream().mapToLong(f -> f.issues().size()).sum();
        return "Revisão concluída: " + result.status() + " / " + result.files().size() + " arquivos / " + totalIssues + " issues";
    }
}
