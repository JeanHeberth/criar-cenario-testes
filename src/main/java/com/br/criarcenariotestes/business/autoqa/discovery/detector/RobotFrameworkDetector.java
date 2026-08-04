package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class RobotFrameworkDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.ROBOT_FRAMEWORK;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean robotFile = !project.robotFiles().isEmpty();
        boolean robotYaml = project.robotYaml();
        boolean requirements = project.requirementsContents().stream().anyMatch(content -> content.contains("robotframework"));
        boolean pyproject = project.pyprojectContents().stream().anyMatch(content -> content.contains("robotframework"));
        boolean poetry = project.poetryContents().stream().anyMatch(content -> content.contains("robotframework"));

        if (!robotFile && !robotYaml && !requirements && !pyproject && !poetry) {
            return FrameworkDetection.notDetected(framework());
        }

        DiscoveryConfidence confidence;
        if (robotYaml && (requirements || pyproject || poetry)) {
            confidence = DiscoveryConfidence.HIGH;
        } else if (requirements || pyproject || poetry) {
            confidence = DiscoveryConfidence.MEDIUM;
        } else {
            confidence = DiscoveryConfidence.LOW;
        }

        Set<String> evidence = new LinkedHashSet<>(project.robotFiles());
        if (project.robotYamlPath() != null) {
            evidence.add(project.robotYamlPath());
        }
        if (requirements && project.requirementsPath() != null) {
            evidence.add(project.requirementsPath());
        }
        if (pyproject && project.pyprojectPath() != null) {
            evidence.add(project.pyprojectPath());
        }
        if (poetry && project.poetryLockPath() != null) {
            evidence.add(project.poetryLockPath());
        }

        return new FrameworkDetection(
                framework(),
                true,
                AutomationLanguage.ROBOT,
                confidence,
                Set.of(TestingFramework.ROBOT),
                Set.of("ROBOT_FRAMEWORK"),
                List.copyOf(evidence),
                List.of()
        );
    }
}
