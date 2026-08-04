package com.br.criarcenariotestes.business.autoqa.context;

import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
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
