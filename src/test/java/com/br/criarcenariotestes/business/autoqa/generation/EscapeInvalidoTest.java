package com.br.criarcenariotestes.business.autoqa.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EscapeInvalidoTest {

    private final GenerationResponseParser parser = new GenerationResponseParser(new ObjectMapper());

    /** Reproduz o JSON real que derrubou a regeração. */
    private String respostaCom(String conteudoDoArquivo) {
        return """
                {"executionId":null,"framework":"PLAYWRIGHT","language":"TYPESCRIPT",
                 "files":[{"relativePath":"tests/api/auth/client.ts","operation":"CREATE",
                 "componentType":"API_CLIENT","content":"%s","encoding":"UTF-8",
                 "status":"GENERATED","existingFile":false}],
                 "reusedFiles":[],"warnings":[],"status":"COMPLETED","confidence":"HIGH","valid":true}
                """.formatted(conteudoDoArquivo);
    }

    @Test
    void deveCorrigirAspaSimplesEscapadaEmVezDePerderAGeracao() {
        // O modelo escreve import { x } from '@playwright/test' dentro da string
        // JSON e escapa a aspa simples por hábito. \' não existe em JSON: o
        // Jackson recusa e a geração inteira é perdida, já paga.
        String comEscapeInvalido = respostaCom("import { test } from \\'@playwright/test\\';");

        assertThatCode(() -> parser.parse(comEscapeInvalido)).doesNotThrowAnyException();

        var resultado = parser.parse(comEscapeInvalido);
        assertThat(resultado.files().get(0).content())
                .as("a aspa simples fica sem escape, que é a forma válida em JSON")
                .isEqualTo("import { test } from '@playwright/test';");
    }

    @Test
    void devePreservarEscapesValidos() {
        // Sanear demais corromperia conteúdo bom: \n, \" e \\ são válidos.
        String comEscapesValidos = respostaCom("const a = \\\"x\\\";\\nconst b = 'y';");

        var resultado = parser.parse(comEscapesValidos);

        assertThat(resultado.files().get(0).content())
                .isEqualTo("const a = \"x\";\nconst b = 'y';");
    }
}
