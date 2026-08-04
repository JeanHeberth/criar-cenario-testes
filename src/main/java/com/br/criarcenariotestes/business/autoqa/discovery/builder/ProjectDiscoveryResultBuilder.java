package com.br.criarcenariotestes.business.autoqa.discovery.builder;

import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.AutomationLanguageResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.BuildToolResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.DiscoveryConfidenceResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.PackageManagerResolver;
import com.br.criarcenariotestes.business.autoqa.discovery.resolver.TestingFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProjectDiscoveryResultBuilder {

    private static final List<AutomationFramework> DETECTION_ORDER = List.of(
            AutomationFramework.PLAYWRIGHT,
            AutomationFramework.CYPRESS,
            AutomationFramework.ROBOT_FRAMEWORK,
            AutomationFramework.SELENIDE,
            AutomationFramework.SELENIUM,
            AutomationFramework.REST_ASSURED
    );

    private final AutomationFrameworkResolver automationFrameworkResolver;
    private final AutomationLanguageResolver automationLanguageResolver;
    private final PackageManagerResolver packageManagerResolver;
    private final BuildToolResolver buildToolResolver;
    private final TestingFrameworkResolver testingFrameworkResolver;
    private final DiscoveryConfidenceResolver discoveryConfidenceResolver;

    public ProjectDiscoveryResultBuilder(AutomationFrameworkResolver automationFrameworkResolver,
                                         AutomationLanguageResolver automationLanguageResolver,
                                         PackageManagerResolver packageManagerResolver,
                                         BuildToolResolver buildToolResolver,
                                         TestingFrameworkResolver testingFrameworkResolver,
                                         DiscoveryConfidenceResolver discoveryConfidenceResolver) {
        this.automationFrameworkResolver = automationFrameworkResolver;
        this.automationLanguageResolver = automationLanguageResolver;
        this.packageManagerResolver = packageManagerResolver;
        this.buildToolResolver = buildToolResolver;
        this.testingFrameworkResolver = testingFrameworkResolver;
        this.discoveryConfidenceResolver = discoveryConfidenceResolver;
    }

    public ProjectDiscoveryResult build(Path normalizedProjectPath,
                                        ParsedProjectFiles parsedProjectFiles,
                                        List<FrameworkDetection> detections) {
        Map<AutomationFramework, FrameworkDetection> detectionsByFramework = indexDetections(detections);
        LinkedHashSet<AutomationFramework> detectedFrameworks = collectDetectedFrameworks(detectionsByFramework);
        LinkedHashSet<String> libraries = collectLibraries(detectionsByFramework);
        List<String> warnings = collectWarnings(parsedProjectFiles, detectionsByFramework);

        if (detectedFrameworks.contains(AutomationFramework.PLAYWRIGHT)
                && detectedFrameworks.contains(AutomationFramework.CYPRESS)) {
            warnings.add("Ambiguidade: Playwright e Cypress detectados no mesmo projeto");
        }
        if (automationFrameworkResolver.hasHybridWebAndApi(detectedFrameworks)) {
            warnings.add("Projeto híbrido web e API detectado");
        }
        if (multipleNodeLockfiles(parsedProjectFiles)) {
            warnings.add("Múltiplos lockfiles Node encontrados");
        }

        PackageManager packageManager = packageManagerResolver.resolve(parsedProjectFiles);
        BuildTool buildTool = buildToolResolver.resolve(parsedProjectFiles, packageManager);
        Set<TestingFramework> testingFrameworks = testingFrameworkResolver.resolve(detectionsByFramework, parsedProjectFiles);
        AutomationFramework automationFramework = automationFrameworkResolver.resolve(detectedFrameworks, warnings);
        AutomationLanguage language = automationLanguageResolver.resolve(automationFramework, detectionsByFramework);
        DiscoveryConfidence confidence = discoveryConfidenceResolver.resolve(automationFramework, detectionsByFramework, warnings);
        String configurationFile = selectConfigurationFile(parsedProjectFiles);

        return new ProjectDiscoveryResult(
                normalizedProjectPath,
                automationFramework,
                language,
                packageManager,
                buildTool,
                testingFrameworks,
                detectedFrameworks,
                new ArrayList<>(libraries),
                configurationFile,
                new ArrayList<>(parsedProjectFiles.evidenceFiles()),
                warnings,
                confidence,
                true
        );
    }

    private Map<AutomationFramework, FrameworkDetection> indexDetections(List<FrameworkDetection> detections) {
        Map<AutomationFramework, FrameworkDetection> indexed = new EnumMap<>(AutomationFramework.class);
        for (FrameworkDetection detection : detections) {
            indexed.put(detection.framework(), detection);
        }
        return indexed;
    }

    private LinkedHashSet<AutomationFramework> collectDetectedFrameworks(Map<AutomationFramework, FrameworkDetection> detections) {
        LinkedHashSet<AutomationFramework> detected = new LinkedHashSet<>();
        for (AutomationFramework framework : DETECTION_ORDER) {
            FrameworkDetection detection = detections.get(framework);
            if (detection != null && detection.detected()) {
                detected.add(framework);
            }
        }
        return detected;
    }

    private LinkedHashSet<String> collectLibraries(Map<AutomationFramework, FrameworkDetection> detections) {
        LinkedHashSet<String> libraries = new LinkedHashSet<>();
        for (AutomationFramework framework : DETECTION_ORDER) {
            FrameworkDetection detection = detections.get(framework);
            if (detection != null && detection.detected()) {
                libraries.addAll(detection.libraries());
            }
        }
        return libraries;
    }

    private List<String> collectWarnings(ParsedProjectFiles parsedProjectFiles,
                                         Map<AutomationFramework, FrameworkDetection> detectionsByFramework) {
        List<String> warnings = new ArrayList<>(parsedProjectFiles.warnings());
        for (AutomationFramework framework : DETECTION_ORDER) {
            FrameworkDetection detection = detectionsByFramework.get(framework);
            if (detection != null && detection.detected()) {
                warnings.addAll(detection.warnings());
            }
        }
        return warnings;
    }

    private boolean multipleNodeLockfiles(ParsedProjectFiles parsedProjectFiles) {
        int count = 0;
        if (parsedProjectFiles.packageLock()) {
            count++;
        }
        if (parsedProjectFiles.yarnLock()) {
            count++;
        }
        if (parsedProjectFiles.pnpmLock()) {
            count++;
        }
        return count > 1;
    }

    private String selectConfigurationFile(ParsedProjectFiles parsedProjectFiles) {
        if (parsedProjectFiles.playwrightConfig() != null) {
            return parsedProjectFiles.playwrightConfig();
        }
        if (parsedProjectFiles.cypressConfig() != null) {
            return parsedProjectFiles.cypressConfig();
        }
        if (parsedProjectFiles.robotYamlPath() != null) {
            return parsedProjectFiles.robotYamlPath();
        }
        if (parsedProjectFiles.mavenPomPath() != null) {
            return parsedProjectFiles.mavenPomPath();
        }
        if (parsedProjectFiles.gradleBuildPath() != null) {
            return parsedProjectFiles.gradleBuildPath();
        }
        if (parsedProjectFiles.requirementsPath() != null) {
            return parsedProjectFiles.requirementsPath();
        }
        if (parsedProjectFiles.pyprojectPath() != null) {
            return parsedProjectFiles.pyprojectPath();
        }
        if (parsedProjectFiles.poetryLockPath() != null) {
            return parsedProjectFiles.poetryLockPath();
        }
        if (parsedProjectFiles.packageJsonPath() != null) {
            return parsedProjectFiles.packageJsonPath();
        }
        if (!parsedProjectFiles.evidenceFiles().isEmpty()) {
            return parsedProjectFiles.evidenceFiles().get(0);
        }
        return null;
    }
}
