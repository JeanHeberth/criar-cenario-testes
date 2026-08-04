package com.br.criarcenariotestes.business.autoqa.model.response;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta geral do workflow Auto QA.
 * Path é serializado como String — nunca exposto como java.nio.file.Path.
 */
public record AutoQaResponse(

        String executionId,

        String title,

        AutoQaStatus status,

        String statusDescricao,

        AutomationFramework framework,

        AutomationLanguage language,

        AutoQaMode mode,

        String scenarioId,

        String scenarioText,

        boolean allowFileUpdate,

        boolean executeAfterGeneration,

        String projectPath,

        ProjectDiscoveryResult discoveryResult,

        ProjectAnalysisResult projectAnalysis,

        AutomationPlan automationPlan,

        List<GeneratedFileMetadata> generatedFiles,

        CodeReviewResult codeReviewResult,

        TestExecutionResult executionResult,

        FailureAnalysisResult failureAnalysisResult,

        List<WorkflowIssue> issues,

        LocalDateTime startedAt,

        LocalDateTime finishedAt,

        String message

) {}
