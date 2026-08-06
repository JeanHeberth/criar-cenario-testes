package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningValidationException;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningValidator - Testes Unitários")
class LearningValidatorTest {

    private final LearningValidator validator = new LearningValidator();

    @Test
    @DisplayName("Deve aceitar e retornar a mesma instância para resultado válido")
    void deveAceitarResultadoValido() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        LearningResult validated = validator.validate(result, List.of(item));
        assertThat(validated).isSameAs(result);
    }

    @Test
    @DisplayName("Deve rejeitar item de scope GLOBAL")
    void deveRejeitarScopeGlobal() {
        LearningItem item = itemComScope(LearningScope.GLOBAL, LearningApprovalStatus.PENDING, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar item de scope TEAM")
    void deveRejeitarScopeTeam() {
        LearningItem item = itemComScope(LearningScope.TEAM, LearningApprovalStatus.PENDING, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item PROJECT deve nascer PENDING — deve rejeitar APPROVED")
    void deveRejeitarProjectApproved() {
        LearningItem item = itemComScope(LearningScope.PROJECT, LearningApprovalStatus.APPROVED, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item FRAMEWORK deve nascer PENDING — deve rejeitar NOT_REQUIRED")
    void deveRejeitarFrameworkNotRequired() {
        LearningItem item = itemComScope(LearningScope.FRAMEWORK, LearningApprovalStatus.NOT_REQUIRED, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar approvalStatus APPROVED em qualquer item nesta fase")
    void deveRejeitarApprovedEmQualquerItem() {
        LearningItem item = itemComScope(LearningScope.EXECUTION, LearningApprovalStatus.APPROVED, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar approvalStatus REJECTED em qualquer item nesta fase")
    void deveRejeitarRejectedEmQualquerItem() {
        LearningItem item = itemComScope(LearningScope.EXECUTION, LearningApprovalStatus.REJECTED, LearningConfidence.MEDIUM, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item PROJECT deve exigir humanReviewRequired=true")
    void itemProjectDeveExigirHumanReview() {
        LearningItem item = new LearningItem("id", LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                LearningSource.PROJECT_KNOWLEDGE, "t", "d", "r", LearningConfidence.MEDIUM, LearningApprovalStatus.PENDING,
                List.of(evidence()), List.of(), List.of(), List.of(), List.of(), true, true, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item PROJECT nunca pode ter confidence HIGH nesta fase")
    void itemProjectNuncaPodeSerHigh() {
        LearningItem item = itemComScope(LearningScope.PROJECT, LearningApprovalStatus.PENDING, LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item FRAMEWORK nunca pode ter confidence HIGH nesta fase")
    void itemFrameworkNuncaPodeSerHigh() {
        LearningItem item = itemComScope(LearningScope.FRAMEWORK, LearningApprovalStatus.PENDING, LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Item EXECUTION pode ter confidence HIGH")
    void itemExecutionPodeSerHigh() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        assertThat(validator.validate(result, List.of(item))).isSameAs(result);
    }

    @Test
    @DisplayName("Confidence HIGH sem evidência deve ser rejeitada")
    void confidenceHighSemEvidenciaDeveSerRejeitada() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of());
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Confidence MEDIUM sem evidência deve ser rejeitada")
    void confidenceMediumSemEvidenciaDeveSerRejeitada() {
        LearningItem item = executionItem(LearningConfidence.MEDIUM, List.of());
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("reusable=true sem evidência deve ser rejeitado")
    void reusableSemEvidenciaDeveSerRejeitado() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.LOW, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, true, true);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("reusable=true com confidence LOW deve ser rejeitado")
    void reusableComConfidenceLowDeveSerRejeitado() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.LOW, LearningApprovalStatus.NOT_REQUIRED,
                List.of(evidence()), List.of(), List.of(), List.of(), List.of(), true, true, true);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Confidence LOW sem humanReviewRequired deve ser rejeitada")
    void confidenceLowSemHumanReviewDeveSerRejeitada() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.LOW, LearningApprovalStatus.NOT_REQUIRED,
                List.of(evidence()), List.of(), List.of(), List.of(), List.of(), true, false, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Confidence UNKNOWN sem humanReviewRequired deve ser rejeitada")
    void confidenceUnknownSemHumanReviewDeveSerRejeitada() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.UNKNOWN, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, false, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relatedFiles com caminho absoluto")
    void deveRejeitarRelatedFilesComCaminhoAbsoluto() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.HIGH, LearningApprovalStatus.NOT_REQUIRED,
                List.of(evidence()), List.of(), List.of("/etc/passwd"), List.of(), List.of(), true, true, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relatedFiles com path traversal")
    void deveRejeitarRelatedFilesComTraversal() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.HIGH, LearningApprovalStatus.NOT_REQUIRED,
                List.of(evidence()), List.of(), List.of("../../etc/passwd"), List.of(), List.of(), true, true, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar descrição contendo diff/patch")
    void deveRejeitarDescricaoComDiffPatch() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "aplique este diff no arquivo", "r", LearningConfidence.HIGH,
                LearningApprovalStatus.NOT_REQUIRED, List.of(evidence()), List.of(), List.of(), List.of(), List.of(),
                true, true, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar item que contenha caminho absoluto na descrição")
    void deveRejeitarDescricaoComCaminhoAbsoluto() {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "erro em /Users/qualquer/projeto/secreto", "r", LearningConfidence.HIGH,
                LearningApprovalStatus.NOT_REQUIRED, List.of(evidence()), List.of(), List.of(), List.of(), List.of(),
                true, true, false);
        LearningResult result = collectedResult(List.of(item));
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve preservar item determinístico — rejeitar quando IA remove/substitui")
    void deveRejeitarQuandoDeterministicoForRemovido() {
        LearningItem deterministic = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of()); // determinístico ausente do resultado final
        assertThatThrownBy(() -> validator.validate(result, List.of(deterministic)))
                .isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar quando item determinístico foi substituído por item de origem IA com mesmo id")
    void deveRejeitarQuandoDeterministicoForSubstituidoPorIa() {
        LearningItem deterministic = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningItem aiReplacement = new LearningItem(deterministic.id(), deterministic.type(), deterministic.scope(),
                LearningSource.AI_SUGGESTION, deterministic.title(), deterministic.description(), deterministic.recommendation(),
                deterministic.confidence(), deterministic.approvalStatus(), deterministic.evidence(), List.of(), List.of(),
                List.of(), List.of(), true, true, false);
        LearningResult result = collectedResult(List.of(aiReplacement));
        assertThatThrownBy(() -> validator.validate(result, List.of(deterministic)))
                .isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("REVIEW_REQUIRED deve exigir ao menos um item PENDING")
    void reviewRequiredDeveExigirItemPendente() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(item), List.of(), List.of(),
                LearningStatus.REVIEW_REQUIRED, LearningConfidence.HIGH, 1, 0, false, true);
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("COLLECTED_WITH_WARNINGS deve exigir ao menos um warning")
    void collectedWithWarningsDeveExigirWarning() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(item), List.of(), List.of(),
                LearningStatus.COLLECTED_WITH_WARNINGS, LearningConfidence.HIGH, 1, 0, false, true);
        assertThatThrownBy(() -> validator.validate(result, List.of(item))).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("BLOCKED deve exigir ao menos um warning")
    void blockedDeveExigirWarning() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.BLOCKED, LearningConfidence.UNKNOWN, 0, 0, true, true);
        assertThatThrownBy(() -> validator.validate(result, List.of())).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("COLLECTED exige ao menos um item")
    void collectedExigeItem() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.COLLECTED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThatThrownBy(() -> validator.validate(result, List.of())).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("INVALID deve implicar valid=false")
    void invalidDeveImplicarValidFalse() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.INVALID, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThatThrownBy(() -> validator.validate(result, List.of())).isInstanceOf(LearningValidationException.class);
    }

    @Test
    @DisplayName("Não deve modificar o resultado (retorna a mesma instância)")
    void naoDeveModificarResultado() {
        LearningItem item = executionItem(LearningConfidence.HIGH, List.of(evidence()));
        LearningResult result = collectedResult(List.of(item));
        LearningResult validated = validator.validate(result, List.of(item));
        assertThat(validated).isSameAs(result);
        assertThat(validated.items()).isSameAs(result.items());
    }

    @Test
    @DisplayName("Deve rejeitar result nulo")
    void deveRejeitarResultNulo() {
        assertThatThrownBy(() -> validator.validate(null, List.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar deterministicItems nulo")
    void deveRejeitarDeterministicItemsNulo() {
        LearningResult result = collectedResult(List.of());
        assertThatThrownBy(() -> validator.validate(result, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SKIPPED sem itens deve ser válido")
    void skippedSemItensDeveSerValido() {
        LearningResult result = new LearningResult(UUID.randomUUID(), List.of(), List.of(), List.of(),
                LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        assertThat(validator.validate(result, List.of())).isSameAs(result);
    }

    // --- helpers ---

    private LearningEvidence evidence() {
        return new LearningEvidence("execution", "test-summary", "id", null, "descrição", null, true);
    }

    private LearningItem executionItem(LearningConfidence confidence, List<LearningEvidence> evidence) {
        boolean needsReview = confidence == LearningConfidence.LOW || confidence == LearningConfidence.UNKNOWN;
        return new LearningItem("exec-id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, LearningSource.EXECUTION,
                "t", "d", "r", confidence, LearningApprovalStatus.NOT_REQUIRED, evidence, List.of(), List.of(), List.of(),
                List.of(), true, !evidence.isEmpty(), needsReview);
    }

    private LearningItem itemComScope(LearningScope scope, LearningApprovalStatus approval, LearningConfidence confidence,
                                       List<LearningEvidence> evidence) {
        return new LearningItem("id-" + scope, LearningType.REUSABLE_COMPONENT, scope, LearningSource.PROJECT_KNOWLEDGE,
                "t", "d", "r", confidence, approval, evidence, List.of(), List.of(), List.of(), List.of(),
                true, false, true);
    }

    private LearningResult collectedResult(List<LearningItem> items) {
        LearningStatus status = items.isEmpty() ? LearningStatus.SKIPPED : LearningStatus.COLLECTED;
        return new LearningResult(UUID.randomUUID(), items, List.of(), List.of(), status,
                items.isEmpty() ? LearningConfidence.UNKNOWN : LearningConfidence.HIGH, items.size(), 0, false, true);
    }
}
