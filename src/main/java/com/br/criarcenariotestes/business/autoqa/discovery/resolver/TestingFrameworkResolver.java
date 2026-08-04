package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TestingFrameworkResolver {

    private static final List<AutomationFramework> DETECTOR_ORDER = List.of(
            AutomationFramework.PLAYWRIGHT,
            AutomationFramework.CYPRESS,
            AutomationFramework.ROBOT_FRAMEWORK,
            AutomationFramework.SELENIDE,
            AutomationFramework.SELENIUM,
            AutomationFramework.REST_ASSURED
    );

    public Set<TestingFramework> resolve(Map<AutomationFramework, FrameworkDetection> detections,
                                         ParsedProjectFiles parsedProjectFiles) {
        LinkedHashSet<TestingFramework> frameworks = new LinkedHashSet<>();
        for (AutomationFramework framework : DETECTOR_ORDER) {
            FrameworkDetection detection = detections.get(framework);
            if (detection != null && detection.detected()) {
                frameworks.addAll(detection.testingFrameworks());
            }
        }

        if (contains(parsedProjectFiles.mavenDependencies(), "org.junit.jupiter:junit-jupiter")
                || contains(parsedProjectFiles.gradleContents(), "org.junit.jupiter:junit-jupiter")) {
            frameworks.add(TestingFramework.JUNIT_5);
        }
        if (contains(parsedProjectFiles.mavenDependencies(), "junit:junit")
                || contains(parsedProjectFiles.gradleContents(), "junit:junit")) {
            frameworks.add(TestingFramework.JUNIT_4);
        }
        if (contains(parsedProjectFiles.mavenDependencies(), "org.testng:testng")
                || contains(parsedProjectFiles.gradleContents(), "org.testng:testng")) {
            frameworks.add(TestingFramework.TESTNG);
        }
        if (contains(parsedProjectFiles.requirementsContents(), "pytest")
                || contains(parsedProjectFiles.pyprojectContents(), "pytest")) {
            frameworks.add(TestingFramework.PYTEST);
        }
        return frameworks;
    }

    private boolean contains(Set<String> values, String target) {
        return values.stream().anyMatch(value -> value.contains(target));
    }

    private boolean contains(List<String> values, String target) {
        return values.stream().anyMatch(value -> value.contains(target));
    }
}
