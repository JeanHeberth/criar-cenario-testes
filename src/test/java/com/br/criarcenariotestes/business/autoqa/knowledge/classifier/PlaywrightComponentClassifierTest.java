package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaywrightComponentClassifier - Testes Unitários")
class PlaywrightComponentClassifierTest {

    private final PlaywrightComponentClassifier classifier = new PlaywrightComponentClassifier();

    @Test
    @DisplayName("Deve classificar spec como test")
    void deveClassificarSpecComoTest() {
        assertThat(classifier.classify(discovery(), metadata("src/login.spec.ts", "login.spec", List.of("TEST"), true, List.of("test"))).type())
                .isEqualTo(ComponentType.TEST);
    }

    @Test
    @DisplayName("Deve classificar page object")
    void deveClassificarPageObject() {
        assertThat(classifier.classify(discovery(), metadata("src/pages/LoginPage.ts", "LoginPage", List.of("PAGE_OBJECT_EVIDENCE"), false, List.of("open"))).type())
                .isEqualTo(ComponentType.PAGE_OBJECT);
    }

    @Test
    @DisplayName("Deve classificar fixture")
    void deveClassificarFixture() {
        assertThat(classifier.classify(discovery(), metadata("src/fixtures/auth.ts", "auth", List.of("FIXTURE"), false, List.of("setup"))).type())
                .isEqualTo(ComponentType.FIXTURE);
    }

    @Test
    @DisplayName("Deve classificar helper")
    void deveClassificarHelper() {
        assertThat(classifier.classify(discovery(), metadata("src/helpers/login.ts", "login", List.of("HELPER_EVIDENCE"), false, List.of("build"))).type())
                .isEqualTo(ComponentType.HELPER);
    }

    @Test
    @DisplayName("Deve classificar api client")
    void deveClassificarApiClient() {
        assertThat(classifier.classify(discovery(), metadata("src/api/auth.ts", "auth", List.of("API_CLIENT_EVIDENCE"), false, List.of("request"))).type())
                .isEqualTo(ComponentType.API_CLIENT);
    }

    @Test
    @DisplayName("Deve não classificar somente por nome quando conteúdo contradiz")
    void deveNaoClassificarSomentePorNomeQuandoConteudoContradiz() {
        ProjectComponent component = classifier.classify(discovery(), metadata("src/misc/LoginPage.ts", "LoginPage", List.of(), false, List.of()));

        assertThat(component.type()).isEqualTo(ComponentType.UNKNOWN);
    }

    private ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(Path.of("/tmp/project"), AutomationFramework.PLAYWRIGHT, com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage.TYPESCRIPT, com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager.NPM, com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool.NPM, Set.of(), Set.of(AutomationFramework.PLAYWRIGHT), List.of(), null, List.of(), List.of(), com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence.HIGH, true);
    }

    private SourceMetadataParser.SourceMetadata metadata(String path, String name, List<String> tags, boolean testComponent, List<String> methods) {
        return new SourceMetadataParser.SourceMetadata(path, name, SourceLanguage.TYPESCRIPT, null, List.of(), methods, List.of(), List.of(), List.of(), tags, testComponent, List.of());
    }
}
