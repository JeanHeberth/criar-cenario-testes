package com.br.criarcenariotestes.business.autoqa.knowledge;

import com.br.criarcenariotestes.business.autoqa.knowledge.builder.ProjectKnowledgeResultBuilder;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.CypressComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.GenericComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.JavaComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.PlaywrightComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.ProjectComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.RobotComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.JavaMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.PythonMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.ResourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.RobotMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.TypeScriptMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.NamingConventionResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ProjectStructureResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ReuseCandidateResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanPolicy;
import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanner;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectKnowledgeService - Testes Unitários")
class ProjectKnowledgeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve coletar conhecimento Playwright")
    void deveColetarConhecimentoPlaywright() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.tests()).isNotEmpty();
        assertThat(result.pageObjects()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve coletar conhecimento Cypress")
    void deveColetarConhecimentoCypress() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.CYPRESS, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.helpers()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve coletar conhecimento Selenide")
    void deveColetarConhecimentoSelenide() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.SELENIDE, AutomationLanguage.JAVA),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.pageObjects()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve coletar conhecimento Selenium")
    void deveColetarConhecimentoSelenium() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.SELENIUM, AutomationLanguage.JAVA),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.components()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve coletar conhecimento RestAssured")
    void deveColetarConhecimentoRestAssured() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.REST_ASSURED, AutomationLanguage.JAVA),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.apiClients()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve coletar conhecimento Robot")
    void deveColetarConhecimentoRobot() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.ROBOT_FRAMEWORK, AutomationLanguage.ROBOT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.COMPLETE);
        assertThat(result.tests()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve catalogar projeto unknown como partial")
    void deveCatalogarProjetoUnknownComoPartial() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.unknownDiscovery(tempDir),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.PARTIAL);
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar empty sem componentes")
    void deveRetornarEmptySemComponentes() throws Exception {
        ProjectKnowledgeResult result = service(false).collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.status()).isEqualTo(KnowledgeStatus.EMPTY);
    }

    @Test
    @DisplayName("Deve rejeitar discovery nulo")
    void deveRejeitarDiscoveryNulo() {
        assertThatThrownBy(() -> service().collect(null, KnowledgeTestData.analysis()))
                .isInstanceOf(ProjectKnowledgeValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar scenario analysis nulo")
    void deveRejeitarScenarioAnalysisNulo() {
        assertThatThrownBy(() -> service().collect(KnowledgeTestData.unknownDiscovery(tempDir), null))
                .isInstanceOf(ProjectKnowledgeValidationException.class);
    }

    @Test
    @DisplayName("Deve usar caminho normalizado da descoberta")
    void deveUsarCaminhoNormalizadoDaDescoberta() throws Exception {
        Path path = tempDir.resolve(".").normalize();
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(path, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.normalizedProjectPath()).isEqualTo(path.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThatThrownBy(() -> result.components().add(KnowledgeTestData.component("a", "b", com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType.TEST, com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage.TYPESCRIPT)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve não duplicar componentes")
    void deveNaoDuplicarComponentes() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.components()).extracting(ProjectComponent::relativePath).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Deve manter caminhos relativos")
    void deveManterCaminhosRelativos() throws Exception {
        ProjectKnowledgeResult result = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(result.components()).allSatisfy(component -> assertThat(Path.of(component.relativePath()).isAbsolute()).isFalse());
    }

    @Test
    @DisplayName("Deve ser stateless")
    void deveSerStateless() throws Exception {
        ProjectKnowledgeResult first = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );
        ProjectKnowledgeResult second = service().collect(
                KnowledgeTestData.discovery(tempDir, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT),
                KnowledgeTestData.analysis()
        );

        assertThat(first).usingRecursiveComparison().isEqualTo(second);
    }

    private ProjectKnowledgeService service() throws Exception {
        return service(true);
    }

    private ProjectKnowledgeService service(boolean populate) throws Exception {
        Path root = tempDir;
        if (populate) {
            Files.createDirectories(root.resolve("src/pages"));
            Files.createDirectories(root.resolve("src/fixtures"));
            Files.createDirectories(root.resolve("src/helpers"));
            Files.createDirectories(root.resolve("src/api"));
            Files.createDirectories(root.resolve("src/test/java"));
            Files.createDirectories(root.resolve("src/support"));
            Files.createDirectories(root.resolve("tests"));
            Files.writeString(root.resolve("src/pages/LoginPage.ts"), """
                    import { Page } from '@playwright/test';
                    export class LoginPage {
                      open() {}
                    }
                    """);
            Files.writeString(root.resolve("src/fixtures/auth.ts"), """
                    export const test = base.test.extend({});
                    """);
            Files.writeString(root.resolve("src/helpers/http.ts"), "export function buildUser() {}");
            Files.writeString(root.resolve("src/api/client.ts"), "export function callApi() {}");
            Files.writeString(root.resolve("tests/login.spec.ts"), "test('login', () => {});");
            Files.writeString(root.resolve("tests/login.cy.ts"), "describe('login', () => {});");
            Files.writeString(root.resolve("src/support/commands.ts"), "Cypress.Commands.add('login', () => {});");
            Files.writeString(root.resolve("src/test/java/LoginTest.java"), """
                    import org.junit.jupiter.api.Test;
                    public class LoginTest {
                      @Test public void open() {}
                    }
                    """);
            Files.writeString(root.resolve("src/test/java/LoginPage.java"), """
                    import org.openqa.selenium.WebElement;
                    public class LoginPage {
                      public void open() {}
                    }
                    """);
            Files.writeString(root.resolve("src/test/java/UserClient.java"), """
                    import io.restassured.RestAssured;
                    public class UserClient {
                      public void call() {}
                    }
                    """);
            Files.writeString(root.resolve("tests/login.robot"), """
                    *** Test Cases ***
                    Login válido
                        No Operation
                    """);
            Files.writeString(root.resolve("tests/common.resource"), """
                    *** Keywords ***
                    Abrir Login
                        No Operation
                    """);
        }

        return new ProjectKnowledgeService(
                new KnowledgeScanner(new KnowledgeScanPolicy()),
                List.of(new TypeScriptMetadataParser(), new JavaMetadataParser(), new PythonMetadataParser(), new RobotMetadataParser(), new ResourceMetadataParser()),
                List.of(new PlaywrightComponentClassifier(), new CypressComponentClassifier(), new JavaComponentClassifier(), new RobotComponentClassifier(), new GenericComponentClassifier()),
                new ProjectStructureResolver(),
                new NamingConventionResolver(),
                new ReuseCandidateResolver(),
                new ProjectKnowledgeResultBuilder()
        );
    }
}
