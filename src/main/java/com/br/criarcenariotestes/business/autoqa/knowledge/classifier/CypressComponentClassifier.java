package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(10)
public class CypressComponentClassifier implements ProjectComponentClassifier {

    @Override
    public boolean supports(ProjectDiscoveryResult discovery) {
        return discovery.getAutomationFramework() == AutomationFramework.CYPRESS
                || discovery.getDetectedFrameworks().contains(AutomationFramework.CYPRESS);
    }

    @Override
    public ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        ComponentType type = determineType(metadata);
        boolean reusable = type != ComponentType.TEST && type != ComponentType.UNKNOWN && type != ComponentType.RESOURCE && type != ComponentType.TEST_DATA;
        List<String> tags = new ArrayList<>(metadata.tags());
        tags.add("CYPRESS");
        if (metadata.relativePath().contains("/commands") || metadata.relativePath().toLowerCase().contains("command")) {
            tags.add("CYPRESS_CUSTOM_COMMAND");
        }
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
                metadata.warnings()
        );
    }

    private ComponentType determineType(SourceMetadataParser.SourceMetadata metadata) {
        String path = metadata.relativePath().toLowerCase();
        String name = metadata.name().toLowerCase();
        if (metadata.testComponent() || name.endsWith("cy") || path.contains("/cypress/") || path.contains("/tests/")) {
            return ComponentType.TEST;
        }
        if ((name.endsWith("page") || path.contains("/pages/") || path.contains("/pageobjects/"))
                && (!metadata.declaredMethods().isEmpty() || metadata.imports().stream().anyMatch(value -> value.toLowerCase().contains("cypress")))) {
            return ComponentType.PAGE_OBJECT;
        }
        if (path.contains("/fixtures/") || metadata.tags().contains("FIXTURE")) {
            return ComponentType.FIXTURE;
        }
        if (path.contains("/commands") || name.contains("command")) {
            return ComponentType.UTILITY;
        }
        if (path.contains("/support/") || path.contains("/helpers/") || path.contains("/utils/")) {
            return ComponentType.HELPER;
        }
        if (path.contains("/api/") || path.contains("/client/")) {
            return ComponentType.API_CLIENT;
        }
        return ComponentType.UNKNOWN;
    }
}
