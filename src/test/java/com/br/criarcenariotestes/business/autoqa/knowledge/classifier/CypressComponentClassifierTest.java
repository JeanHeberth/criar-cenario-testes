package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CypressComponentClassifier - Testes Unitários")
class CypressComponentClassifierTest {

    private final CypressComponentClassifier classifier = new CypressComponentClassifier();

    @Test
    @DisplayName("Deve classificar cy como test")
    void deveClassificarCyComoTest() {
        assertThat(classifier.classify(discovery(), metadata("src/login.cy.ts", "login.cy", List.of("TEST"), true, List.of("test"))).type())
                .isEqualTo(ComponentType.TEST);
    }

    @Test
    @DisplayName("Deve classificar fixture")
    void deveClassificarFixture() {
        assertThat(classifier.classify(discovery(), metadata("src/fixtures/login.ts", "login", List.of("FIXTURE"), false, List.of("setup"))).type())
                .isEqualTo(ComponentType.FIXTURE);
    }

    @Test
    @DisplayName("Deve classificar support como helper")
    void deveClassificarSupportComoHelper() {
        assertThat(classifier.classify(discovery(), metadata("src/support/commands.ts", "commands", List.of(), false, List.of("click"))).type())
                .isEqualTo(ComponentType.UTILITY);
    }

    @Test
    @DisplayName("Deve detectar custom command")
    void deveDetectarCustomCommand() {
        assertThat(classifier.classify(discovery(), metadata("src/support/commands.ts", "commands", List.of(), false, List.of("click"))).tags())
                .contains("CYPRESS_CUSTOM_COMMAND");
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(Path.of("/tmp/project"), AutomationFramework.CYPRESS, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT, com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager.NPM, com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool.NPM, Set.of(), Set.of(AutomationFramework.CYPRESS), List.of(), null, List.of(), List.of(), com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence.HIGH, true);
    }

    private SourceMetadataParser.SourceMetadata metadata(String path, String name, List<String> tags, boolean testComponent, List<String> methods) {
        return new SourceMetadataParser.SourceMetadata(path, name, SourceLanguage.TYPESCRIPT, null, List.of(), methods, List.of(), List.of(), List.of(), tags, testComponent, List.of());
    }
}
