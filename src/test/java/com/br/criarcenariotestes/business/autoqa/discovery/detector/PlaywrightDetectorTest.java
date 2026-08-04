package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaywrightDetector - Testes Unitários")
class PlaywrightDetectorTest {

    private final PlaywrightDetector detector = new PlaywrightDetector();

    @Test
    @DisplayName("Configuração e dependência devem resultar em HIGH")
    void configuracaoEDependenciaDevemResultarEmHigh() {
        ParsedProjectFiles parsed = new ParsedProjectFiles(
                java.util.List.of(), java.util.List.of(), java.util.Set.of("@playwright/test"), java.util.Set.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.Set.of(), true, false, false, false, false, false, false, true, false, false, false,
                "package.json", null, null, null, null, null, null, "playwright.config.ts", null, true, false,
                java.util.Set.of()
        );

        FrameworkDetection detection = detector.detect(parsed);

        assertThat(detection.detected()).isTrue();
        assertThat(detection.confidence()).isEqualTo(DiscoveryConfidence.HIGH);
        assertThat(detection.language()).isEqualTo(AutomationLanguage.TYPESCRIPT);
    }

    @Test
    @DisplayName("Apenas configuração deve resultar em MEDIUM")
    void apenasConfiguracaoDeveResultarEmMedium() {
        ParsedProjectFiles parsed = new ParsedProjectFiles(
                java.util.List.of(), java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.Set.of(), false, false, false, false, false, false, false, false, false, false, false,
                null, null, null, null, null, null, null, "playwright.config.js", null, false, false,
                java.util.Set.of()
        );

        FrameworkDetection detection = detector.detect(parsed);

        assertThat(detection.detected()).isTrue();
        assertThat(detection.confidence()).isEqualTo(DiscoveryConfidence.MEDIUM);
        assertThat(detection.language()).isEqualTo(AutomationLanguage.JAVASCRIPT);
    }

    @Test
    @DisplayName("Apenas dependência deve resultar em MEDIUM")
    void apenasDependenciaDeveResultarEmMedium() {
        ParsedProjectFiles parsed = new ParsedProjectFiles(
                java.util.List.of(), java.util.List.of(), java.util.Set.of("@playwright/test"), java.util.Set.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.Set.of(), true, false, false, false, false, false, false, false, false, false, false,
                "package.json", null, null, null, null, null, null, null, null, false, false,
                java.util.Set.of()
        );

        FrameworkDetection detection = detector.detect(parsed);

        assertThat(detection.detected()).isTrue();
        assertThat(detection.confidence()).isEqualTo(DiscoveryConfidence.MEDIUM);
        assertThat(detection.language()).isEqualTo(AutomationLanguage.UNKNOWN);
    }
}
