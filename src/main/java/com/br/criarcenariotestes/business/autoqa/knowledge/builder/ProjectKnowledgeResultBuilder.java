package com.br.criarcenariotestes.business.autoqa.knowledge.builder;

import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ProjectStructureResolver;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ProjectKnowledgeResultBuilder {

    public ProjectKnowledgeResult build(Path normalizedProjectPath,
                                        List<ProjectComponent> components,
                                        List<ReuseCandidate> reuseCandidates,
                                        NamingConvention namingConvention,
                                        ProjectStructureResolver.ProjectStructure structure,
                                        List<String> warnings,
                                        KnowledgeStatus status,
                                        boolean valid) {
        Objects.requireNonNull(normalizedProjectPath, "normalizedProjectPath must not be null");
        Objects.requireNonNull(components, "components must not be null");
        Objects.requireNonNull(reuseCandidates, "reuseCandidates must not be null");
        Objects.requireNonNull(structure, "structure must not be null");

        List<ProjectComponent> sortedComponents = dedupeAndSort(components);
        List<ReuseCandidate> sortedCandidates = reuseCandidates.stream()
                .sorted(Comparator.comparing(ReuseCandidate::confidence).reversed().thenComparing(ReuseCandidate::componentPath))
                .toList();

        return new ProjectKnowledgeResult(
                normalizedProjectPath,
                sortedComponents,
                filter(sortedComponents, ComponentType.TEST),
                filter(sortedComponents, ComponentType.PAGE_OBJECT),
                filter(sortedComponents, ComponentType.FIXTURE),
                filter(sortedComponents, ComponentType.HELPER, ComponentType.UTILITY),
                filter(sortedComponents, ComponentType.API_CLIENT),
                filter(sortedComponents, ComponentType.MODEL, ComponentType.DTO),
                filter(sortedComponents, ComponentType.RESOURCE, ComponentType.TEST_DATA, ComponentType.VARIABLE_FILE),
                sortedCandidates,
                namingConvention,
                structure.testDirectories(),
                structure.sourceDirectories(),
                structure.ignoredDirectories(),
                warnings == null ? List.of() : List.copyOf(warnings),
                status,
                valid
        );
    }

    private List<ProjectComponent> dedupeAndSort(List<ProjectComponent> components) {
        Map<String, ProjectComponent> byKey = new LinkedHashMap<>();
        for (ProjectComponent component : components) {
            String key = component.relativePath() + "|" + component.type() + "|" + component.name();
            byKey.putIfAbsent(key, component);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(ProjectComponent::relativePath).thenComparing(ProjectComponent::name))
                .toList();
    }

    private List<ProjectComponent> filter(List<ProjectComponent> components, ComponentType... types) {
        List<ComponentType> accepted = List.of(types);
        return components.stream()
                .filter(component -> accepted.contains(component.type()))
                .toList();
    }
}
