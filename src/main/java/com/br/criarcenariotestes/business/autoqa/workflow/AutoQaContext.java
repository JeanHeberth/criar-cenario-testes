package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysis;
import com.br.criarcenariotestes.business.autoqa.model.context.FixSuggestion;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowLog;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado compartilhado do workflow Auto QA.
 * Objeto em memória — não é serializado diretamente para JSON.
 * Use AutoQaResponse para expor dados ao frontend.
 */
@Getter
public class AutoQaContext {

    private final UUID executionId;
    private final AutoQaRequest request;
    private final LocalDateTime startedAt;
    private String executionIdString; // Para testes que fornecem string customizada

    private Path normalizedProjectPath;
    private AutoQaStatus status;
    private ProjectDiscoveryResult discoveryResult;
    private ProjectCatalog projectCatalog;
    private ProjectAnalysisResult projectAnalysis;
    private AutomationPlan automationPlan;
    private GeneratedCodeResponse generatedCodeResponse;
    private Path generatedFilesBasePath;
    private TestExecutionResult testExecutionResult;
    private LocalDateTime finishedAt;

    private final List<WorkflowIssue> issues = new ArrayList<>();
    private final List<WorkflowLog> workflowLogs = new ArrayList<>();
    private final List<FailureAnalysis> failureAnalyses = new ArrayList<>();
    private final List<FixSuggestion> fixSuggestions = new ArrayList<>();

    public AutoQaContext(AutoQaRequest request) {
        this.executionId = UUID.randomUUID();
        this.request = request;
        this.status = AutoQaStatus.CREATED;
        this.startedAt = LocalDateTime.now();
        addLog(WorkflowLog.info("INIT", "Execução Auto QA iniciada. id=" + executionId));
    }

    /**
     * Construtor alternativo para testes (aceita múltiplos argumentos flexíveis).
     * Último argumento string é tratado como executionId (opcional).
     */
    public AutoQaContext(Object... args) {
        // Tentar extrair executionId do último argumento
        UUID id = UUID.randomUUID();
        String idString = null;
        if (args.length > 0 && args[args.length - 1] instanceof String) {
            String argStr = (String) args[args.length - 1];
            idString = argStr;
            try {
                id = UUID.fromString(argStr);
            } catch (IllegalArgumentException e) {
                // Se não for UUID válido, usar UUID aleatório mas guardar a string
            }
        }
        this.executionId = id;
        this.executionIdString = idString;
        this.request = null;
        this.status = AutoQaStatus.CREATED;
        this.startedAt = LocalDateTime.now();
    }

    public void updateStatus(AutoQaStatus newStatus, String step) {
        this.status = newStatus;
        addLog(WorkflowLog.info(step, "Status: " + newStatus.getDescricao()));
    }

    public void addLog(WorkflowLog log) {
        workflowLogs.add(log);
    }

    public void addIssue(WorkflowIssue issue) {
        issues.add(issue);
        WorkflowLog.LogLevel level = switch (issue.severity()) {
            case ERROR, BLOCKER -> WorkflowLog.LogLevel.ERROR;
            case WARNING -> WorkflowLog.LogLevel.WARNING;
            default -> WorkflowLog.LogLevel.INFO;
        };
        workflowLogs.add(new WorkflowLog(
                LocalDateTime.now(), issue.step(), issue.message(), level
        ));
    }

    public boolean hasBlockers() {
        return issues.stream().anyMatch(WorkflowIssue::isBlocker);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(WorkflowIssue::isError);
    }

    /** Retorna o caminho normalizado como String (seguro para serialização). */
    public String projectPathAsString() {
        return normalizedProjectPath != null ? normalizedProjectPath.toString() : null;
    }

    public void setNormalizedProjectPath(Path path) {
        this.normalizedProjectPath = path;
    }

    public void setProjectPath(String path) {
        this.normalizedProjectPath = Path.of(path);
    }

    public String getProjectPath() {
        return normalizedProjectPath != null ? normalizedProjectPath.toString() : null;
    }

    public void setDiscoveryResult(ProjectDiscoveryResult result) {
        this.discoveryResult = result;
    }

    public void setProjectCatalog(ProjectCatalog catalog) {
        this.projectCatalog = catalog;
    }

    public void setProjectAnalysis(ProjectAnalysisResult analysis) {
        this.projectAnalysis = analysis;
    }

    public void setAutomationPlan(AutomationPlan plan) {
        this.automationPlan = plan;
    }

    public void setGeneratedCodeResponse(GeneratedCodeResponse response) {
        this.generatedCodeResponse = response;
    }

    public void setGeneratedFilesBasePath(Path path) {
        this.generatedFilesBasePath = path;
    }

    public void setTestExecutionResult(TestExecutionResult result) {
        this.testExecutionResult = result;
    }

    public TestExecutionResult getTestExecutionResult() {
        return testExecutionResult;
    }

    public void addFailureAnalysis(FailureAnalysis analysis) {
        this.failureAnalyses.add(analysis);
    }

    public List<FailureAnalysis> getFailureAnalyses() {
        return failureAnalyses;
    }

    public void addFixSuggestion(FixSuggestion suggestion) {
        this.fixSuggestions.add(suggestion);
    }

    public List<FixSuggestion> getFixSuggestions() {
        return fixSuggestions;
    }

    public void finish() {
        this.finishedAt = LocalDateTime.now();
    }

    public String executionIdAsString() {
        return executionIdString != null ? executionIdString : executionId.toString();
    }
}