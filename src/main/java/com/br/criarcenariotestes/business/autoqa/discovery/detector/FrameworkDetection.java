package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;

import java.util.List;
import java.util.Set;

public record FrameworkDetection(
        AutomationFramework framework,
        boolean detected,
        AutomationLanguage language,
        DiscoveryConfidence confidence,
        Set<TestingFramework> testingFrameworks,
        Set<String> libraries,
        List<String> evidenceFiles,
        List<String> warnings
) {
    public FrameworkDetection {
        framework = framework == null ? AutomationFramework.UNKNOWN : framework;
        language = language == null ? AutomationLanguage.UNKNOWN : language;
        confidence = confidence == null ? DiscoveryConfidence.UNKNOWN : confidence;
        testingFrameworks = testingFrameworks == null ? Set.of() : Set.copyOf(testingFrameworks);
        libraries = libraries == null ? Set.of() : Set.copyOf(libraries);
        evidenceFiles = evidenceFiles == null ? List.of() : List.copyOf(evidenceFiles);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static FrameworkDetection notDetected(AutomationFramework framework) {
        return new FrameworkDetection(framework, false, AutomationLanguage.UNKNOWN, DiscoveryConfidence.UNKNOWN, Set.of(), Set.of(), List.of(), List.of());
    }
}
