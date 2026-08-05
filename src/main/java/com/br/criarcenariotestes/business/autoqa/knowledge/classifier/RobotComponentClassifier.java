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
@Order(30)
public class RobotComponentClassifier implements ProjectComponentClassifier {

    @Override
    public boolean supports(ProjectDiscoveryResult discovery) {
        return discovery.getAutomationFramework() == AutomationFramework.ROBOT_FRAMEWORK
                || discovery.getDetectedFrameworks().contains(AutomationFramework.ROBOT_FRAMEWORK);
    }

    @Override
    public ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        ComponentType type = determineType(metadata);
        boolean reusable = type != ComponentType.TEST && type != ComponentType.UNKNOWN;
        List<String> tags = new ArrayList<>(metadata.tags());
        tags.add("ROBOT");
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
        if (path.endsWith(".resource")) {
            return ComponentType.RESOURCE;
        }
        if (metadata.testComponent() || metadata.tags().contains("TEST_CASES")) {
            return ComponentType.TEST;
        }
        if (metadata.tags().contains("KEYWORDS")) {
            return ComponentType.KEYWORD;
        }
        if (metadata.tags().contains("VARIABLES")) {
            return ComponentType.VARIABLE_FILE;
        }
        if (!metadata.imports().isEmpty()) {
            return ComponentType.HELPER;
        }
        return ComponentType.UNKNOWN;
    }
}
