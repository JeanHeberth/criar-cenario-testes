package com.br.criarcenariotestes.business.autoqa.model.knowledge;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ProjectKnowledgeResult(
        Path normalizedProjectPath,
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
    public ProjectKnowledgeResult {
        normalizedProjectPath = Objects.requireNonNull(normalizedProjectPath, "normalizedProjectPath must not be null");
        components = copyList(components);
        tests = copyList(tests);
        pageObjects = copyList(pageObjects);
        fixtures = copyList(fixtures);
        helpers = copyList(helpers);
        apiClients = copyList(apiClients);
        models = copyList(models);
        resources = copyList(resources);
        reuseCandidates = copyList(reuseCandidates);
        namingConvention = namingConvention == null ? new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN) : namingConvention;
        testDirectories = copyList(testDirectories);
        sourceDirectories = copyList(sourceDirectories);
        ignoredDirectories = copyList(ignoredDirectories);
        warnings = copyList(warnings);
        status = status == null ? KnowledgeStatus.EMPTY : status;
    }

    private static <T> List<T> copyList(List<T> values) {
        if (values == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
