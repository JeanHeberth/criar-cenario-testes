package com.br.criarcenariotestes.business.autoqa.context;

import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AutoQaContext {

    private final UUID executionId;
    private final String scenario;
    private final String projectPath;
    private final LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private AutoQaStatus status;
    private String currentAgent;
    private ProjectDiscoveryResult projectDiscoveryResult;
    private ScenarioAnalysisResult scenarioAnalysisResult;
    private ProjectKnowledgeResult projectKnowledgeResult;
    private TechnicalPlanResult technicalPlanResult;
    private GenerationResult generationResult;
    private CodeReviewResult codeReviewResult;
    private ApplyApproval applyApproval;
    private ApplyResult applyResult;
    private ExecutionApproval executionApproval;
    private ExecutionResult executionResult;
    private final List<AgentExecutionResult> agentExecutions;
    private final List<String> errors;

    private AutoQaContext(String scenario, String projectPath) {
        this.executionId = UUID.randomUUID();
        this.scenario = requireText(scenario, "scenario");
        this.projectPath = requireText(projectPath, "projectPath");
        this.startedAt = LocalDateTime.now();
        this.status = AutoQaStatus.CREATED;
        this.finishedAt = null;
        this.currentAgent = null;
        this.projectDiscoveryResult = null;
        this.scenarioAnalysisResult = null;
        this.projectKnowledgeResult = null;
        this.technicalPlanResult = null;
        this.generationResult = null;
        this.codeReviewResult = null;
        this.applyApproval = null;
        this.applyResult = null;
        this.executionApproval = null;
        this.executionResult = null;
        this.agentExecutions = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public static AutoQaContext create(String scenario, String projectPath) {
        return new AutoQaContext(scenario, projectPath);
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getScenario() {
        return scenario;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public AutoQaStatus getStatus() {
        return status;
    }

    public String getCurrentAgent() {
        return currentAgent;
    }

    public List<AgentExecutionResult> getAgentExecutions() {
        return Collections.unmodifiableList(agentExecutions);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public ProjectDiscoveryResult getProjectDiscoveryResult() {
        return projectDiscoveryResult;
    }

    public ScenarioAnalysisResult getScenarioAnalysisResult() {
        return scenarioAnalysisResult;
    }

    public ProjectKnowledgeResult getProjectKnowledgeResult() {
        return projectKnowledgeResult;
    }

    public TechnicalPlanResult getTechnicalPlanResult() {
        return technicalPlanResult;
    }

    public GenerationResult getGenerationResult() {
        return generationResult;
    }

    public CodeReviewResult getCodeReviewResult() {
        return codeReviewResult;
    }

    public ApplyApproval getApplyApproval() {
        return applyApproval;
    }

    public ApplyResult getApplyResult() {
        return applyResult;
    }

    public ExecutionApproval getExecutionApproval() {
        return executionApproval;
    }

    public ExecutionResult getExecutionResult() {
        return executionResult;
    }

    public void startWorkflow() {
        this.status = AutoQaStatus.RUNNING;
        this.finishedAt = null;
    }

    public void startAgent(String agentName) {
        this.currentAgent = requireText(agentName, "agentName");
    }

    public void addAgentExecution(AgentExecutionResult result) {
        agentExecutions.add(Objects.requireNonNull(result, "result must not be null"));
    }

    public void addError(String error) {
        errors.add(requireText(error, "error"));
    }

    public void finishSuccessfully() {
        this.status = AutoQaStatus.FINISHED;
        this.currentAgent = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void finishWithError(String error) {
        addError(error);
        this.status = AutoQaStatus.ERROR;
        this.currentAgent = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void registerProjectDiscovery(ProjectDiscoveryResult result) {
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }
        if (this.projectDiscoveryResult != null) {
            throw new IllegalStateException("Project discovery already registered");
        }
        this.projectDiscoveryResult = result;
    }

    public void registerScenarioAnalysis(ScenarioAnalysisResult result) {
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }
        if (this.projectDiscoveryResult == null) {
            throw new IllegalStateException("Project discovery must be registered before scenario analysis");
        }
        if (this.scenarioAnalysisResult != null) {
            throw new IllegalStateException("Scenario analysis already registered");
        }
        this.scenarioAnalysisResult = result;
    }

    public void registerProjectKnowledge(ProjectKnowledgeResult result) {
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }
        if (this.projectDiscoveryResult == null) {
            throw new IllegalStateException("Project discovery must be registered before project knowledge");
        }
        if (this.scenarioAnalysisResult == null) {
            throw new IllegalStateException("Scenario analysis must be registered before project knowledge");
        }
        if (this.projectKnowledgeResult != null) {
            throw new IllegalStateException("Project knowledge already registered");
        }
        this.projectKnowledgeResult = result;
    }

    public void registerTechnicalPlan(TechnicalPlanResult result) {
        if (result == null) throw new NullPointerException("result must not be null");
        if (this.projectDiscoveryResult == null) throw new IllegalStateException("Project discovery must be registered before technical plan");
        if (this.scenarioAnalysisResult == null) throw new IllegalStateException("Scenario analysis must be registered before technical plan");
        if (this.projectKnowledgeResult == null) throw new IllegalStateException("Project knowledge must be registered before technical plan");
        if (this.technicalPlanResult != null) throw new IllegalStateException("Technical plan already registered");
        this.technicalPlanResult = result;
    }

    public void registerGeneration(GenerationResult result) {
        if (result == null) throw new NullPointerException("result must not be null");
        if (this.projectDiscoveryResult == null) throw new IllegalStateException("Project discovery must be registered before generation");
        if (this.scenarioAnalysisResult == null) throw new IllegalStateException("Scenario analysis must be registered before generation");
        if (this.projectKnowledgeResult == null) throw new IllegalStateException("Project knowledge must be registered before generation");
        if (this.technicalPlanResult == null) throw new IllegalStateException("Technical plan must be registered before generation");
        if (this.generationResult != null) throw new IllegalStateException("Generation already registered");
        this.generationResult = result;
    }

    public void registerCodeReview(CodeReviewResult result) {
        if (result == null) throw new NullPointerException("result must not be null");
        if (this.projectDiscoveryResult == null) throw new IllegalStateException("Project discovery must be registered before code review");
        if (this.scenarioAnalysisResult == null) throw new IllegalStateException("Scenario analysis must be registered before code review");
        if (this.projectKnowledgeResult == null) throw new IllegalStateException("Project knowledge must be registered before code review");
        if (this.technicalPlanResult == null) throw new IllegalStateException("Technical plan must be registered before code review");
        if (this.generationResult == null) throw new IllegalStateException("Generation must be registered before code review");
        if (this.codeReviewResult != null) throw new IllegalStateException("Code review already registered");
        this.codeReviewResult = result;
    }

    public void registerApplyApproval(ApplyApproval approval) {
        if (approval == null) throw new NullPointerException("approval must not be null");
        if (this.codeReviewResult == null) throw new IllegalStateException("Code review must be registered before apply approval");
        if (!approval.approved()) throw new IllegalStateException("ApplyApproval.approved must be true to register");
        if (this.applyApproval != null) throw new IllegalStateException("Apply approval already registered");
        this.applyApproval = approval;
    }

    public void registerApplyResult(ApplyResult result) {
        if (result == null) throw new NullPointerException("result must not be null");
        if (this.projectDiscoveryResult == null) throw new IllegalStateException("Project discovery must be registered before apply result");
        if (this.scenarioAnalysisResult == null) throw new IllegalStateException("Scenario analysis must be registered before apply result");
        if (this.projectKnowledgeResult == null) throw new IllegalStateException("Project knowledge must be registered before apply result");
        if (this.technicalPlanResult == null) throw new IllegalStateException("Technical plan must be registered before apply result");
        if (this.generationResult == null) throw new IllegalStateException("Generation must be registered before apply result");
        if (this.codeReviewResult == null) throw new IllegalStateException("Code review must be registered before apply result");
        if (this.applyApproval == null) throw new IllegalStateException("Apply approval must be registered before apply result");
        if (this.applyResult != null) throw new IllegalStateException("Apply result already registered");
        this.applyResult = result;
    }

    public void registerExecutionApproval(ExecutionApproval approval) {
        if (approval == null) throw new NullPointerException("approval must not be null");
        if (this.applyResult == null) throw new IllegalStateException("Apply result must be registered before execution approval");
        if (!approval.approved()) throw new IllegalStateException("ExecutionApproval.approved must be true to register");
        if (!approval.allowTestExecution()) throw new IllegalStateException("ExecutionApproval.allowTestExecution must be true to register");
        if (this.executionApproval != null) throw new IllegalStateException("Execution approval already registered");
        this.executionApproval = approval;
    }

    public void registerExecutionResult(ExecutionResult result) {
        if (result == null) throw new NullPointerException("result must not be null");
        if (this.projectDiscoveryResult == null) throw new IllegalStateException("Project discovery must be registered before execution result");
        if (this.scenarioAnalysisResult == null) throw new IllegalStateException("Scenario analysis must be registered before execution result");
        if (this.projectKnowledgeResult == null) throw new IllegalStateException("Project knowledge must be registered before execution result");
        if (this.technicalPlanResult == null) throw new IllegalStateException("Technical plan must be registered before execution result");
        if (this.generationResult == null) throw new IllegalStateException("Generation must be registered before execution result");
        if (this.codeReviewResult == null) throw new IllegalStateException("Code review must be registered before execution result");
        if (this.applyResult == null) throw new IllegalStateException("Apply result must be registered before execution result");
        if (this.executionApproval == null) throw new IllegalStateException("Execution approval must be registered before execution result");
        if (this.executionResult != null) throw new IllegalStateException("Execution result already registered");
        this.executionResult = result;
    }

    private String requireText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }
}
