package com.br.criarcenariotestes.infrastructure.entity;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Documento MongoDB para persistência das execuções Auto QA.
 * O conteúdo dos arquivos gerados fica no filesystem — aqui somente metadados.
 * Caminhos são armazenados como String (nunca como java.nio.file.Path).
 * Credenciais e dados sensíveis nunca são armazenados.
 */
@Data
@Document("auto_qa_execution")
public class AutoQaExecutionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String executionId;

    private String title;

    private String scenarioId;

    /** Caminho normalizado do projeto como String. */
    private String projectPath;

    private AutomationFramework framework;

    private AutomationLanguage language;

    private AutoQaMode mode;

    private AutoQaStatus status;

    private AutomationPlan automationPlan;

    private List<GeneratedFileMetadata> generatedFileMetadata;

    private ProjectDiscoveryResult discoveryResult;

    private ProjectAnalysisResult projectAnalysis;

    private CodeReviewResult codeReviewResult;

    private TestExecutionResult executionResult;

    private FailureAnalysisResult failureAnalysisResult;

    private String scenarioText;

    private boolean allowFileUpdate;

    private boolean executeAfterGeneration;

    private List<WorkflowIssue> issues;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime finishedAt;
}
