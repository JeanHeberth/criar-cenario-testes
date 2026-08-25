package com.br.criarcenariotestes.business.autoqa.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PadroesDeConteudoProibido - segredos")
class PadroesDeConteudoProibidoTest {

    @Test
    @DisplayName("Não deve redigir referência a variável de ambiente")
    void naoDeveRedigirReferencia() {
        // Esta foi a raiz de um ciclo inteiro de bloqueios: a redação convertia
        // código correto em "const password = [REDACTED];", e a IA revisora,
        // vendo isso, acusava HARDCODED_SECRET (CRÍTICO) e travava o apply.
        String codigo = "const password = process.env.AUTH_VALID_PASSWORD;";
        assertThat(PadroesDeConteudoProibido.redigirSegredosLiterais(codigo)).isEqualTo(codigo);

        String java = "String senha = System.getenv(\"AUTH_PASSWORD\");";
        assertThat(PadroesDeConteudoProibido.redigirSegredosLiterais(java)).isEqualTo(java);
    }

    @Test
    @DisplayName("Deve redigir segredo literal de verdade")
    void deveRedigirSegredoLiteral() {
        assertThat(PadroesDeConteudoProibido.redigirSegredosLiterais("const senha = 'SuperSecreta123';"))
                .contains("[REDACTED]")
                .doesNotContain("SuperSecreta123");
    }

    @Test
    @DisplayName("Não deve acusar segredo em referência, seletor ou credencial falsa")
    void naoDeveAcusarOqueNaoESegredo() {
        String[] legitimos = {
                "const password = process.env.AUTH_VALID_PASSWORD;",
                "password: VALID_PASSWORD",
                "String senha = System.getenv(\"AUTH_PASSWORD\");",
                "password: 'input[type=\"password\"]'",
                "senha: 'senha_errada'",
                "password: 'senha_incorreta'",
        };
        for (String trecho : legitimos) {
            assertThat(PadroesDeConteudoProibido.contemSegredoLiteral(trecho))
                    .as("acusou indevidamente: %s", trecho)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Deve acusar segredo literal com entropia")
    void deveAcusarSegredoComEntropia() {
        String[] segredos = {
                "const senha = 'SuperSecreta123';",
                "apiKey: 'sk-abc123def456'",
                "password = 'teste123'",
                "const invalidPassword = 'SenhaIncorreta123!';",
        };
        for (String trecho : segredos) {
            assertThat(PadroesDeConteudoProibido.contemSegredoLiteral(trecho))
                    .as("deixou passar: %s", trecho)
                    .isTrue();
        }
    }

    @Test
    void naoDeveAcusarPayloadJsonComoCodigo() {
        // Regressão: a chave estava na regra de código, então descrever o corpo
        // de uma requisição REST derrubava a análise inteira do cenário no
        // primeiro estágio ("Código indevido detectado"). Teste de API sem JSON
        // no exemplo não existe.
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher(
                "{\"email\": \"usuario@teste.com\", \"senha\": \"...\"}").find())
                .as("payload JSON não é código")
                .isFalse();
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher(
                "a resposta deve conter { \"status\": 400, \"erro\": \"Erro de Validação\" }").find())
                .as("contrato de resposta descrito em prosa não é código")
                .isFalse();
    }

    @Test
    void deveContinuarAcusandoCodigoDeVerdade() {
        // O que sustenta a regra depois da saída da chave: keyword, "=>" e ";"
        // fechando linha.
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher("const token = resposta.token;").find()).isTrue();
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher("public class LoginTest").find()).isTrue();
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher("import { test } from '@playwright/test'").find()).isTrue();
        assertThat(PadroesDeConteudoProibido.CODIGO.matcher("() => expect(res).toBeOk()").find()).isTrue();
    }
}
