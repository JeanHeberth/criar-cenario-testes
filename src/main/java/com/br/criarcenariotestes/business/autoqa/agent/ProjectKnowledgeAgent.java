package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeException;
import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeService;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(20)
public class ProjectKnowledgeAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ProjectKnowledgeAgent.class);

    private final ProjectKnowledgeService projectKnowledgeService;

    public ProjectKnowledgeAgent(ProjectKnowledgeService projectKnowledgeService) {
        this.projectKnowledgeService = Objects.requireNonNull(projectKnowledgeService, "projectKnowledgeService must not be null");
    }

    @Override
    public String getName() {
        return "project-knowledge";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Project knowledge started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null || context.getScenarioAnalysisResult() == null) {
            log.warn("Project knowledge skipped. executionId={}, reason=missing-preconditions", context.getExecutionId());
            log.info("Project knowledge finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Project knowledge requires discovery and scenario analysis");
        }

        try {
            ProjectKnowledgeResult result = projectKnowledgeService.collect(
                    context.getProjectDiscoveryResult(),
                    context.getScenarioAnalysisResult()
            );
            context.registerProjectKnowledge(result);
            log.info("Project knowledge finished. executionId={}, status={}", context.getExecutionId(), result.status());
            return AgentExecutionResult.success(buildSummary(result));
        } catch (ProjectKnowledgeException | IllegalArgumentException exception) {
            log.warn("Project knowledge failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), exception.getClass().getSimpleName(), exception.getMessage());
            log.info("Project knowledge finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na coleta de conhecimento do projeto");
        }
    }

    private String buildSummary(ProjectKnowledgeResult result) {
        return "Conhecimento coletado: "
                + result.status() + " / "
                + result.components().size() + " componentes / "
                + result.reuseCandidates().size() + " candidatos";
    }
}
