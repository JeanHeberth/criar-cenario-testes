package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TypeScriptMetadataParser - Testes Unitários")
class TypeScriptMetadataParserTest {

    private final TypeScriptMetadataParser parser = new TypeScriptMetadataParser();

    @Test
    @DisplayName("Deve extrair imports")
    void deveExtrairImports() {
        var metadata = parser.parse(file("src/Login.spec.ts", """
                import { test } from '@playwright/test';
                import { LoginPage } from '../pages/LoginPage';
                """));

        assertThat(metadata.imports()).contains("@playwright/test", "../pages/LoginPage");
    }

    @Test
    @DisplayName("Deve extrair classe")
    void deveExtrairClasse() {
        var metadata = parser.parse(file("src/LoginPage.ts", """
                export class LoginPage {}
                """));

        assertThat(metadata.declaredClasses()).contains("LoginPage");
        assertThat(metadata.language()).isEqualTo(SourceLanguage.TYPESCRIPT);
    }

    @Test
    @DisplayName("Deve extrair métodos públicos")
    void deveExtrairMetodosPublicos() {
        var metadata = parser.parse(file("src/LoginPage.ts", """
                export class LoginPage {
                  async open() {}
                  submit() {}
                }
                """));

        assertThat(metadata.declaredMethods()).contains("open", "submit");
    }

    @Test
    @DisplayName("Deve extrair funções exportadas")
    void deveExtrairFuncoesExportadas() {
        var metadata = parser.parse(file("src/helpers.ts", """
                export function buildUser() {}
                export const createToken = () => {};
                """));

        assertThat(metadata.declaredMethods()).contains("buildUser", "createToken");
    }

    @Test
    @DisplayName("Deve detectar describe e test")
    void deveDetectarDescribeETest() {
        var metadata = parser.parse(file("src/Login.spec.ts", """
                import { test, expect } from '@playwright/test';
                test.describe('login', () => {});
                test('deve abrir', () => {});
                """));

        assertThat(metadata.tags()).contains("TEST");
        assertThat(metadata.testComponent()).isTrue();
    }

    @Test
    @DisplayName("Deve detectar test.extend")
    void deveDetectarTestExtend() {
        var metadata = parser.parse(file("src/fixtures.ts", """
                export const test = base.test.extend({});
                """));

        assertThat(metadata.tags()).contains("FIXTURE");
    }

    @Test
    @DisplayName("Deve não armazenar corpo completo")
    void deveNaoArmazenarCorpoCompleto() {
        var metadata = parser.parse(file("src/LoginPage.ts", """
                export class LoginPage {
                  submit() {
                    const secret = 'abc';
                    return secret;
                  }
                }
                """));

        assertThat(metadata.declaredMethods()).doesNotContain("const secret = 'abc';");
    }

    @Test
    @DisplayName("Deve tratar arquivo inválido")
    void deveTratarArquivoInvalido() {
        assertThatThrownBy(() -> parser.parse(new KnowledgeScanResult.KnowledgeFile("src/LoginPage.ts", "LoginPage.ts", ".ts", 0, null)))
                .isInstanceOf(NullPointerException.class);
    }

    private KnowledgeScanResult.KnowledgeFile file(String path, String content) {
        return new KnowledgeScanResult.KnowledgeFile(path, path.substring(path.lastIndexOf('/') + 1), ".ts", content == null ? null : content.length(), content == null ? "" : content);
    }
}
