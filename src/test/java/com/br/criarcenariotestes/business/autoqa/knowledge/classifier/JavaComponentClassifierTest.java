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

@DisplayName("JavaComponentClassifier - Testes Unitários")
class JavaComponentClassifierTest {

    private final JavaComponentClassifier classifier = new JavaComponentClassifier();

    @Test
    @DisplayName("Deve classificar test")
    void deveClassificarTest() {
        assertThat(classifier.classify(discovery(), metadata("src/test/java/LoginTest.java", "LoginTest", List.of("TEST"), true, List.of("open"))).type())
                .isEqualTo(ComponentType.TEST);
    }

    @Test
    @DisplayName("Deve classificar page object")
    void deveClassificarPageObject() {
        assertThat(classifier.classify(discovery(), metadata("src/test/java/LoginPage.java", "LoginPage", List.of("PAGE_OBJECT_EVIDENCE"), false, List.of("open"))).type())
                .isEqualTo(ComponentType.PAGE_OBJECT);
    }

    @Test
    @DisplayName("Deve classificar base test")
    void deveClassificarBaseTest() {
        var metadata = new SourceMetadataParser.SourceMetadata("src/test/java/BaseTest.java", "BaseTest", SourceLanguage.JAVA, null, List.of("BaseTest"), List.of(), List.of(), List.of(), List.of("BaseTest"), List.of(), false, List.of());
        assertThat(classifier.classify(discovery(), metadata).type()).isEqualTo(ComponentType.BASE_TEST);
    }

    @Test
    @DisplayName("Deve classificar api client rest assured")
    void deveClassificarApiClientRestAssured() {
        var metadata = new SourceMetadataParser.SourceMetadata("src/main/java/UserClient.java", "UserClient", SourceLanguage.JAVA, null, List.of("UserClient"), List.of(), List.of("io.restassured.RestAssured"), List.of(), List.of(), List.of("API_CLIENT_EVIDENCE"), false, List.of());
        assertThat(classifier.classify(discovery(), metadata).type()).isEqualTo(ComponentType.API_CLIENT);
    }

    @Test
    @DisplayName("Deve classificar model")
    void deveClassificarModel() {
        assertThat(classifier.classify(discovery(), metadata("src/main/java/UserModel.java", "UserModel", List.of(), false, List.of("load"))).type())
                .isEqualTo(ComponentType.MODEL);
    }

    @Test
    @DisplayName("Deve classificar factory")
    void deveClassificarFactory() {
        assertThat(classifier.classify(discovery(), metadata("src/main/java/UserFactory.java", "UserFactory", List.of(), false, List.of("build"))).type())
                .isEqualTo(ComponentType.FACTORY);
    }

    @Test
    @DisplayName("Deve classificar builder")
    void deveClassificarBuilder() {
        assertThat(classifier.classify(discovery(), metadata("src/main/java/UserBuilder.java", "UserBuilder", List.of(), false, List.of("build"))).type())
                .isEqualTo(ComponentType.BUILDER);
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(Path.of("/tmp/project"), AutomationFramework.SELENIUM, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.JAVA, com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager.UNKNOWN, com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool.MAVEN, Set.of(), Set.of(AutomationFramework.SELENIUM), List.of(), null, List.of(), List.of(), com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence.HIGH, true);
    }

    private SourceMetadataParser.SourceMetadata metadata(String path, String name, List<String> tags, boolean testComponent, List<String> methods) {
        return new SourceMetadataParser.SourceMetadata(path, name, SourceLanguage.JAVA, null, List.of(name), methods, List.of("java.util.List"), List.of(), List.of(), tags, testComponent, List.of());
    }
}
