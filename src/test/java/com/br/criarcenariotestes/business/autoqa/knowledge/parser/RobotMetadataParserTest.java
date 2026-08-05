package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RobotMetadataParser - Testes Unitários")
class RobotMetadataParserTest {

    private final RobotMetadataParser parser = new RobotMetadataParser();

    @Test
    @DisplayName("Deve detectar test cases")
    void deveDetectarTestCases() {
        var metadata = parser.parse(file("tests/login.robot", """
                *** Test Cases ***
                Login válido
                    No Operation
                """));

        assertThat(metadata.testComponent()).isTrue();
        assertThat(metadata.declaredMethods()).contains("TEST_CASES");
    }

    @Test
    @DisplayName("Deve detectar keywords")
    void deveDetectarKeywords() {
        var metadata = parser.parse(file("resources/common.resource", """
                *** Keywords ***
                Abrir Login
                    No Operation
                """));

        assertThat(metadata.declaredMethods()).contains("KEYWORDS");
    }

    @Test
    @DisplayName("Deve detectar resources")
    void deveDetectarResources() {
        var metadata = parser.parse(file("resources/common.resource", """
                Resource    shared.resource
                """));

        assertThat(metadata.imports()).contains("shared.resource");
    }

    @Test
    @DisplayName("Deve detectar libraries")
    void deveDetectarLibraries() {
        var metadata = parser.parse(file("tests/login.robot", """
                Library    SeleniumLibrary
                """));

        assertThat(metadata.imports()).contains("SeleniumLibrary");
    }

    @Test
    @DisplayName("Deve detectar variables")
    void deveDetectarVariables() {
        var metadata = parser.parse(file("tests/login.robot", """
                Variables    vars.py
                """));

        assertThat(metadata.imports()).contains("vars.py");
    }

    @Test
    @DisplayName("Deve detectar tags")
    void deveDetectarTags() {
        var metadata = parser.parse(file("tests/login.robot", """
                *** Test Cases ***
                Login válido
                    [Tags]    smoke    login
                """));

        assertThat(metadata.tags()).contains("smoke", "login");
    }

    @Test
    @DisplayName("Deve não armazenar valores de variáveis")
    void deveNaoArmazenarValoresDeVariaveis() {
        var metadata = parser.parse(file("tests/vars.resource", """
                ${PASSWORD}    secret
                """));

        assertThat(metadata.declaredMethods()).doesNotContain("secret");
    }

    private KnowledgeScanResult.KnowledgeFile file(String path, String content) {
        String extension = path.substring(path.lastIndexOf('.'));
        return new KnowledgeScanResult.KnowledgeFile(path, path.substring(path.lastIndexOf('/') + 1), extension, content.length(), content);
    }
}
