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
@Order(20)
public class JavaComponentClassifier implements ProjectComponentClassifier {

    @Override
    public boolean supports(ProjectDiscoveryResult discovery) {
        return discovery.getAutomationFramework() == AutomationFramework.SELENIDE
                || discovery.getAutomationFramework() == AutomationFramework.SELENIUM
                || discovery.getAutomationFramework() == AutomationFramework.REST_ASSURED
                || discovery.getDetectedFrameworks().contains(AutomationFramework.SELENIDE)
                || discovery.getDetectedFrameworks().contains(AutomationFramework.SELENIUM)
                || discovery.getDetectedFrameworks().contains(AutomationFramework.REST_ASSURED);
    }

    @Override
    public ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        ComponentType type = determineType(metadata);
        boolean reusable = type != ComponentType.TEST && type != ComponentType.UNKNOWN && type != ComponentType.RESOURCE && type != ComponentType.TEST_DATA;
        List<String> tags = new ArrayList<>(metadata.tags());
        tags.add("JAVA");
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
        if (name.endsWith("basetest") || metadata.hierarchy().stream().anyMatch(value -> value.toLowerCase().contains("basetest"))) {
            return ComponentType.BASE_TEST;
        }
        if (metadata.testComponent() || name.endsWith("test") || path.contains("/src/test/") || metadata.tags().contains("TEST")) {
            return ComponentType.TEST;
        }
        if ((name.endsWith("page") || path.contains("/page/") || path.contains("/pages/") || metadata.tags().contains("PAGE_OBJECT_EVIDENCE"))
                && (!metadata.declaredMethods().isEmpty() || metadata.imports().stream().anyMatch(value -> value.contains("WebElement") || value.contains("SelenideElement") || value.contains("By")))) {
            return ComponentType.PAGE_OBJECT;
        }
        if (name.endsWith("client") || path.contains("/client/") || metadata.tags().contains("API_CLIENT_EVIDENCE") || metadata.imports().stream().anyMatch(value -> value.contains("RestAssured"))) {
            return ComponentType.API_CLIENT;
        }
        if (path.contains("/fixtures/") || metadata.tags().contains("FIXTURE") || metadata.tags().contains("TEST_DATA")) {
            return path.endsWith(".json") || path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".properties")
                    ? ComponentType.TEST_DATA
                    : ComponentType.FIXTURE;
        }
        if (path.contains("/support/") || path.contains("/helpers/") || path.contains("/utils/")) {
            return ComponentType.HELPER;
        }
        if (name.endsWith("factory")) {
            return ComponentType.FACTORY;
        }
        if (name.endsWith("builder")) {
            return ComponentType.BUILDER;
        }
        if (name.endsWith("service")) {
            return ComponentType.SERVICE;
        }
        if (name.endsWith("dto")) {
            return ComponentType.DTO;
        }
        if (name.endsWith("model") || name.endsWith("entity") || path.contains("/model/") || path.contains("/entity/") || path.contains("/dto/")) {
            return ComponentType.MODEL;
        }
        if (metadata.annotations().stream().anyMatch(value -> value.contains("Configuration")) || path.contains("/config/")) {
            return ComponentType.CONFIGURATION;
        }
        if (metadata.annotations().stream().anyMatch(value -> value.contains("Before") || value.contains("After"))) {
            return ComponentType.HOOK;
        }
        return ComponentType.UNKNOWN;
    }
}
