package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationParseException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationResponseParser - Testes Unitários")
class GenerationResponseParserTest {

    private GenerationResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new GenerationResponseParser(new ObjectMapper());
    }

    @Test
    @DisplayName("Deve parsear resposta válida")
    void deveParsearRespostaValida() {
        GenerationResult result = parser.parse(validJson());
        assertThat(result.files()).hasSize(1);
        assertThat(result.status()).isNotNull();
        assertThat(result.confidence()).isNotNull();
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarRespostaNula() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(GenerationParseException.class)
                .hasMessageContaining("nula");
    }

    @Test
    @DisplayName("Deve rejeitar resposta vazia")
    void deveRejeitarRespostaVazia() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(GenerationParseException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("Deve rejeitar resposta somente com espaços (blank)")
    void deveRejeitarBlank() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(GenerationParseException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    @DisplayName("Deve rejeitar JSON inválido")
    void deveRejeitarJsonInvalido() {
        assertThatThrownBy(() -> parser.parse("{invalid json}"))
                .isInstanceOf(GenerationParseException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    @DisplayName("Deve remover wrapper Markdown json conhecido")
    void deveRemoverWrapperConhecido() {
        String wrapped = "```json\n" + validJson() + "\n```";
        GenerationResult result = parser.parse(wrapped);
        assertThat(result.files()).hasSize(1);
    }

    @Test
    @DisplayName("Deve remover wrapper Markdown simples")
    void deveRemoverWrapperMarkdownSimples() {
        String wrapped = "```\n" + validJson() + "\n```";
        GenerationResult result = parser.parse(wrapped);
        assertThat(result.files()).hasSize(1);
    }

    @Test
    @DisplayName("Deve ignorar campo desconhecido")
    void deveIgnorarCampoDesconhecido() {
        String json = """
                {
                  "files": [],
                  "warnings": [],
                  "status": "COMPLETED",
                  "confidence": "HIGH",
                  "valid": true,
                  "unknownField": "should be ignored"
                }
                """;
        GenerationResult result = parser.parse(json);
        assertThat(result).isNotNull();
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar enum desconhecido (DELETE não existe em GeneratedFileOperation)")
    void deveRejeitarEnumDesconhecido() {
        String json = """
                {
                  "files": [
                    {
                      "relativePath": "tests/x.ts",
                      "operation": "DELETE",
                      "componentType": "TEST",
                      "content": "codigo",
                      "encoding": "UTF-8",
                      "existingFile": false,
                      "reusedComponents": [],
                      "dependencies": [],
                      "warnings": []
                    }
                  ],
                  "warnings": [],
                  "status": "COMPLETED",
                  "confidence": "HIGH",
                  "valid": true
                }
                """;
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(GenerationParseException.class);
    }

    @Test
    @DisplayName("Deve rejeitar resposta acima do limite")
    void deveRejeitarRespostaAcimaDoLimite() {
        String tooLong = "x".repeat(GenerationResponseParser.MAX_RESPONSE_LENGTH + 1);
        assertThatThrownBy(() -> parser.parse(tooLong))
                .isInstanceOf(GenerationParseException.class)
                .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("Não deve inventar campo (executionId ausente permanece nulo)")
    void deveNaoInventarCampo() {
        GenerationResult result = parser.parse(validJson());
        assertThat(result.executionId()).isNull();
        assertThat(result.generatedRoot()).isNull();
    }

    @Test
    @DisplayName("Deve preservar código com quebras de linha")
    void devePreservarCodigoComQuebrasDeLinha() {
        GenerationResult result = parser.parse(validJson());
        assertThat(result.files().get(0).content()).contains("\n");
    }

    @Test
    @DisplayName("Deve não armazenar JSON bruto")
    void deveNaoArmazenarJsonBruto() {
        GenerationResult result = parser.parse(validJson());
        assertThat(result.getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("rawJson", "json", "raw");
    }

    @Test
    @DisplayName("Deve parsear operation e componentType corretamente")
    void deveParsearOperationEComponentType() {
        GenerationResult result = parser.parse(validJson());
        assertThat(result.files().get(0).operation()).isEqualTo(GeneratedFileOperation.CREATE);
    }

    private String validJson() {
        return """
                {
                  "files": [
                    {
                      "relativePath": "tests/login.spec.ts",
                      "operation": "CREATE",
                      "componentType": "TEST",
                      "content": "import { test } from '@playwright/test';\\n\\ntest('login', async () => {});\\n",
                      "encoding": "UTF-8",
                      "existingFile": false,
                      "reusedComponents": [],
                      "dependencies": [],
                      "warnings": []
                    }
                  ],
                  "warnings": [],
                  "status": "COMPLETED",
                  "confidence": "HIGH",
                  "valid": true
                }
                """;
    }
}
