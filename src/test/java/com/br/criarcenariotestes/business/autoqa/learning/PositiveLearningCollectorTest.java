package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningApprovalStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningItem;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningScope;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PositiveLearningCollector - Testes Unitários")
class PositiveLearningCollectorTest {

    private final PositiveLearningCollector collector = new PositiveLearningCollector();

    @Test
    @DisplayName("Deve retornar vazio quando ExecutionStatus não for PASSED")
    void deveRetornarVazioQuandoNaoForPassed() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.FAILED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Não deve concluir sucesso apenas porque a execução passou, sem review aprovado")
    void naoDeveConcluirSucessoSemReviewAprovado() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.CHANGES_REQUIRED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Não deve concluir sucesso sem apply concluído")
    void naoDeveConcluirSucessoSemApplyConcluido() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.ROLLED_BACK), evidenceComGeneration());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Deve aceitar review APPROVED_WITH_WARNINGS")
    void deveAceitarReviewComWarnings() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED_WITH_WARNINGS),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).isNotEmpty();
    }

    @Test
    @DisplayName("Deve aceitar apply COMPLETED_WITH_WARNINGS")
    void deveAceitarApplyComWarnings() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED_WITH_WARNINGS), evidenceComGeneration());
        assertThat(items).isNotEmpty();
    }

    @Test
    @DisplayName("Deve gerar item REUSABLE_COMPONENT a partir de evidência de generation")
    void deveGerarItemReusableComponent() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).anyMatch(i -> i.type() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningType.REUSABLE_COMPONENT);
    }

    @Test
    @DisplayName("Item de scope PROJECT deve nascer com approvalStatus PENDING")
    void itemProjectDeveNascerPending() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).filteredOn(i -> i.scope() == LearningScope.PROJECT)
                .isNotEmpty()
                .allMatch(i -> i.approvalStatus() == LearningApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("Item de scope PROJECT deve exigir humanReviewRequired=true")
    void itemProjectDeveExigirRevisaoHumana() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).filteredOn(i -> i.scope() == LearningScope.PROJECT)
                .allMatch(LearningItem::humanReviewRequired);
    }

    @Test
    @DisplayName("Deve gerar item COMMAND_PATTERN de scope EXECUTION a partir de evidência de execution")
    void deveGerarItemCommandPatternExecution() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComExecution());
        assertThat(items).anyMatch(i -> i.type() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningType.COMMAND_PATTERN
                && i.scope() == LearningScope.EXECUTION);
    }

    @Test
    @DisplayName("Item EXECUTION pode usar approvalStatus NOT_REQUIRED")
    void itemExecutionPodeUsarNotRequired() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComExecution());
        assertThat(items).filteredOn(i -> i.scope() == LearningScope.EXECUTION)
                .allMatch(i -> i.approvalStatus() == LearningApprovalStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("Todo item gerado deve conter evidência")
    void todoItemDeveConterEvidencia() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).allMatch(i -> !i.evidence().isEmpty());
    }

    @Test
    @DisplayName("Todo item gerado deve possuir positive=true")
    void todoItemDevePossuirPositiveTrue() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComGeneration());
        assertThat(items).allMatch(LearningItem::positive);
    }

    @Test
    @DisplayName("Deve retornar vazio quando não houver evidência")
    void deveRetornarVazioSemEvidencia() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), List.of());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar execution nula")
    void deveRejeitarExecutionNula() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                collector.collect(null, review(ReviewStatus.APPROVED), apply(ApplyStatus.COMPLETED), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve gerar itens com confidence MEDIUM quando review e apply forem limpos (sem warnings)")
    void deveGerarConfidenceMediumQuandoLimpo() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComExecution());
        assertThat(items).allMatch(i -> i.confidence() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Deve gerar itens com confidence LOW quando review tiver warnings")
    void deveGerarConfidenceLowQuandoReviewComWarnings() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED_WITH_WARNINGS),
                apply(ApplyStatus.COMPLETED), evidenceComExecution());
        assertThat(items).allMatch(i -> i.confidence() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence.LOW);
    }

    @Test
    @DisplayName("Deve gerar itens com confidence LOW quando apply tiver warnings")
    void deveGerarConfidenceLowQuandoApplyComWarnings() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED_WITH_WARNINGS), evidenceComExecution());
        assertThat(items).allMatch(i -> i.confidence() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence.LOW);
    }

    @Test
    @DisplayName("Item com confidence LOW nunca deve ser reusable=true")
    void itemComConfidenceLowNuncaDeveSerReusable() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED_WITH_WARNINGS),
                apply(ApplyStatus.COMPLETED), evidenceComExecution());
        assertThat(items).allMatch(i -> !i.reusable());
    }

    @Test
    @DisplayName("Deve gerar item REVIEW_RULE de scope FRAMEWORK a partir de evidência de review")
    void deveGerarItemReviewRuleFramework() {
        List<LearningItem> items = collector.collect(execution(ExecutionStatus.PASSED), review(ReviewStatus.APPROVED),
                apply(ApplyStatus.COMPLETED), evidenceComReview());
        assertThat(items).anyMatch(i -> i.type() == com.br.criarcenariotestes.business.autoqa.model.learning.LearningType.REVIEW_RULE
                && i.scope() == LearningScope.FRAMEWORK);
    }

    // --- helpers ---

    private ExecutionResult execution(ExecutionStatus status) {
        return LearningTestData.passedExecution(UUID.randomUUID(), "PLAYWRIGHT", 3).status() == status
                ? LearningTestData.passedExecution(UUID.randomUUID(), "PLAYWRIGHT", 3)
                : new ExecutionResult(UUID.randomUUID(), LearningTestData.command(), status,
                        status == ExecutionStatus.PASSED ? 0 : 1, java.time.Instant.now(), java.time.Instant.now(),
                        java.time.Duration.ofSeconds(1), "out", "", false, false, List.of(), List.of(), true);
    }

    private CodeReviewResult review(ReviewStatus status) {
        UUID id = UUID.randomUUID();
        return new CodeReviewResult(id, List.of(), List.of(), List.of(), List.of("no-hardcoded-wait"), List.of(), List.of(),
                status, com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence.HIGH, false, true);
    }

    private ApplyResult apply(ApplyStatus status) {
        return LearningTestData.completedApply(UUID.randomUUID(), "tests/login.spec.ts").status() == status
                ? LearningTestData.completedApply(UUID.randomUUID(), "tests/login.spec.ts")
                : new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), "projeto", "backup",
                        status, false, true);
    }

    private List<LearningEvidence> evidenceComGeneration() {
        return List.of(new LearningEvidence("generation", "reused-file", "tests/support/loginPage.ts",
                "tests/support/loginPage.ts", "Arquivo reaproveitado durante a geração", null, true));
    }

    private List<LearningEvidence> evidenceComExecution() {
        return List.of(new LearningEvidence("execution", "test-summary", "PLAYWRIGHT", null,
                "3 passaram, 0 falharam de 3 testes (PLAYWRIGHT)", null, true));
    }

    private List<LearningEvidence> evidenceComReview() {
        return List.of(new LearningEvidence("review", "passed-rule", "no-hardcoded-wait", null,
                "Regra de review aprovada: no-hardcoded-wait", null, true));
    }
}
