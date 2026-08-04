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
public class RestAssuredDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.REST_ASSURED;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean pom = project.mavenDependencies().stream().anyMatch(dep -> dep.contains("io.rest-assured:rest-assured"));
        boolean gradle = project.gradleContents().stream().anyMatch(content -> content.contains("io.rest-assured:rest-assured"));
        if (!pom && !gradle) {
            return FrameworkDetection.notDetected(framework());
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
                Set.of("REST_ASSURED"),
                List.copyOf(evidence),
                List.of()
        );
    }
}
