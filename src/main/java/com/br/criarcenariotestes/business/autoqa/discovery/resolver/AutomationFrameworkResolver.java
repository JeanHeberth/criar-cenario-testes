package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AutomationFrameworkResolver {

    public AutomationFramework resolve(Set<AutomationFramework> detectedFrameworks, List<String> warnings) {
        if (detectedFrameworks.isEmpty()) {
            return AutomationFramework.UNKNOWN;
        }
        if (hasHybridWebAndApi(detectedFrameworks)) {
            return AutomationFramework.UNKNOWN;
        }
        if (detectedFrameworks.contains(AutomationFramework.PLAYWRIGHT)
                && detectedFrameworks.contains(AutomationFramework.CYPRESS)) {
            return AutomationFramework.UNKNOWN;
        }
        if (detectedFrameworks.contains(AutomationFramework.SELENIDE)) {
            return AutomationFramework.SELENIDE;
        }
        if (detectedFrameworks.contains(AutomationFramework.REST_ASSURED)) {
            return AutomationFramework.REST_ASSURED;
        }
        if (detectedFrameworks.contains(AutomationFramework.PLAYWRIGHT)) {
            return AutomationFramework.PLAYWRIGHT;
        }
        if (detectedFrameworks.contains(AutomationFramework.CYPRESS)) {
            return AutomationFramework.CYPRESS;
        }
        if (detectedFrameworks.contains(AutomationFramework.SELENIUM)) {
            return AutomationFramework.SELENIUM;
        }
        if (detectedFrameworks.contains(AutomationFramework.ROBOT_FRAMEWORK)) {
            return AutomationFramework.ROBOT_FRAMEWORK;
        }
        return AutomationFramework.UNKNOWN;
    }

    public boolean hasHybridWebAndApi(Set<AutomationFramework> detectedFrameworks) {
        boolean web = detectedFrameworks.contains(AutomationFramework.PLAYWRIGHT)
                || detectedFrameworks.contains(AutomationFramework.CYPRESS)
                || detectedFrameworks.contains(AutomationFramework.SELENIDE)
                || detectedFrameworks.contains(AutomationFramework.SELENIUM);
        boolean api = detectedFrameworks.contains(AutomationFramework.REST_ASSURED);
        return web && api;
    }
}
