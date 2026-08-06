package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.learning.LearningService;
import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningResult;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Constrói conhecimento estruturado a partir dos resultados reais do
 * workflow. Não altera código, agentes ou prompts anteriores, não aplica
 * aprendizado automaticamente, não persiste, não executa comando/arquivo.
 */
@Component
@Order(90)
public class LearningAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(LearningAgent.class);

    private static final Set<LearningStatus> AGENT_SUCCESS_STATUSES = EnumSet.of(
            LearningStatus.COLLECTED, LearningStatus.COLLECTED_WITH_WARNINGS,
            LearningStatus.REVIEW_REQUIRED, LearningStatus.SKIPPED);

    private final LearningService service;

    public LearningAgent(LearningService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @Override
    public String getName() {
        return "learning";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Learning agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null
                || context.getGenerationResult() == null
                || context.getCodeReviewResult() == null
                || context.getApplyResult() == null
                || context.getExecutionResult() == null
                || context.getFailureAnalysisResult() == null) {
            return failureSkip(context, "missing-preconditions");
        }

        try {
            LearningResult result = service.learn(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult(),
                    context.getProjectKnowledgeResult(),
                    context.getTechnicalPlanResult(),
                    context.getGenerationResult(),
                    context.getCodeReviewResult(),
                    context.getApplyResult(),
                    context.getExecutionResult(),
                    context.getFailureAnalysisResult()
            );
            context.registerLearning(result);
            log.info("Learning agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return buildAgentResult(result);
        } catch (LearningException ex) {
            log.warn("Learning agent failed. executionId={}, failureType={}",
                    context.getExecutionId(), ex.getClass().getSimpleName());
            log.info("Learning agent finished. executionId={}, status=BLOCKED", context.getExecutionId());
            context.addError("Falha ao coletar aprendizado da execução");
            return AgentExecutionResult.failure("Falha ao coletar aprendizado da execução");
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Learning agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Learning agent finished. executionId={}, status=BLOCKED", context.getExecutionId());
        return AgentExecutionResult.failure("Falha ao coletar aprendizado da execução");
    }

    private AgentExecutionResult buildAgentResult(LearningResult result) {
        String summary = "Aprendizado coletado: " + result.status() + " / " + result.items().size()
                + " itens / confiança " + result.confidence();
        if (AGENT_SUCCESS_STATUSES.contains(result.status())) {
            return AgentExecutionResult.success(summary);
        }
        return AgentExecutionResult.failure(summary);
    }
}
