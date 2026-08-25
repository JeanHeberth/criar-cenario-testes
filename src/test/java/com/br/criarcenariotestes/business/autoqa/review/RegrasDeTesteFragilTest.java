package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDeTesteFragilTest {

    private final StaticReviewRuleEngine engine = new StaticReviewRuleEngine();

    /** Contrato real: define 200, 401 e 400 — nunca 405 nem 404. */
    private static final Set<String> STATUS_DO_CENARIO = Set.of("200", "401", "400");

    private List<ReviewIssue> revisar(String codigo) {
        var artifact = new GeneratedArtifactReader.ReadArtifact("tests/login.spec.ts",
                GeneratedFileOperation.CREATE, PlanComponentType.TEST, codigo, "hash", true);
        var plano = new TechnicalPlanResult("t", "s", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), PlanningStatus.READY_WITH_WARNINGS,
                PlanningConfidence.MEDIUM, true);
        var ger = new GenerationResult(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", List.of(), List.of(),
                List.of(), "r", "m.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
        return engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT, plano, ger,
                List.of(artifact), VocabularioDoContrato.doTexto("cenario"), STATUS_DO_CENARIO);
    }

    private List<ReviewIssue> apenas(String codigo, ReviewRule regra) {
        return revisar(codigo).stream().filter(i -> regra.name().equals(i.code())).toList();
    }

    @Test
    void deveAcusarTesteQueSeAutoPulaPorFaltaDeVariavelDeAmbiente() {
        // Caso real: num CI sem as variáveis, a suíte reporta verde sem ter
        // exercitado nada. Falso sucesso é pior que falha — ninguém investiga
        // o verde.
        String codigo = """
                test('login', async () => {
                  const usuario = process.env.AUTH_USERNAME;
                  test.skip(!usuario || !senha, 'variáveis devem estar definidas');
                });
                """;

        assertThat(apenas(codigo, ReviewRule.SKIP_POR_AMBIENTE))
                .singleElement()
                .satisfies(i -> assertThat(i.message()).contains("verde sem testar"));
    }

    @Test
    void deveAcusarStatusQueOCenarioNaoDefine() {
        // Caso real: o cenário tratava "método não permitido" como exploratório
        // e o teste afirmou 405/404. A API responde 500 — falharia por palpite.
        String codigo = "expect([405, 404]).toContain(resposta.status());";

        assertThat(apenas(codigo, ReviewRule.ASSERCAO_SOBRE_COMPORTAMENTO_EXPLORATORIO))
                .singleElement()
                .satisfies(i -> assertThat(i.message()).contains("405"));
    }

    @Test
    void naoDeveAcusarStatusQueOCenarioDefine() {
        String codigo = """
                expect(resposta.status()).toBe(200);
                expect(resposta.status()).toBe(401);
                expect(resposta.status()).toBe(400);
                """;

        assertThat(apenas(codigo, ReviewRule.ASSERCAO_SOBRE_COMPORTAMENTO_EXPLORATORIO))
                .as("200, 401 e 400 estão no contrato — acusar seria falso positivo")
                .isEmpty();
    }

    @Test
    void naoDeveAcusarSkipSemRelacaoComAmbiente() {
        // test.skip legítimo, por navegador ou plataforma, não é o alvo.
        String codigo = "test.skip(browserName === 'webkit', 'instável no webkit');";

        assertThat(apenas(codigo, ReviewRule.SKIP_POR_AMBIENTE)).isEmpty();
    }
}
