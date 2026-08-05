package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(0)
public class PlaywrightComponentClassifier implements ProjectComponentClassifier {

    @Override
    public boolean supports(ProjectDiscoveryResult discovery) {
        return discovery.getAutomationFramework() == AutomationFramework.PLAYWRIGHT
                || discovery.getDetectedFrameworks().contains(AutomationFramework.PLAYWRIGHT);
    }

    @Override
    public ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        ComponentType type = determineType(metadata);
        boolean reusable = type != ComponentType.TEST && type != ComponentType.UNKNOWN && type != ComponentType.RESOURCE && type != ComponentType.TEST_DATA;
        List<String> warnings = new ArrayList<>(metadata.warnings());
        if (type == ComponentType.UNKNOWN) {
            warnings.add("Playwright sem evidência suficiente");
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
                tags(metadata, "PLAYWRIGHT"),
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
        if ((name.endsWith("page") || path.contains("/pages/") || path.contains("/pageobjects/") || metadata.tags().contains("PAGE_OBJECT_EVIDENCE"))
                && (hasEvidence(metadata) || metadata.tags().contains("PAGE_OBJECT_EVIDENCE"))) {
            return ComponentType.PAGE_OBJECT;
        }
        if (path.contains("/fixtures/") || metadata.tags().contains("FIXTURE")) {
            return ComponentType.FIXTURE;
        }
        if (path.contains("/support/") || path.contains("/commands/") || name.contains("command")) {
            return ComponentType.HELPER;
        }
        if (path.contains("/helpers/") || path.contains("/utils/") || metadata.tags().contains("HELPER_EVIDENCE")) {
            return ComponentType.HELPER;
        }
        if (path.contains("/api/") || path.contains("/client/") || metadata.tags().contains("API_CLIENT_EVIDENCE")) {
            return ComponentType.API_CLIENT;
        }
        if (metadata.language() == SourceLanguage.UNKNOWN && path.endsWith(".json")) {
            return ComponentType.RESOURCE;
        }
        return ComponentType.UNKNOWN;
    }

    private boolean hasEvidence(SourceMetadataParser.SourceMetadata metadata) {
        return !metadata.declaredMethods().isEmpty()
                || metadata.imports().stream().anyMatch(value -> value.toLowerCase().contains("page") || value.toLowerCase().contains("locator"));
    }

    private List<String> tags(SourceMetadataParser.SourceMetadata metadata, String framework) {
        List<String> tags = new ArrayList<>(metadata.tags());
        tags.add(framework);
        return tags.stream().distinct().toList();
    }
}
