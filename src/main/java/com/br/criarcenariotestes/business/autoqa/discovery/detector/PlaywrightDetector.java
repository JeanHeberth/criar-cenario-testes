package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PlaywrightDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.PLAYWRIGHT;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean dependency = project.nodeDependencies().contains("@playwright/test");
        boolean config = project.playwrightConfig() != null;
        if (!dependency && !config) {
            return FrameworkDetection.notDetected(framework());
        }
        boolean ts = project.playwrightConfigIsTs() || project.tsconfig();
        AutomationLanguage language = ts ? AutomationLanguage.TYPESCRIPT : (config ? AutomationLanguage.JAVASCRIPT : AutomationLanguage.UNKNOWN);
        DiscoveryConfidence confidence = config && dependency ? DiscoveryConfidence.HIGH : DiscoveryConfidence.MEDIUM;
        Set<String> evidence = new LinkedHashSet<>();
        if (project.playwrightConfig() != null) {
            evidence.add(project.playwrightConfig());
        }
        if (dependency && project.packageJsonPath() != null) {
            evidence.add(project.packageJsonPath());
        }
        return new FrameworkDetection(
                framework(),
                true,
                language,
                confidence,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of("PLAYWRIGHT"),
                evidence.stream().toList(),
                List.of()
        );
    }
}
