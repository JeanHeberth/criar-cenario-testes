package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningConfidenceResolver - Testes Unitários")
class LearningConfidenceResolverTest {

    private final LearningConfidenceResolver resolver = new LearningConfidenceResolver();

    @Test
    @DisplayName("Item EXECUTION com múltiplas evidências concordantes deve alcançar HIGH")
    void itemExecutionComMultiplasEvidenciasDeveSerHigh() {
        LearningItem item = item(LearningScope.EXECUTION, LearningConfidence.MEDIUM, evidences(2));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.HIGH);
    }

    @Test
    @DisplayName("Item EXECUTION com uma única evidência permanece MEDIUM")
    void itemExecutionComUmaEvidenciaPermaneceMedium() {
        LearningItem item = item(LearningScope.EXECUTION, LearningConfidence.MEDIUM, evidences(1));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Item EXECUTION sem evidência deve ser UNKNOWN")
    void itemExecutionSemEvidenciaDeveSerUnknown() {
        LearningItem item = item(LearningScope.EXECUTION, LearningConfidence.MEDIUM, List.of());
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.UNKNOWN);
    }

    @Test
    @DisplayName("Item PROJECT nunca deve alcançar HIGH mesmo com múltiplas evidências da mesma execução")
    void itemProjectNuncaAlcancaHigh() {
        LearningItem item = item(LearningScope.PROJECT, LearningConfidence.HIGH, evidences(5));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isNotEqualTo(LearningConfidence.HIGH);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Item FRAMEWORK nunca deve alcançar HIGH mesmo com múltiplas evidências da mesma execução")
    void itemFrameworkNuncaAlcancaHigh() {
        LearningItem item = item(LearningScope.FRAMEWORK, LearningConfidence.HIGH, evidences(5));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Item PROJECT sem evidência deve ser UNKNOWN")
    void itemProjectSemEvidenciaDeveSerUnknown() {
        LearningItem item = item(LearningScope.PROJECT, LearningConfidence.MEDIUM, List.of());
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.UNKNOWN);
    }

    @Test
    @DisplayName("Não deve alterar approvalStatus")
    void naoDeveAlterarApprovalStatus() {
        LearningItem item = item(LearningScope.PROJECT, LearningConfidence.MEDIUM, evidences(1));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.approvalStatus()).isEqualTo(item.approvalStatus());
    }

    @Test
    @DisplayName("Deve forçar humanReviewRequired=true quando confidence final for LOW ou UNKNOWN")
    void deveForcarRevisaoHumanaQuandoConfidenceBaixa() {
        LearningItem item = item(LearningScope.EXECUTION, LearningConfidence.MEDIUM, List.of());
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.confidence()).isEqualTo(LearningConfidence.UNKNOWN);
        assertThat(resolved.humanReviewRequired()).isTrue();
    }

    @Test
    @DisplayName("Não deve alterar o id do item")
    void naoDeveAlterarId() {
        LearningItem item = item(LearningScope.EXECUTION, LearningConfidence.MEDIUM, evidences(2));
        LearningItem resolved = resolver.resolve(item);
        assertThat(resolved.id()).isEqualTo(item.id());
    }

    @Test
    @DisplayName("Deve rejeitar item nulo")
    void deveRejeitarItemNulo() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(NullPointerException.class);
    }

    private List<LearningEvidence> evidences(int count) {
        List<LearningEvidence> list = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new LearningEvidence("execution", "test-summary", "id" + i, null, "desc " + i, null, true));
        }
        return list;
    }

    private LearningItem item(LearningScope scope, LearningConfidence confidence, List<LearningEvidence> evidence) {
        LearningApprovalStatus approval = scope == LearningScope.EXECUTION
                ? LearningApprovalStatus.NOT_REQUIRED : LearningApprovalStatus.PENDING;
        return new LearningItem("id-fixo", LearningType.COMMAND_PATTERN, scope, LearningSource.EXECUTION,
                "título", "descrição", "recomendação", confidence, approval, evidence, List.of(), List.of(),
                List.of(), List.of(), true, true, scope != LearningScope.EXECUTION);
    }
}
