package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdiomasDoFrameworkTest {

    private final IdiomasDoFramework idiomas = new IdiomasDoFramework();

    private String corrigir(String codigo) {
        return idiomas.aplicar(AutomationFramework.PLAYWRIGHT, "tests/x.ts", codigo).conteudo();
    }

    @Test
    void deveTrocarBodyPorDataEmChamadaDeRequisicao() {
        // Defeito mais persistente da sessão: apareceu nas CINCO regerações,
        // mesmo com a mensagem do compilador apontando linha e coluna.
        String codigo = """
                return await this.request.post(this.endpoint, {
                  body: JSON.stringify({ email, senha }),
                  headers: { 'Content-Type': 'application/json' }
                });
                """;

        assertThat(corrigir(codigo))
                .contains("data: JSON.stringify")
                .doesNotContain("body: JSON.stringify");
    }

    @Test
    void deveRemoverPlaywrightDosImportsNomeados() {
        // 'playwright' não é export de @playwright/test — é fixture.
        String codigo = "import { test, expect, playwright, APIResponse } from '@playwright/test';";

        String corrigido = corrigir(codigo);

        assertThat(corrigido).doesNotContain("playwright,").doesNotContain(", playwright");
        assertThat(corrigido).contains("test").contains("expect").contains("APIResponse");
    }

    @Test
    void naoDeveTocarEmBodyQueNaoEOpcaoDeRequisicao() {
        // Variável local chamada body é legítima e comum. Corrigir demais
        // quebraria código correto — o critério é "construção inexistente",
        // não "palavra parecida".
        String codigo = """
                const body: AuthResponse = await res.json();
                expect(body.token).toBeDefined();
                """;

        assertThat(corrigir(codigo)).isEqualTo(codigo);
    }

    @Test
    void naoDeveTocarEmFrameworkDiferenteDePlaywright() {
        String codigo = "given().body(payload).post(\"/login\");";
        assertThat(idiomas.aplicar(AutomationFramework.REST_ASSURED, "T.java", codigo).conteudo())
                .isEqualTo(codigo);
    }

    @Test
    void deveRegistrarQuaisCorrecoesForamAplicadas() {
        // O sistema reescrever código gerado não pode acontecer em silêncio.
        String codigo = """
                import { test, playwright } from '@playwright/test';
                await request.post('/x', { body: '{}' });
                """;

        var correcao = idiomas.aplicar(AutomationFramework.PLAYWRIGHT, "tests/x.ts", codigo);

        assertThat(correcao.aplicadas()).hasSize(2);
        assertThat(correcao.aplicadas().toString())
                .contains("body: → data:")
                .contains("removido 'playwright'");
    }
}
