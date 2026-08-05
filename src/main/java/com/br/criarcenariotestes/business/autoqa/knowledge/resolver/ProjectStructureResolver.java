package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

@Component
public class ProjectStructureResolver {

    public ProjectStructure resolve(KnowledgeScanResult scanResult, List<ProjectComponent> components) {
        Objects.requireNonNull(scanResult, "scanResult must not be null");
        Objects.requireNonNull(components, "components must not be null");

        TreeSet<String> testDirectories = new TreeSet<>();
        TreeSet<String> sourceDirectories = new TreeSet<>();
        TreeSet<String> ignoredDirectories = new TreeSet<>();

        for (ProjectComponent component : components) {
            String directory = directoryOf(component.relativePath());
            if (directory == null) {
                continue;
            }
            if (isTestComponent(component)) {
                testDirectories.add(directory);
            } else {
                sourceDirectories.add(directory);
            }
        }

        for (String ignoredPath : scanResult.ignoredPaths()) {
            String directory = directoryOf(ignoredPath);
            if (directory != null) {
                ignoredDirectories.add(directory);
            }
        }

        return new ProjectStructure(
                List.copyOf(testDirectories),
                List.copyOf(sourceDirectories),
                List.copyOf(ignoredDirectories)
        );
    }

    private boolean isTestComponent(ProjectComponent component) {
        return component.testComponent()
                || component.type() == ComponentType.TEST
                || component.type() == ComponentType.BASE_TEST
                || component.type() == ComponentType.KEYWORD;
    }

    private String directoryOf(String relativePath) {
        int index = relativePath.lastIndexOf('/');
        if (index <= 0) {
            return null;
        }
        return relativePath.substring(0, index);
    }

    public record ProjectStructure(
            List<String> testDirectories,
            List<String> sourceDirectories,
            List<String> ignoredDirectories
    ) {
        public ProjectStructure {
            testDirectories = testDirectories == null ? List.of() : List.copyOf(testDirectories);
            sourceDirectories = sourceDirectories == null ? List.of() : List.copyOf(sourceDirectories);
            ignoredDirectories = ignoredDirectories == null ? List.of() : List.copyOf(ignoredDirectories);
        }
    }
}
