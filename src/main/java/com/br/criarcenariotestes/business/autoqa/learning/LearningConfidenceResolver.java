package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningConfidence;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningItem;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningScope;

import java.util.Objects;

/**
 * Calcula/ajusta a confidence final de um LearningItem respeitando o teto por
 * scope. Nunca altera approvalStatus, nunca chama IA. Nenhum item de uma
 * única execução é promovido automaticamente a padrão recorrente: PROJECT e
 * FRAMEWORK nunca alcançam HIGH nesta fase, mesmo com múltiplas evidências da
 * mesma execução — só EXECUTION pode, e mesmo assim significa apenas alta
 * confiança naquela execução específica.
 */
public class LearningConfidenceResolver {

    public LearningItem resolve(LearningItem item) {
        Objects.requireNonNull(item, "item must not be null");

        if (item.scope() != LearningScope.EXECUTION && item.scope() != LearningScope.PROJECT
                && item.scope() != LearningScope.FRAMEWORK) {
            return item;
        }

        LearningConfidence resolved = resolveConfidence(item);
        boolean humanReviewRequired = item.humanReviewRequired()
                || resolved == LearningConfidence.LOW || resolved == LearningConfidence.UNKNOWN;

        if (resolved == item.confidence() && humanReviewRequired == item.humanReviewRequired()) {
            return item;
        }

        return new LearningItem(item.id(), item.type(), item.scope(), item.source(), item.title(),
                item.description(), item.recommendation(), resolved, item.approvalStatus(), item.evidence(),
                item.relatedComponents(), item.relatedFiles(), item.relatedFailureCodes(), item.tags(),
                item.positive(), item.reusable(), humanReviewRequired);
    }

    private LearningConfidence resolveConfidence(LearningItem item) {
        int evidenceCount = item.evidence().size();
        if (evidenceCount == 0) {
            return LearningConfidence.UNKNOWN;
        }
        if (item.scope() == LearningScope.EXECUTION) {
            return evidenceCount >= 2 ? LearningConfidence.HIGH : LearningConfidence.MEDIUM;
        }
        // PROJECT / FRAMEWORK: teto MEDIUM — uma única execução nunca prova recorrência.
        return item.confidence() == LearningConfidence.HIGH ? LearningConfidence.MEDIUM : item.confidence();
    }
}
