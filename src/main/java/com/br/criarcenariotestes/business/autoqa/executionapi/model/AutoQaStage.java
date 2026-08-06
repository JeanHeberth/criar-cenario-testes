package com.br.criarcenariotestes.business.autoqa.executionapi.model;

import java.util.Arrays;

/**
 * As 10 etapas do workflow Auto QA, na mesma ordem dos @Order das Fases 1-11
 * (DISCOVERY=0 .. LEARNING=90). agentName() é o valor real retornado por
 * AutoQaAgent.getName() de cada agente — usado por AutoQaStageExecutor para
 * localizar o agente correspondente na lista injetada pelo Spring.
 */
public enum AutoQaStage {
    DISCOVERY("project-discovery"),
    SCENARIO_ANALYSIS("scenario-analysis"),
    PROJECT_KNOWLEDGE("project-knowledge"),
    PLANNING("planning"),
    GENERATION("generation"),
    REVIEW("review"),
    APPLY("apply"),
    EXECUTION("execute"),
    FAILURE_ANALYSIS("failure-analysis"),
    LEARNING("learning");

    private final String agentName;

    AutoQaStage(String agentName) {
        this.agentName = agentName;
    }

    public String agentName() {
        return agentName;
    }

    public static AutoQaStage fromAgentName(String agentName) {
        return Arrays.stream(values())
                .filter(stage -> stage.agentName.equals(agentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown agent name: " + agentName));
    }
}
