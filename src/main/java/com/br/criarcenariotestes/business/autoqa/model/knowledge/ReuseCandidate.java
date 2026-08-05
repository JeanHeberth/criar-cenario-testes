package com.br.criarcenariotestes.business.autoqa.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ReuseCandidate(
        String componentPath,
        ComponentType type,
        String reason,
        ReuseConfidence confidence,
        List<String> matchingTerms
) {
    public ReuseCandidate {
        componentPath = normalize(componentPath);
        type = type == null ? ComponentType.UNKNOWN : type;
        reason = normalize(reason);
        confidence = confidence == null ? ReuseConfidence.UNKNOWN : confidence;
        matchingTerms = matchingTerms == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(matchingTerms));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
