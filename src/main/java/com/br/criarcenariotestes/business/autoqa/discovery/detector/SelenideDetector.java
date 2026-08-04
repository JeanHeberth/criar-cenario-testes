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
public class SelenideDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.SELENIDE;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean pom = project.mavenDependencies().stream().anyMatch(dep -> dep.contains("com.codeborne:selenide"));
        boolean gradle = project.gradleContents().stream().anyMatch(content -> content.contains("com.codeborne:selenide"));
        if (!pom && !gradle) {
            return FrameworkDetection.notDetected(framework());
        }

        LinkedHashSet<String> libraries = new LinkedHashSet<>();
        libraries.add("SELENIDE");
        if (project.mavenDependencies().stream().anyMatch(dep -> dep.contains("org.seleniumhq.selenium"))
                || project.gradleContents().stream().anyMatch(content -> content.contains("selenium"))) {
            libraries.add("SELENIUM");
        }

        Set<String> evidence = new LinkedHashSet<>();
        if (pom && project.mavenPomPath() != null) {
            evidence.add(project.mavenPomPath());
        }
        if (gradle && project.gradleBuildPath() != null) {
            evidence.add(project.gradleBuildPath());
        }

        return new FrameworkDetection(
                framework(),
                true,
                AutomationLanguage.JAVA,
                (project.mavenPom() || project.gradleBuild()) ? DiscoveryConfidence.HIGH : DiscoveryConfidence.MEDIUM,
                Set.of(),
                libraries,
                List.copyOf(evidence),
                List.of()
        );
    }
}
