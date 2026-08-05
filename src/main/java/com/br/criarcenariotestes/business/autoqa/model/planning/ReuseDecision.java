package com.br.criarcenariotestes.business.autoqa.model.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ReuseDecision(
    String componentPath,
    PlanComponentType componentType,
    boolean reuse,
    String reason,
    PlanningConfidence confidence,
    List<String> matchedTerms,
    List<String> limitations
) {
    public ReuseDecision {
        componentPath = componentPath == null ? null : componentPath.trim();
        reason = reason == null ? null : reason.trim();
        matchedTerms = matchedTerms == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(matchedTerms));
        limitations = limitations == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(limitations));
    }
}
