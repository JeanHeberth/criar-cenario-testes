package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationService;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Component
@Order(40)
public class GenerationAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(GenerationAgent.class);

    private static final Set<AutomationFramework> SUPPORTED_FRAMEWORKS = EnumSet.of(
            AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS, AutomationFramework.SELENIDE,
            AutomationFramework.SELENIUM, AutomationFramework.REST_ASSURED, AutomationFramework.ROBOT_FRAMEWORK
    );

    private final GenerationService generationService;

    public GenerationAgent(GenerationService generationService) {
        this.generationService = Objects.requireNonNull(generationService, "generationService must not be null");
    }

    @Override
    public String getName() {
        return "generation";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Generation agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null) {
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

        if (!SUPPORTED_FRAMEWORKS.contains(context.getProjectDiscoveryResult().getAutomationFramework())) {
            return failureSkip(context, "unsupported-framework");
        }

        try {
            GenerationResult result = generationService.generate(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult(),
                    context.getProjectKnowledgeResult(),
                    context.getTechnicalPlanResult()
            );
            context.registerGeneration(result);
            log.info("Generation agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (GenerationException | IllegalArgumentException exception) {
            // O tipo da exceção sozinho não diz QUAL regra reprovou, e a resposta
            // da IA muda a cada geração: sem a mensagem, reproduzir para
            // diagnosticar custa outra rodada de chamadas.
            log.warn("Generation agent failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), exception.getClass().getSimpleName(), exception.getMessage());
            log.info("Generation agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na geração de automação");
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Generation agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Generation agent finished. executionId={}, status=FAILED", context.getExecutionId());
        return AgentExecutionResult.failure("Falha na geração de automação");
    }

    private String buildSummary(GenerationResult result) {
        long generated = result.files().stream().filter(f -> f.status() == GeneratedFileStatus.GENERATED).count();
        return "Geração concluída: " + result.status() + " / " + generated + " arquivos / "
                + result.reusedFiles().size() + " reutilizações";
    }
}
