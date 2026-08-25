package com.br.criarcenariotestes.business.autoqa.context;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NovaTentativaDeGeracaoTest {

    private AutoQaContext contextoComGeracao() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(GenerationTestData.readyPlan());
        context.registerGeneration(geracao());
        return context;
    }

    @Test
    void deveRecusarSegundaGeracaoSemPrepararTentativa() {
        // O guard existe para pegar dupla gravação acidental e continua valendo.
        AutoQaContext context = contextoComGeracao();

        assertThatThrownBy(() -> context.registerGeneration(geracao()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void devePermitirNovaGeracaoAposPrepararTentativa() {
        // Regressão: o laço de regeração disparava e morria na segunda volta com
        // "Generation already registered" — detectava o erro e não conseguia
        // corrigi-lo.
        AutoQaContext context = contextoComGeracao();

        context.prepararNovaTentativaDeGeracao();

        assertThat(context.getGenerationResult()).isNull();
        assertThat(context.getCodeReviewResult()).isNull();
        assertThatCode(() -> context.registerGeneration(geracao()))
                .doesNotThrowAnyException();
    }

    @Test
    void naoDeveDescartarAsEtapasAnterioresAoPrepararTentativa() {
        // Refazer discovery/análise/plano custaria chamadas de IA à toa: eles
        // são a base sobre a qual a nova geração acontece.
        AutoQaContext context = contextoComGeracao();

        context.prepararNovaTentativaDeGeracao();

        assertThat(context.getProjectDiscoveryResult()).isNotNull();
        assertThat(context.getScenarioAnalysisResult()).isNotNull();
        assertThat(context.getProjectKnowledgeResult()).isNotNull();
        assertThat(context.getTechnicalPlanResult()).isNotNull();
    }

    private com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult geracao() {
        return GenerationTestData.aiResult(
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);
    }
}
