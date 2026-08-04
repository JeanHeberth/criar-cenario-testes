package com.br.criarcenariotestes.business.autoqa.model.discovery;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ProjectDiscoveryResult(
        Path normalizedProjectPath,
        AutomationFramework automationFramework,
        AutomationLanguage language,
        PackageManager packageManager,
        BuildTool buildTool,
        Set<TestingFramework> testingFrameworks,
        Set<AutomationFramework> detectedFrameworks,
        List<String> libraries,
        String configurationFile,
        List<String> evidenceFiles,
        List<String> warnings,
        DiscoveryConfidence confidence,
        boolean valid
) {

    public ProjectDiscoveryResult {
        normalizedProjectPath = Objects.requireNonNull(normalizedProjectPath, "normalizedProjectPath must not be null");
        automationFramework = automationFramework == null ? AutomationFramework.UNKNOWN : automationFramework;
        language = language == null ? AutomationLanguage.UNKNOWN : language;
        packageManager = packageManager == null ? PackageManager.UNKNOWN : packageManager;
        buildTool = buildTool == null ? BuildTool.UNKNOWN : buildTool;
        testingFrameworks = testingFrameworks == null ? Set.of() : Set.copyOf(testingFrameworks);
        detectedFrameworks = detectedFrameworks == null ? Set.of() : Set.copyOf(detectedFrameworks);
        libraries = libraries == null ? List.of() : List.copyOf(libraries);
        evidenceFiles = evidenceFiles == null ? List.of() : List.copyOf(evidenceFiles);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        confidence = confidence == null ? DiscoveryConfidence.UNKNOWN : confidence;
    }

    public Path getNormalizedProjectPath() {
        return normalizedProjectPath;
    }

    public AutomationFramework getAutomationFramework() {
        return automationFramework;
    }

    public AutomationLanguage getLanguage() {
        return language;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }

    public Set<TestingFramework> getTestingFrameworks() {
        return testingFrameworks;
    }

    public Set<AutomationFramework> getDetectedFrameworks() {
        return detectedFrameworks;
    }

    public List<String> getLibraries() {
        return libraries;
    }

    public String getConfigurationFile() {
        return configurationFile;
    }

    public List<String> getEvidenceFiles() {
        return evidenceFiles;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public DiscoveryConfidence getConfidence() {
        return confidence;
    }

    public boolean isValid() {
        return valid;
    }
}
