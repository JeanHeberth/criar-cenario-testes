package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(100)
public class GenericComponentClassifier implements ProjectComponentClassifier {

    @Override
    public boolean supports(ProjectDiscoveryResult discovery) {
        return true;
    }

    @Override
    public ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        ComponentType type = determineType(metadata);
        List<String> warnings = new ArrayList<>(metadata.warnings());
        if (type == ComponentType.UNKNOWN) {
            warnings.add("Componente genérico sem evidência suficiente");
        }
        boolean reusable = type != ComponentType.TEST && type != ComponentType.UNKNOWN && type != ComponentType.RESOURCE && type != ComponentType.TEST_DATA;
        List<String> tags = new ArrayList<>(metadata.tags());
        tags.add("GENERIC");
        return new ProjectComponent(
                metadata.relativePath(),
                metadata.name(),
                type,
                metadata.language(),
                metadata.packageName(),
                metadata.declaredClasses(),
                metadata.declaredMethods(),
                metadata.imports(),
                metadata.annotations(),
                tags.stream().distinct().toList(),
                metadata.testComponent() || type == ComponentType.TEST,
                reusable,
                warnings
        );
    }

    private ComponentType determineType(SourceMetadataParser.SourceMetadata metadata) {
        String path = metadata.relativePath().toLowerCase();
        String name = metadata.name().toLowerCase();
        if (metadata.testComponent() || name.matches(".*(spec|test|cy)$") || path.contains("/tests/") || path.contains("/test/")) {
            return ComponentType.TEST;
        }
        if ((name.endsWith("page") || path.contains("/pages/") || path.contains("/pageobjects/"))
                && (!metadata.declaredMethods().isEmpty() || metadata.imports().stream().anyMatch(value -> value.toLowerCase().contains("page") || value.toLowerCase().contains("locator")))) {
            return ComponentType.PAGE_OBJECT;
        }
        if (path.contains("/fixtures/") || metadata.tags().contains("FIXTURE") || metadata.tags().contains("TEST_DATA")) {
            return path.endsWith(".json") || path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".properties")
                    ? ComponentType.TEST_DATA
                    : ComponentType.FIXTURE;
        }
        if (path.endsWith(".resource")) {
            return ComponentType.RESOURCE;
        }
        if (path.contains("/helpers/") || path.contains("/utils/") || path.contains("/support/")) {
            return ComponentType.HELPER;
        }
        if (name.endsWith("client") || path.contains("/api/") || path.contains("/client/")) {
            return ComponentType.API_CLIENT;
        }
        if (path.contains("/models/")) {
            return ComponentType.MODEL;
        }
        if (path.contains("/factories/")) {
            return ComponentType.FACTORY;
        }
        if (path.contains("/builders/")) {
            return ComponentType.BUILDER;
        }
        if (path.contains("/config/")) {
            return ComponentType.CONFIGURATION;
        }
        if (path.endsWith(".robot")) {
            return ComponentType.KEYWORD;
        }
        return ComponentType.UNKNOWN;
    }
}
