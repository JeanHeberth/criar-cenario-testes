package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Deriva status/contagens/confidence final a partir dos itens já prontos
 * (após coleta, resolução de confidence e deduplicação). Não corrige item
 * inválido, não aprova nada, não chama IA. O status sugerido pela IA nunca é
 * considerado — este builder não recebe esse dado como entrada.
 */
public class LearningSummaryBuilder {

    public LearningResult summarize(UUID executionId, ExecutionStatus executionStatus, boolean operationalBlocked,
                                     List<LearningItem> items, List<LearningEvidence> globalEvidence,
                                     List<LearningWarning> priorWarnings) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(executionStatus, "executionStatus must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(globalEvidence, "globalEvidence must not be null");
        Objects.requireNonNull(priorWarnings, "priorWarnings must not be null");

        List<LearningWarning> warnings = new ArrayList<>(priorWarnings);

        if (operationalBlocked) {
            warnings.add(new LearningWarning("OPERATIONAL_FAILURE",
                    "Status operacional da execução impede aprendizado confiável", true));
            return new LearningResult(executionId, List.of(), List.copyOf(globalEvidence), List.copyOf(warnings),
                    LearningStatus.BLOCKED, LearningConfidence.UNKNOWN, 0, 0, true, true);
        }

        if (items.isEmpty()) {
            return new LearningResult(executionId, List.of(), List.copyOf(globalEvidence), List.copyOf(warnings),
                    LearningStatus.SKIPPED, LearningConfidence.UNKNOWN, 0, 0, false, true);
        }

        boolean pendingProjectOrFramework = false;
        for (LearningItem item : items) {
            if ((item.scope() == LearningScope.PROJECT || item.scope() == LearningScope.FRAMEWORK)) {
                warnings.add(new LearningWarning("SINGLE_EXECUTION_ONLY",
                        "Aprendizado de escopo " + item.scope() + " baseado em uma única execução: " + item.id(), false));
                if (item.approvalStatus() == LearningApprovalStatus.PENDING) {
                    pendingProjectOrFramework = true;
                }
            }
        }

        LearningStatus status;
        if (pendingProjectOrFramework) {
            status = LearningStatus.REVIEW_REQUIRED;
        } else if (executionStatus == ExecutionStatus.PASSED) {
            status = warnings.isEmpty() ? LearningStatus.COLLECTED : LearningStatus.COLLECTED_WITH_WARNINGS;
        } else {
            status = LearningStatus.COLLECTED_WITH_WARNINGS;
        }

        int positive = (int) items.stream().filter(LearningItem::positive).count();
        int negative = (int) items.stream().filter(i -> !i.positive()).count();
        boolean humanReviewRequired = items.stream().anyMatch(LearningItem::humanReviewRequired);
        LearningConfidence confidence = bestConfidence(items);

        return new LearningResult(executionId, List.copyOf(items), List.copyOf(globalEvidence), List.copyOf(warnings),
                status, confidence, positive, negative, humanReviewRequired, true);
    }

    private LearningConfidence bestConfidence(List<LearningItem> items) {
        for (LearningConfidence candidate : List.of(LearningConfidence.HIGH, LearningConfidence.MEDIUM, LearningConfidence.LOW)) {
            if (items.stream().anyMatch(i -> i.confidence() == candidate)) {
                return candidate;
            }
        }
        return LearningConfidence.UNKNOWN;
    }
}
