package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningConfidence;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import com.br.criarcenariotestes.business.autoqa.model.scenario.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FidelidadeAoContratoTest {

    private final StaticReviewRuleEngine engine = new StaticReviewRuleEngine();

    /** Trecho do contrato real usado no teste ponta a ponta. */
    private static final String CONTRATO = """
            Corpo, com estes nomes de campo exatos: "email" (string), "senha" (string).
            RESPOSTA 200: { "token": "...", "tipo": "Bearer", "usuario": { "id": 2, "nome": "x", "email": "a@b.com" } }
            RESPOSTA 401: { "status": 401, "erro": "401 UNAUTHORIZED", "mensagem": "Credenciais inválidas",
                            "path": "/criandoAPI/v1/auth/login", "timestamp": "ISO-8601" }
            """;

    private List<ReviewIssue> revisar(String codigo) {
        var artifact = new GeneratedArtifactReader.ReadArtifact(
                "tests/api/auth/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, codigo, "hash", true);
        return engine.review(AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                planoVazio(), geracaoVazia(), List.of(artifact),
                VocabularioDoContrato.doTexto(CONTRATO));
    }

    @Test
    void deveAcusarCampoInventadoQueOContratoNaoTem() {
        // Reprodução do defeito real: o código gerado passou em TODAS as regras
        // estáticas asserindo "message" onde o contrato diz "mensagem".
        String codigo = """
                test('login invalido', async () => {
                  const body = await response.json();
                  expect(body).toMatchObject({ message: expect.any(String), path: expect.any(String) });
                  expect(Object.keys(body)).toEqual(['message', 'path']);
                });
                """;

        List<ReviewIssue> issues = revisar(codigo);

        assertThat(issues)
                .filteredOn(i -> ReviewRule.CONTRACT_FIELD_UNKNOWN.name().equals(i.code()))
                .singleElement()
                .satisfies(i -> {
                    assertThat(i.message()).contains("message");
                    assertThat(i.severity())
                            .as("HIGH mapeia para CHANGES_REQUIRED: não aprova, e o apply não é liberado")
                            .isEqualTo(ReviewSeverity.HIGH);
                });
    }

    @Test
    void naoDeveAcusarCampoQueOContratoMenciona() {
        String codigo = """
                test('login valido', async () => {
                  const body = await response.json();
                  expect(body).toMatchObject({ token: expect.any(String), tipo: 'Bearer' });
                  expect(Object.keys(body)).toEqual(['token', 'tipo', 'usuario']);
                });
                """;

        assertThat(revisar(codigo))
                .filteredOn(i -> ReviewRule.CONTRACT_FIELD_UNKNOWN.name().equals(i.code()))
                .as("token/tipo/usuario vêm do contrato — acusar seria falso positivo")
                .isEmpty();
    }

    @Test
    void deveSeCalarQuandoNaoHaVocabularioConfiavel() {
        // Cenário sem campo citado não serve de referência: sem ela a regra não
        // adivinha, e bloquear com base em nada reprovaria código correto.
        assertThat(VocabularioDoContrato.doTexto("cenario em prosa sem citar campo").utilizavel()).isFalse();
    }

    @Test
    void deveAcusarOContratoAlucinadoQueOReviewAprovouEmProducao() {
        // Caso real: a geração produziu um contrato estilo NestJS
        // (statusCode/message/error) para uma API que responde
        // status/erro/mensagem — e o review APROVOU, porque a primeira versão
        // desta regra lia o vocabulário da análise da IA, que já continha os
        // nomes errados. Com o texto original como fonte, os três são acusados.
        String codigo = """
                expect(body, 'estrutura de erro').toMatchObject({
                  statusCode: 401,
                  message: expect.anything(),
                  error: expect.stringMatching(/Unauthorized/i)
                });
                """;

        assertThat(revisar(codigo))
                .filteredOn(i -> ReviewRule.CONTRACT_FIELD_UNKNOWN.name().equals(i.code()))
                .singleElement()
                .satisfies(i -> assertThat(i.message())
                        .contains("statusCode").contains("message").contains("error"));
    }

    private GenerationResult geracaoVazia() {
        return new GenerationResult(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                List.of(), List.of(), List.of(), "raiz", "manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    /** Plano sem arquivos: a aderência ao plano não é o que este teste exercita. */
    private TechnicalPlanResult planoVazio() {
        return new TechnicalPlanResult("t", "s", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                PlanningStatus.READY_WITH_WARNINGS, PlanningConfidence.MEDIUM, true);
    }
}
