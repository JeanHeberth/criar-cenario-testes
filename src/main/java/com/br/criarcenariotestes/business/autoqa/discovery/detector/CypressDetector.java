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
public class CypressDetector implements FrameworkDetector {

    @Override
    public AutomationFramework framework() {
        return AutomationFramework.CYPRESS;
    }

    @Override
    public FrameworkDetection detect(ParsedProjectFiles project) {
        boolean dependency = project.nodeDependencies().contains("cypress");
        boolean config = project.cypressConfig() != null;
        if (!dependency && !config) {
            return FrameworkDetection.notDetected(framework());
        }
        boolean ts = project.cypressConfigIsTs() || project.tsconfig();
        AutomationLanguage language = ts ? AutomationLanguage.TYPESCRIPT : (config ? AutomationLanguage.JAVASCRIPT : AutomationLanguage.UNKNOWN);
        DiscoveryConfidence confidence = config && dependency ? DiscoveryConfidence.HIGH : DiscoveryConfidence.MEDIUM;
        Set<String> evidence = new LinkedHashSet<>();
        if (project.cypressConfig() != null) {
            evidence.add(project.cypressConfig());
        }
        if (dependency && project.packageJsonPath() != null) {
            evidence.add(project.packageJsonPath());
        }
        return new FrameworkDetection(
                framework(),
                true,
                language,
                confidence,
                Set.of(TestingFramework.CYPRESS),
                Set.of("CYPRESS"),
                evidence.stream().toList(),
                List.of()
        );
    }
}
