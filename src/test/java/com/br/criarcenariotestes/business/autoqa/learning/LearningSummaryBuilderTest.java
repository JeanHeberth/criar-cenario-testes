package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningSummaryBuilder - Testes Unitários")
class LearningSummaryBuilderTest {

    private final LearningSummaryBuilder builder = new LearningSummaryBuilder();

    @Test
    @DisplayName("Deve retornar BLOCKED com warning operacional quando operationalBlocked=true")
    void deveRetornarBlockedQuandoOperacional() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.ERROR, true, List.of(), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.BLOCKED);
        assertThat(result.warnings()).anyMatch(w -> w.code().equals("OPERATIONAL_FAILURE"));
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Não deve chamar IA (não depende de nada além dos itens já prontos) quando BLOCKED")
    void blockedNaoDependeDeItens() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.BLOCKED, true, List.of(executionItem(true)), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.BLOCKED);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar SKIPPED quando não houver itens")
    void deveRetornarSkippedSemItens() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false, List.of(), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.SKIPPED);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar COLLECTED para PASSED com itens válidos e sem warnings")
    void deveRetornarCollectedParaPassedSemWarnings() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(executionItem(true)), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.COLLECTED);
    }

    @Test
    @DisplayName("Deve retornar COLLECTED_WITH_WARNINGS para PASSED com warnings não bloqueantes")
    void deveRetornarCollectedComWarnings() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(executionItem(true)), List.of(), List.of(new LearningWarning("DUPLICATE_LEARNING", "duplicado", false)));
        assertThat(result.status()).isEqualTo(LearningStatus.COLLECTED_WITH_WARNINGS);
    }

    @Test
    @DisplayName("Deve retornar REVIEW_REQUIRED quando houver item PROJECT/FRAMEWORK pendente")
    void deveRetornarReviewRequiredComItemPendente() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(projectItem()), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.REVIEW_REQUIRED);
        assertThat(result.items()).anyMatch(i -> i.approvalStatus() == LearningApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("Deve retornar REVIEW_REQUIRED para FAILED com item PROJECT pendente")
    void deveRetornarReviewRequiredParaFailedComPendente() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.FAILED, false,
                List.of(projectItem()), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("Deve adicionar warning SINGLE_EXECUTION_ONLY para item PROJECT")
    void deveAdicionarWarningSingleExecutionParaProject() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(projectItem()), List.of(), List.of());
        assertThat(result.warnings()).anyMatch(w -> w.code().equals("SINGLE_EXECUTION_ONLY"));
    }

    @Test
    @DisplayName("Deve contar positiveLearnings e negativeLearnings corretamente")
    void deveContarPositiveENegativeLearnings() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(executionItem(true), executionItem(false)), List.of(), List.of());
        assertThat(result.positiveLearnings()).isEqualTo(1);
        assertThat(result.negativeLearnings()).isEqualTo(1);
    }

    @Test
    @DisplayName("humanReviewRequired deve ser true quando algum item exigir revisão")
    void humanReviewRequiredDeveSerTrueQuandoAlgumItemExigir() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(projectItem()), List.of(), List.of());
        assertThat(result.humanReviewRequired()).isTrue();
    }

    @Test
    @DisplayName("Confidence final deve ser a melhor confidence entre os itens")
    void confidenceFinalDeveSerAMelhorEntreOsItens() {
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(executionItem(true)), List.of(), List.of());
        assertThat(result.confidence()).isEqualTo(LearningConfidence.HIGH);
    }

    @Test
    @DisplayName("Status sugerido pela IA não deve ser considerado (não há parâmetro para isso)")
    void statusDaIaNaoEhFonteDaVerdade() {
        // O builder não recebe nenhum "status sugerido" — a assinatura do método já garante isso.
        LearningResult result = builder.summarize(UUID.randomUUID(), ExecutionStatus.PASSED, false,
                List.of(executionItem(true)), List.of(), List.of());
        assertThat(result.status()).isEqualTo(LearningStatus.COLLECTED);
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> builder.summarize(null, ExecutionStatus.PASSED, false, List.of(), List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executionStatus nulo")
    void deveRejeitarExecutionStatusNulo() {
        assertThatThrownBy(() -> builder.summarize(UUID.randomUUID(), null, false, List.of(), List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    private LearningItem executionItem(boolean positive) {
        return new LearningItem(UUID.randomUUID().toString(), LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "título", "descrição", "recomendação", LearningConfidence.HIGH,
                LearningApprovalStatus.NOT_REQUIRED, List.of(), List.of(), List.of(), List.of(), List.of(),
                positive, true, false);
    }

    private LearningItem projectItem() {
        return new LearningItem(UUID.randomUUID().toString(), LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                LearningSource.PROJECT_KNOWLEDGE, "título", "descrição", "recomendação", LearningConfidence.MEDIUM,
                LearningApprovalStatus.PENDING, List.of(), List.of(), List.of(), List.of(), List.of(),
                true, true, true);
    }
}
