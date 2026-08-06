package com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Espelha ProjectKnowledgeResult omitindo normalizedProjectPath (nunca
 * persistido). Os demais campos (ProjectComponent.relativePath etc.) já são
 * garantidamente relativos pelas próprias fases anteriores.
 */
public record SanitizedProjectKnowledgeSnapshot(
        List<ProjectComponent> components,
        List<ProjectComponent> tests,
        List<ProjectComponent> pageObjects,
        List<ProjectComponent> fixtures,
        List<ProjectComponent> helpers,
        List<ProjectComponent> apiClients,
        List<ProjectComponent> models,
        List<ProjectComponent> resources,
        List<ReuseCandidate> reuseCandidates,
        NamingConvention namingConvention,
        List<String> testDirectories,
        List<String> sourceDirectories,
        List<String> ignoredDirectories,
        List<String> warnings,
        KnowledgeStatus status,
        boolean valid
) {
    public static SanitizedProjectKnowledgeSnapshot from(ProjectKnowledgeResult result) {
        return new SanitizedProjectKnowledgeSnapshot(
                result.components(), result.tests(), result.pageObjects(), result.fixtures(), result.helpers(),
                result.apiClients(), result.models(), result.resources(), result.reuseCandidates(),
                result.namingConvention(), result.testDirectories(), result.sourceDirectories(),
                result.ignoredDirectories(), result.warnings(), result.status(), result.valid());
    }

    public ProjectKnowledgeResult toResult(Path projectPath) {
        return new ProjectKnowledgeResult(projectPath, components, tests, pageObjects, fixtures, helpers, apiClients,
                models, resources, reuseCandidates, namingConvention, testDirectories, sourceDirectories,
                ignoredDirectories, warnings, status, valid);
    }
}
