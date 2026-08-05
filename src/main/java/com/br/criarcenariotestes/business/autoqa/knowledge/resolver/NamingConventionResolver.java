package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

@Component
public class NamingConventionResolver {

    public NamingConvention resolve(List<ProjectComponent> components) {
        Objects.requireNonNull(components, "components must not be null");

        List<ProjectComponent> ordered = components.stream()
                .sorted(Comparator.comparing(ProjectComponent::relativePath))
                .toList();

        String testPattern = mostCommon(ordered.stream().filter(this::isTest).map(ProjectComponent::relativePath).toList(), this::inferTestPattern);
        String pagePattern = mostCommon(ordered.stream().filter(this::isPageObject).map(ProjectComponent::relativePath).toList(), this::inferPagePattern);
        String classPattern = inferClassPattern(ordered);
        String methodPattern = inferMethodPattern(ordered);
        String directoryPattern = inferDirectoryPattern(ordered);
        List<String> examples = ordered.stream().map(ProjectComponent::relativePath).limit(3).toList();
        ReuseConfidence confidence = examples.isEmpty() ? ReuseConfidence.UNKNOWN : confidenceFor(ordered, testPattern, pagePattern);

        return new NamingConvention(testPattern, pagePattern, classPattern, methodPattern, directoryPattern, examples, confidence);
    }

    private boolean isTest(ProjectComponent component) {
        return component.testComponent() || component.type() == ComponentType.TEST || component.relativePath().toLowerCase(Locale.ROOT).matches(".*(spec|test|cy)\\.(ts|tsx|js|jsx|java|robot)$");
    }

    private boolean isPageObject(ProjectComponent component) {
        String path = component.relativePath().toLowerCase(Locale.ROOT);
        return component.type() == ComponentType.PAGE_OBJECT || path.contains("/pages/") || path.contains("/pageobjects/") || component.name().endsWith("Page");
    }

    private String mostCommon(List<String> values, java.util.function.Function<String, String> inferer) {
        if (values.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String value : values) {
            String pattern = inferer.apply(value);
            if (pattern != null) {
                counts.merge(pattern, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String inferTestPattern(String relativePath) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        if (fileName.endsWith(".spec.ts") || fileName.endsWith(".spec.tsx") || fileName.endsWith(".spec.js") || fileName.endsWith(".spec.jsx")) {
            return "*.spec." + extension(fileName);
        }
        if (fileName.endsWith(".test.ts") || fileName.endsWith(".test.tsx") || fileName.endsWith(".test.js") || fileName.endsWith(".test.jsx")) {
            return "*.test." + extension(fileName);
        }
        if (fileName.endsWith(".cy.ts") || fileName.endsWith(".cy.js")) {
            return "*.cy." + extension(fileName);
        }
        if (fileName.endsWith("Test.java")) {
            return "*Test.java";
        }
        if (fileName.endsWith(".robot")) {
            return "*.robot";
        }
        return null;
    }

    private String inferPagePattern(String relativePath) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        if (fileName.endsWith("Page.ts") || fileName.endsWith("Page.tsx") || fileName.endsWith("Page.js") || fileName.endsWith("Page.jsx") || fileName.endsWith("Page.java")) {
            return "*Page" + suffix(fileName);
        }
        return null;
    }

    private String inferClassPattern(List<ProjectComponent> components) {
        long pascal = components.stream().filter(component -> component.name().matches("[A-Z][A-Za-z0-9]+")).count();
        if (pascal == 0) {
            return null;
        }
        return pascal >= Math.max(1, components.size() / 2) ? "PascalCase" : null;
    }

    private String inferMethodPattern(List<ProjectComponent> components) {
        long camel = components.stream()
                .flatMap(component -> component.declaredMethods().stream())
                .filter(method -> method.matches("[a-z][A-Za-z0-9]*"))
                .count();
        if (camel == 0) {
            return null;
        }
        return camel >= 2 ? "camelCase" : null;
    }

    private String inferDirectoryPattern(List<ProjectComponent> components) {
        TreeSet<String> directories = new TreeSet<>();
        for (ProjectComponent component : components) {
            int index = component.relativePath().lastIndexOf('/');
            if (index > 0) {
                directories.add(component.relativePath().substring(0, index));
            }
        }
        if (directories.isEmpty()) {
            return null;
        }
        return directories.first();
    }

    private ReuseConfidence confidenceFor(List<ProjectComponent> components, String testPattern, String pagePattern) {
        if (components.size() >= 3 && (testPattern != null || pagePattern != null)) {
            return ReuseConfidence.HIGH;
        }
        if (components.size() >= 2) {
            return ReuseConfidence.MEDIUM;
        }
        return ReuseConfidence.LOW;
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index + 1) : "";
    }

    private String suffix(String fileName) {
        int index = fileName.indexOf('.');
        return index >= 0 ? fileName.substring(index) : "";
    }
}
