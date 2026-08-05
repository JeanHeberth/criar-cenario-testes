package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PythonMetadataParser - Testes Unitários")
class PythonMetadataParserTest {

    private final PythonMetadataParser parser = new PythonMetadataParser();

    @Test
    @DisplayName("Deve extrair imports")
    void deveExtrairImports() {
        var metadata = parser.parse(file("src/test_login.py", """
                import os
                from pages.login_page import LoginPage
                """));

        assertThat(metadata.imports()).contains("os", "pages.login_page");
    }

    @Test
    @DisplayName("Deve extrair classes")
    void deveExtrairClasses() {
        var metadata = parser.parse(file("src/login_page.py", """
                class LoginPage:
                    pass
                """));

        assertThat(metadata.declaredClasses()).contains("LoginPage");
    }

    @Test
    @DisplayName("Deve extrair funções")
    void deveExtrairFuncoes() {
        var metadata = parser.parse(file("src/helpers.py", """
                def build_user():
                    pass
                """));

        assertThat(metadata.declaredMethods()).contains("build_user");
    }

    @Test
    @DisplayName("Deve detectar fixture pytest")
    void deveDetectarFixturePytest() {
        var metadata = parser.parse(file("src/conftest.py", """
                import pytest

                @pytest.fixture
                def user():
                    return {}
                """));

        assertThat(metadata.tags()).contains("FIXTURE");
    }

    @Test
    @DisplayName("Deve detectar decorators")
    void deveDetectarDecorators() {
        var metadata = parser.parse(file("src/service.py", """
                @decorator
                def run():
                    pass
                """));

        assertThat(metadata.annotations()).contains("decorator");
        assertThat(metadata.tags()).contains("DECORATOR");
    }

    @Test
    @DisplayName("Deve não armazenar corpo integral")
    void deveNaoArmazenarCorpoIntegral() {
        var metadata = parser.parse(file("src/service.py", """
                def run():
                    secret = 'abc'
                    return secret
                """));

        assertThat(metadata.declaredMethods()).doesNotContain("secret = 'abc'");
    }

    private KnowledgeScanResult.KnowledgeFile file(String path, String content) {
        return new KnowledgeScanResult.KnowledgeFile(path, path.substring(path.lastIndexOf('/') + 1), ".py", content.length(), content);
    }
}
