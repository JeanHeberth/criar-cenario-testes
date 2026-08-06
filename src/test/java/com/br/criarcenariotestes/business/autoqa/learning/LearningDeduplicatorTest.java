package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningDeduplicator - Testes Unitários")
class LearningDeduplicatorTest {

    private final LearningDeduplicator deduplicator = new LearningDeduplicator();

    @Test
    @DisplayName("Deve manter itens sem conflito de id")
    void deveManterItensSemConflito() {
        LearningItem a = item("id-a", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem b = item("id-b", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(a, b), List.of());
        assertThat(result.items()).containsExactly(a, b);
    }

    @Test
    @DisplayName("Determinístico deve prevalecer sobre IA com mesmo id")
    void deterministicoDevePrevalecerSobreIa() {
        LearningItem deterministic = item("id-x", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem ai = item("id-x", LearningSource.AI_SUGGESTION, LearningConfidence.HIGH, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(deterministic), List.of(ai));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).source()).isEqualTo(LearningSource.EXECUTION);
    }

    @Test
    @DisplayName("IA nunca deve substituir item determinístico mesmo com confidence maior")
    void iaNuncaSubstituiDeterministico() {
        LearningItem deterministic = item("id-y", LearningSource.EXECUTION, LearningConfidence.LOW, true, true);
        LearningItem ai = item("id-y", LearningSource.AI_SUGGESTION, LearningConfidence.HIGH, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(deterministic), List.of(ai));
        assertThat(result.items().get(0).source()).isEqualTo(LearningSource.EXECUTION);
        assertThat(result.items().get(0).confidence()).isEqualTo(LearningConfidence.LOW);
    }

    @Test
    @DisplayName("Entre dois determinísticos, maior confidence prevalece")
    void entreDoisDeterministicosMaiorConfidencePrevalece() {
        LearningItem low = item("id-z", LearningSource.EXECUTION, LearningConfidence.LOW, true, true);
        LearningItem high = item("id-z", LearningSource.GENERATION, LearningConfidence.HIGH, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(low, high), List.of());
        assertThat(result.items().get(0).confidence()).isEqualTo(LearningConfidence.HIGH);
    }

    @Test
    @DisplayName("Não deve modificar os itens originais")
    void naoDeveModificarItensOriginais() {
        LearningItem a = item("id-w", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem b = item("id-w", LearningSource.GENERATION, LearningConfidence.HIGH, true, true);
        deduplicator.deduplicate(List.of(a, b), List.of());
        assertThat(a.confidence()).isEqualTo(LearningConfidence.MEDIUM);
        assertThat(b.confidence()).isEqualTo(LearningConfidence.HIGH);
    }

    @Test
    @DisplayName("Deve unir evidências de itens com o mesmo id")
    void deveUnirEvidenciasDeItensComMesmoId() {
        LearningEvidence ev1 = new LearningEvidence("execution", "test-summary", "e1", null, "desc1", null, true);
        LearningEvidence ev2 = new LearningEvidence("generation", "reused-file", "e2", null, "desc2", null, true);
        LearningItem a = itemComEvidencia("id-v", LearningSource.EXECUTION, LearningConfidence.HIGH, List.of(ev1));
        LearningItem b = itemComEvidencia("id-v", LearningSource.GENERATION, LearningConfidence.MEDIUM, List.of(ev2));
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(a, b), List.of());
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).evidence()).containsExactlyInAnyOrder(ev1, ev2);
    }

    @Test
    @DisplayName("Deve emitir warning de conflito quando reusable divergir entre itens com mesmo id")
    void deveEmitirWarningDeConflito() {
        LearningItem a = item("id-u", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem b = item("id-u", LearningSource.GENERATION, LearningConfidence.MEDIUM, true, false);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(a, b), List.of());
        assertThat(result.warnings()).anyMatch(w -> w.code().equals("CONFLICTING_EVIDENCE"));
    }

    @Test
    @DisplayName("Não deve emitir warning quando não houver conflito")
    void naoDeveEmitirWarningSemConflito() {
        LearningItem a = item("id-t", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem b = item("id-t2", LearningSource.GENERATION, LearningConfidence.MEDIUM, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(a, b), List.of());
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve manter ordem estável de primeira aparição")
    void deveManterOrdemEstavel() {
        LearningItem a = item("id-1", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem b = item("id-2", LearningSource.EXECUTION, LearningConfidence.MEDIUM, true, true);
        LearningItem c = item("id-1", LearningSource.GENERATION, LearningConfidence.HIGH, true, true);
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(a, b, c), List.of());
        assertThat(result.items()).extracting(LearningItem::id).containsExactly("id-1", "id-2");
    }

    @Test
    @DisplayName("Deve rejeitar lista determinística nula")
    void deveRejeitarListaDeterministicaNula() {
        assertThatThrownBy(() -> deduplicator.deduplicate(null, List.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar lista de IA nula")
    void deveRejeitarListaDeIaNula() {
        assertThatThrownBy(() -> deduplicator.deduplicate(List.of(), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        LearningDeduplicator.Result result = deduplicator.deduplicate(List.of(), List.of());
        assertThatThrownBy(() -> result.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.warnings().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    // --- helpers ---

    private LearningItem item(String id, LearningSource source, LearningConfidence confidence, boolean positive, boolean reusable) {
        return new LearningItem(id, LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, source, "título", "descrição",
                "recomendação", confidence, LearningApprovalStatus.NOT_REQUIRED, List.of(), List.of(), List.of(),
                List.of(), List.of(), positive, reusable, false);
    }

    private LearningItem itemComEvidencia(String id, LearningSource source, LearningConfidence confidence, List<LearningEvidence> evidence) {
        return new LearningItem(id, LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, source, "título", "descrição",
                "recomendação", confidence, LearningApprovalStatus.NOT_REQUIRED, evidence, List.of(), List.of(),
                List.of(), List.of(), true, true, false);
    }
}
