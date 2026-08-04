package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AutomationLanguageResolver {

    public AutomationLanguage resolve(AutomationFramework automationFramework,
                                      Map<AutomationFramework, FrameworkDetection> detections) {
        FrameworkDetection robot = detections.get(AutomationFramework.ROBOT_FRAMEWORK);
        if (robot != null && robot.detected()) {
            return AutomationLanguage.ROBOT;
        }

        FrameworkDetection playwright = detections.get(AutomationFramework.PLAYWRIGHT);
        FrameworkDetection cypress = detections.get(AutomationFramework.CYPRESS);
        FrameworkDetection selenium = detections.get(AutomationFramework.SELENIUM);

        if (automationFramework == AutomationFramework.PLAYWRIGHT && playwright != null) {
            return playwright.language();
        }
        if (automationFramework == AutomationFramework.CYPRESS && cypress != null) {
            return cypress.language();
        }
        if (automationFramework == AutomationFramework.SELENIDE || automationFramework == AutomationFramework.REST_ASSURED) {
            return AutomationLanguage.JAVA;
        }
        if (automationFramework == AutomationFramework.SELENIUM && selenium != null) {
            return selenium.language();
        }
        if (selenium != null && selenium.detected()) {
            return selenium.language();
        }
        if (playwright != null && playwright.detected() && playwright.language() != AutomationLanguage.UNKNOWN) {
            return playwright.language();
        }
        if (cypress != null && cypress.detected() && cypress.language() != AutomationLanguage.UNKNOWN) {
            return cypress.language();
        }
        return AutomationLanguage.UNKNOWN;
    }
}
