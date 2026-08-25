package com.br.criarcenariotestes.business.autoqa.model.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TechnicalPlanResult(
    String title,
    String strategy,
    List<PlannedFileAction> fileActions,
    List<PlannedComponent> components,
    List<ReuseDecision> reuseDecisions,
    List<PlanningRisk> risks,
    List<PlanningWarning> warnings,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            contentUsing = com.br.criarcenariotestes.business.autoqa.model.scenario.TextoFlexivelDeserializer.class)
    List<String> assumptions,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            contentUsing = com.br.criarcenariotestes.business.autoqa.model.scenario.TextoFlexivelDeserializer.class)
    List<String> constraints,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            contentUsing = com.br.criarcenariotestes.business.autoqa.model.scenario.TextoFlexivelDeserializer.class)
    List<String> requiredApprovals,
    PlanningStatus status,
    PlanningConfidence confidence,
    boolean valid
) {
    public TechnicalPlanResult {
        title = title == null ? null : title.trim();
        strategy = strategy == null ? null : strategy.trim();
        fileActions = fileActions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(fileActions));
        components = components == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(components));
        reuseDecisions = reuseDecisions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(reuseDecisions));
        risks = risks == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(risks));
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(warnings));
        assumptions = assumptions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(assumptions));
        constraints = constraints == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(constraints));
        requiredApprovals = requiredApprovals == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(requiredApprovals));
    }
}
