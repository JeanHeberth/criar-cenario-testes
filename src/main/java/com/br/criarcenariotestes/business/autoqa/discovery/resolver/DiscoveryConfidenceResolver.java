package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DiscoveryConfidenceResolver {

    public DiscoveryConfidence resolve(AutomationFramework automationFramework,
                                       Map<AutomationFramework, FrameworkDetection> detections,
                                       List<String> warnings) {
        DiscoveryConfidence confidence = switch (automationFramework) {
            case PLAYWRIGHT -> confidenceOf(detections, AutomationFramework.PLAYWRIGHT);
            case CYPRESS -> confidenceOf(detections, AutomationFramework.CYPRESS);
            case ROBOT_FRAMEWORK -> confidenceOf(detections, AutomationFramework.ROBOT_FRAMEWORK);
            case SELENIDE -> confidenceOf(detections, AutomationFramework.SELENIDE);
            case SELENIUM -> confidenceOf(detections, AutomationFramework.SELENIUM);
            case REST_ASSURED -> confidenceOf(detections, AutomationFramework.REST_ASSURED);
            case UNKNOWN -> hasAnyDetected(detections) ? DiscoveryConfidence.LOW : DiscoveryConfidence.UNKNOWN;
            default -> hasAnyDetected(detections) ? DiscoveryConfidence.LOW : DiscoveryConfidence.UNKNOWN;
        };

        boolean ambiguous = warnings.stream()
                .map(warning -> warning.toLowerCase(Locale.ROOT))
                .anyMatch(warning -> warning.contains("ambig")
                        || warning.contains("híbr")
                        || warning.contains("lockfile"));
        return ambiguous ? degrade(confidence) : confidence;
    }

    private DiscoveryConfidence confidenceOf(Map<AutomationFramework, FrameworkDetection> detections,
                                             AutomationFramework framework) {
        FrameworkDetection detection = detections.get(framework);
        if (detection == null || !detection.detected()) {
            return DiscoveryConfidence.UNKNOWN;
        }
        return detection.confidence();
    }

    private boolean hasAnyDetected(Map<AutomationFramework, FrameworkDetection> detections) {
        return detections.values().stream().anyMatch(FrameworkDetection::detected);
    }

    private DiscoveryConfidence degrade(DiscoveryConfidence confidence) {
        return switch (confidence) {
            case HIGH -> DiscoveryConfidence.MEDIUM;
            case MEDIUM -> DiscoveryConfidence.LOW;
            case LOW, UNKNOWN -> confidence;
        };
    }
}
