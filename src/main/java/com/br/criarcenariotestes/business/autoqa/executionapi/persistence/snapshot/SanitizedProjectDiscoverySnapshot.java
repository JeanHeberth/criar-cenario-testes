package com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot;

import com.br.criarcenariotestes.business.autoqa.model.discovery.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Espelha ProjectDiscoveryResult omitindo normalizedProjectPath (nunca
 * persistido). Na reidratação, o path real vem do AutoQaExecutionDocument
 * (nunca do snapshot).
 */
public record SanitizedProjectDiscoverySnapshot(
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
    public static SanitizedProjectDiscoverySnapshot from(ProjectDiscoveryResult result) {
        return new SanitizedProjectDiscoverySnapshot(
                result.automationFramework(), result.language(), result.packageManager(), result.buildTool(),
                result.testingFrameworks(), result.detectedFrameworks(), result.libraries(), result.configurationFile(),
                result.evidenceFiles(), result.warnings(), result.confidence(), result.valid());
    }

    public ProjectDiscoveryResult toResult(Path projectPath) {
        return new ProjectDiscoveryResult(projectPath, automationFramework, language, packageManager, buildTool,
                testingFrameworks, detectedFrameworks, libraries, configurationFile, evidenceFiles, warnings,
                confidence, valid);
    }
}
