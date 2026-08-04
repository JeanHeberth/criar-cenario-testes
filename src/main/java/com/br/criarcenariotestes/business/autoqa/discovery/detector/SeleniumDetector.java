package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class SeleniumDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.SELENIUM;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean packageJson = project.nodeDependencies().contains("selenium-webdriver");
        boolean requirements = project.requirementsContents().stream().anyMatch(content -> content.contains("selenium"));
        boolean pyproject = project.pyprojectContents().stream().anyMatch(content -> content.contains("selenium"));
        boolean gradle = project.gradleContents().stream().anyMatch(content -> content.contains("org.seleniumhq.selenium"));
        boolean pom = project.mavenDependencies().stream().anyMatch(dep -> dep.contains("org.seleniumhq.selenium"));

        if (!packageJson && !requirements && !pyproject && !gradle && !pom) {
            return FrameworkDetection.notDetected(framework());
        }

        AutomationLanguage language;
        if (packageJson) {
            language = project.tsconfig() ? AutomationLanguage.TYPESCRIPT : AutomationLanguage.JAVASCRIPT;
        } else if (requirements || pyproject) {
            language = AutomationLanguage.PYTHON;
        } else {
            language = AutomationLanguage.JAVA;
        }

        Set<String> evidence = new LinkedHashSet<>();
        if (packageJson && project.packageJsonPath() != null) {
            evidence.add(project.packageJsonPath());
        }
        if (requirements && project.requirementsPath() != null) {
            evidence.add(project.requirementsPath());
        }
        if (pyproject && project.pyprojectPath() != null) {
            evidence.add(project.pyprojectPath());
        }
        if (pom && project.mavenPomPath() != null) {
            evidence.add(project.mavenPomPath());
        }
        if (gradle && project.gradleBuildPath() != null) {
            evidence.add(project.gradleBuildPath());
        }

        return new FrameworkDetection(
                framework(),
                true,
                language,
                (gradle || pom) ? DiscoveryConfidence.HIGH : DiscoveryConfidence.MEDIUM,
                Set.of(),
                Set.of("SELENIUM"),
                List.copyOf(evidence),
                List.of()
        );
    }
}
