package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningEvidenceExtractor - Testes Unitários")
class LearningEvidenceExtractorTest {

    private final LearningEvidenceExtractor extractor = new LearningEvidenceExtractor();

    @Test
    @DisplayName("Deve extrair evidência de framework identificado na discovery")
    void deveExtrairEvidenciaDeDiscovery() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("discovery"));
    }

    @Test
    @DisplayName("Deve extrair evidência de reuseCandidates do knowledge")
    void deveExtrairEvidenciaDeKnowledge() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("knowledge")
                && "tests/support/loginPage.ts".equals(e.relativePath()));
    }

    @Test
    @DisplayName("Deve extrair evidência de reusedFiles da generation")
    void deveExtrairEvidenciaDeGeneration() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("generation")
                && "tests/support/loginPage.ts".equals(e.relativePath()));
    }

    @Test
    @DisplayName("Deve extrair evidência de passedRules do review")
    void deveExtrairEvidenciaDeReview() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("review") && e.referenceId().equals("no-hardcoded-wait"));
    }

    @Test
    @DisplayName("Deve extrair evidência de arquivos aplicados")
    void deveExtrairEvidenciaDeApply() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("apply")
                && "tests/login.spec.ts".equals(e.relativePath()));
    }

    @Test
    @DisplayName("Deve extrair evidência de resumo de execução")
    void deveExtrairEvidenciaDeExecution() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("execution"));
    }

    @Test
    @DisplayName("Não deve extrair evidência de finding quando não houver falha (NO_FAILURE)")
    void naoDeveExtrairFindingQuandoNaoHaFalha() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).noneMatch(e -> e.source().equals("failure-analysis"));
    }

    @Test
    @DisplayName("Deve extrair evidência de finding quando houver falha analisada")
    void deveExtrairEvidenciaDeFindingQuandoHaFalha() {
        AutoQaContext ctx = LearningTestData.failedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).anyMatch(e -> e.source().equals("failure-analysis") && e.referenceId().equals("DF-1"));
    }

    @Test
    @DisplayName("Não deve conter caminho absoluto em nenhuma evidência")
    void naoDeveConterCaminhoAbsoluto() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).allSatisfy(e -> {
            if (e.relativePath() != null) {
                assertThat(e.relativePath()).doesNotStartWith("/");
            }
        });
    }

    @Test
    @DisplayName("Não deve conter o projectPath do contexto em nenhuma descrição")
    void naoDeveConterProjectPath() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThat(evidence).allSatisfy(e -> assertThat(e.description()).doesNotContain("/projeto"));
    }

    @Test
    @DisplayName("Deve rejeitar discovery nula")
    void deveRejeitarDiscoveryNula() {
        AutoQaContext ctx = LearningTestData.passedContext();
        assertThatThrownBy(() -> extractor.extract(null, ctx.getScenarioAnalysisResult(), ctx.getProjectKnowledgeResult(),
                ctx.getTechnicalPlanResult(), ctx.getGenerationResult(), ctx.getCodeReviewResult(), ctx.getApplyResult(),
                ctx.getExecutionResult(), ctx.getFailureAnalysisResult()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar execution nula")
    void deveRejeitarExecutionNula() {
        AutoQaContext ctx = LearningTestData.passedContext();
        assertThatThrownBy(() -> extractor.extract(ctx.getProjectDiscoveryResult(), ctx.getScenarioAnalysisResult(),
                ctx.getProjectKnowledgeResult(), ctx.getTechnicalPlanResult(), ctx.getGenerationResult(),
                ctx.getCodeReviewResult(), ctx.getApplyResult(), null, ctx.getFailureAnalysisResult()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar lista imutável")
    void deveRetornarListaImutavel() {
        AutoQaContext ctx = LearningTestData.passedContext();
        List<LearningEvidence> evidence = extract(ctx);
        assertThatThrownBy(() -> evidence.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve limitar a quantidade de evidências extraídas por fonte")
    void deveLimitarQuantidadePorFonte() {
        String[] manyPaths = new String[80];
        for (int i = 0; i < manyPaths.length; i++) manyPaths[i] = "tests/support/page" + i + ".ts";
        AutoQaContext ctx = AutoQaContext.create("Cenário", "/projeto");
        ctx.registerProjectDiscovery(com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.playwrightDiscovery());
        ctx.registerScenarioAnalysis(com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.validScenario());
        ctx.registerProjectKnowledge(LearningTestData.knowledgeWithReuseCandidates(manyPaths));
        ctx.registerTechnicalPlan(com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.readyPlan(
                com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.createAction("tests/login.spec.ts",
                        com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST)));
        ctx.registerGeneration(LearningTestData.generationWithReusedFiles(ctx.getExecutionId()));
        ctx.registerCodeReview(LearningTestData.approvedReview(ctx.getExecutionId()));
        ctx.registerApplyApproval(new com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval(true, "qa.lead",
                java.time.LocalDateTime.now(), List.of(com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation.CREATE), true, true));
        ctx.registerApplyResult(LearningTestData.completedApply(ctx.getExecutionId()));
        ctx.registerExecutionApproval(new com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval(true, "qa.lead",
                java.time.LocalDateTime.now(), java.util.Set.of(com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false));
        ctx.registerExecutionResult(LearningTestData.passedExecution(ctx.getExecutionId(), "PLAYWRIGHT", 3));
        ctx.registerFailureAnalysis(LearningTestData.noFailureResult(ctx.getExecutionId()));

        List<LearningEvidence> evidence = extract(ctx);
        long knowledgeEvidence = evidence.stream().filter(e -> e.source().equals("knowledge")).count();
        assertThat(knowledgeEvidence).isLessThanOrEqualTo(20);
    }

    private List<LearningEvidence> extract(AutoQaContext ctx) {
        return extractor.extract(ctx.getProjectDiscoveryResult(), ctx.getScenarioAnalysisResult(),
                ctx.getProjectKnowledgeResult(), ctx.getTechnicalPlanResult(), ctx.getGenerationResult(),
                ctx.getCodeReviewResult(), ctx.getApplyResult(), ctx.getExecutionResult(), ctx.getFailureAnalysisResult());
    }
}
