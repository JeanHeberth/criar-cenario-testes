package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.*;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NegativeLearningCollector - Testes Unitários")
class NegativeLearningCollectorTest {

    private final NegativeLearningCollector collector = new NegativeLearningCollector();

    @Test
    @DisplayName("Deve retornar vazio quando ExecutionStatus não for FAILED")
    void deveRetornarVazioQuandoNaoForFailed() {
        UUID id = UUID.randomUUID();
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED, id),
                LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste"), evidence(id));
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando FailureAnalysisResult for inválido")
    void deveRetornarVazioQuandoAnaliseInvalida() {
        UUID id = UUID.randomUUID();
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id),
                LearningTestData.invalidResult(id), evidence(id));
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar vazio quando não houver findings")
    void deveRetornarVazioSemFindings() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult inconclusive = new FailureAnalysisResult(id, List.of(), List.of(), List.of(), List.of(),
                FailureAnalysisStatus.INCONCLUSIVE, FailureConfidence.LOW, true, false, false, true);
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), inconclusive, List.of());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Deve gerar item FAILURE_PATTERN para categoria de asserção")
    void deveGerarFailurePatternParaAssertion() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).anyMatch(i -> i.type() == LearningType.FAILURE_PATTERN);
    }

    @Test
    @DisplayName("Deve gerar item RETRY_STRATEGY para FLAKY_TEST")
    void deveGerarRetryStrategyParaFlaky() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.FLAKY_TEST, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).anyMatch(i -> i.type() == LearningType.RETRY_STRATEGY);
    }

    @Test
    @DisplayName("Deve gerar item REGENERATION_STRATEGY para falha de compilação")
    void deveGerarRegenerationStrategyParaCompilacao() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.COMPILATION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).anyMatch(i -> i.type() == LearningType.REGENERATION_STRATEGY);
    }

    @Test
    @DisplayName("Deve gerar item ANTI_PATTERN para defeito de aplicação")
    void deveGerarAntiPatternParaDefeitoAplicacao() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.APPLICATION_DEFECT, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).anyMatch(i -> i.type() == LearningType.ANTI_PATTERN);
    }

    @Test
    @DisplayName("Item negativo nunca deve ser reusable=true")
    void itemNegativoNuncaDeveSerReusable() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> !i.reusable());
    }

    @Test
    @DisplayName("Item negativo deve ter scope PROJECT (nunca GLOBAL/TEAM)")
    void itemNegativoDeveTerScopeProject() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> i.scope() == LearningScope.PROJECT);
    }

    @Test
    @DisplayName("Item negativo deve nascer com approvalStatus PENDING")
    void itemNegativoDeveNascerPending() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> i.approvalStatus() == LearningApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("Item negativo deve exigir humanReviewRequired=true")
    void itemNegativoDeveExigirRevisaoHumana() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(LearningItem::humanReviewRequired);
    }

    @Test
    @DisplayName("Item negativo deve referenciar o código do finding")
    void itemNegativoDeveReferenciarCodigoDoFinding() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> i.relatedFailureCodes().contains("DF-1"));
    }

    @Test
    @DisplayName("Deve rejeitar execution nula")
    void deveRejeitarExecutionNula() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> collector.collect(null, LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste"), evidence(id)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar failureAnalysis nula")
    void deveRejeitarFailureAnalysisNula() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> collector.collect(execution(ExecutionStatus.FAILED, id), null, evidence(id)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Todo item deve conter evidência")
    void todoItemDeveConterEvidencia() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> !i.evidence().isEmpty());
    }

    @Test
    @DisplayName("Todo item deve possuir positive=false")
    void todoItemDevePossuirPositiveFalse() {
        UUID id = UUID.randomUUID();
        FailureAnalysisResult result = LearningTestData.analyzedResult(id, FailureCategory.ASSERTION_FAILURE, "teste");
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED, id), result, evidence(id));
        assertThat(items).allMatch(i -> !i.positive());
    }

    // --- helpers ---

    private ExecutionResult execution(ExecutionStatus status, UUID id) {
        return status == ExecutionStatus.FAILED
                ? LearningTestData.failedExecution(id, "PLAYWRIGHT", 3, 1, "teste")
                : LearningTestData.passedExecution(id, "PLAYWRIGHT", 3);
    }

    private List<LearningEvidence> evidence(UUID executionId) {
        return List.of(new LearningEvidence("failure-analysis", "finding", "DF-1", null,
                "Falha determinística (ASSERTION_FAILURE)", null, true));
    }
}
