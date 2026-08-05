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

@DisplayName("RobotComponentClassifier - Testes Unitários")
class RobotComponentClassifierTest {

    private final RobotComponentClassifier classifier = new RobotComponentClassifier();

    @Test
    @DisplayName("Deve classificar arquivo com test cases")
    void deveClassificarArquivoComTestCases() {
        var metadata = new SourceMetadataParser.SourceMetadata("tests/login.robot", "login", SourceLanguage.ROBOT, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of("TEST_CASES"), true, List.of());
        assertThat(classifier.classify(discovery(), metadata).type()).isEqualTo(ComponentType.TEST);
    }

    @Test
    @DisplayName("Deve classificar arquivo com keywords")
    void deveClassificarArquivoComKeywords() {
        var metadata = new SourceMetadataParser.SourceMetadata("resources/common.robot", "common", SourceLanguage.ROBOT, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of("KEYWORDS"), false, List.of());
        assertThat(classifier.classify(discovery(), metadata).type()).isEqualTo(ComponentType.KEYWORD);
    }

    @Test
    @DisplayName("Deve classificar resource")
    void deveClassificarResource() {
        var metadata = new SourceMetadataParser.SourceMetadata("resources/common.resource", "common", SourceLanguage.ROBOT, null, List.of(), List.of(), List.of(), List.of(), List.of("RESOURCE"), List.of("RESOURCE"), false, List.of());
        assertThat(classifier.classify(discovery(), metadata).type()).isEqualTo(ComponentType.RESOURCE);
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(Path.of("/tmp/project"), AutomationFramework.ROBOT_FRAMEWORK, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.ROBOT, com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager.PIP, com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool.ROBOT, Set.of(), Set.of(AutomationFramework.ROBOT_FRAMEWORK), List.of(), null, List.of(), List.of(), com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence.HIGH, true);
    }
}
