package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.apply.FileApplicationService;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyValidationException;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Aplica no projeto real somente arquivos já planejados, gerados e
 * revisados. Não executa IA, comandos externos ou testes do projeto
 * analisado. Não cria ExecuteAgent nem avança para a Fase 9.
 */
@Component
@Order(60)
public class ApplyAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ApplyAgent.class);

    private final FileApplicationService fileApplicationService;

    public ApplyAgent(FileApplicationService fileApplicationService) {
        this.fileApplicationService = Objects.requireNonNull(fileApplicationService, "fileApplicationService must not be null");
    }

    @Override
    public String getName() {
        return "apply";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Apply agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null
                || context.getGenerationResult() == null
                || context.getCodeReviewResult() == null
                || context.getApplyApproval() == null) {
            return failureSkip(context, "missing-preconditions");
        }

        if (context.getScenarioAnalysisResult().status() == ScenarioAnalysisStatus.INVALID) {
            return failureSkip(context, "invalid-scenario");
        }
        if (context.getProjectKnowledgeResult().status() == KnowledgeStatus.FAILED) {
            return failureSkip(context, "failed-knowledge");
        }
        PlanningStatus planStatus = context.getTechnicalPlanResult().status();
        if (planStatus == PlanningStatus.BLOCKED || planStatus == PlanningStatus.INVALID) {
            return failureSkip(context, "blocked-or-invalid-plan");
        }
        GenerationStatus generationStatus = context.getGenerationResult().status();
        if (generationStatus == GenerationStatus.PARTIAL || generationStatus == GenerationStatus.FAILED) {
            return failureSkip(context, "generation-partial-or-failed");
        }
        ReviewStatus reviewStatus = context.getCodeReviewResult().status();
        if (reviewStatus == ReviewStatus.CHANGES_REQUIRED
                || reviewStatus == ReviewStatus.BLOCKED
                || reviewStatus == ReviewStatus.INVALID) {
            return failureSkip(context, "review-not-approved");
        }

        try {
            ApplyResult result = fileApplicationService.apply(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getTechnicalPlanResult(),
                    context.getGenerationResult(),
                    context.getCodeReviewResult(),
                    context.getApplyApproval()
            );
            context.registerApplyResult(result);
            log.info("Apply agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (ApplyValidationException | IllegalArgumentException exception) {
            log.warn("Apply agent failed. executionId={}, failureType={}",
                    context.getExecutionId(), exception.getClass().getSimpleName());
            log.info("Apply agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na aplicação de arquivos no projeto");
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Apply agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Apply agent finished. executionId={}, status=FAILED", context.getExecutionId());
        return AgentExecutionResult.failure("Falha na aplicação de arquivos no projeto");
    }

    private String buildSummary(ApplyResult result) {
        long applied = result.files().stream()
                .filter(f -> f.status() == ApplyFileStatus.APPLIED)
                .count();
        return "Aplicação concluída: " + result.status() + " / " + applied + " arquivos aplicados / "
                + result.conflicts().size() + " conflitos / " + result.backups().size() + " backups";
    }
}
