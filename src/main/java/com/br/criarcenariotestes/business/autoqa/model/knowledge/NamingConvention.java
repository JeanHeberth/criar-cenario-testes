package com.br.criarcenariotestes.business.autoqa.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record NamingConvention(
        String testFilePattern,
        String pageObjectPattern,
        String classPattern,
        String methodPattern,
        String directoryPattern,
        List<String> examples,
        ReuseConfidence confidence
) {
    public NamingConvention {
        examples = examples == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(examples));
        confidence = confidence == null ? ReuseConfidence.UNKNOWN : confidence;
    }
}
