package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Coleta aprendizado determinístico a partir de execuções PASSED. Não conclui
 * sucesso apenas porque a execução passou — exige também review aprovado e
 * apply concluído. Não chama IA, não deduplica globalmente, não monta o
 * LearningResult final.
 */
public class PositiveLearningCollector {

    private static final int MAX_ITEMS_PER_SOURCE = 10;
    private static final Set<ReviewStatus> APPROVED_REVIEW = EnumSet.of(ReviewStatus.APPROVED, ReviewStatus.APPROVED_WITH_WARNINGS);
    private static final Set<ApplyStatus> COMPLETED_APPLY = EnumSet.of(ApplyStatus.COMPLETED, ApplyStatus.COMPLETED_WITH_WARNINGS);

    public List<LearningItem> collect(ExecutionResult execution, CodeReviewResult review, ApplyResult applyResult,
                                       List<LearningEvidence> evidence) {
        Objects.requireNonNull(execution, "execution must not be null");
        Objects.requireNonNull(review, "review must not be null");
        Objects.requireNonNull(applyResult, "applyResult must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");

        if (execution.status() != ExecutionStatus.PASSED) {
            return List.of();
        }
        if (!APPROVED_REVIEW.contains(review.status())) {
            return List.of();
        }
        if (!COMPLETED_APPLY.contains(applyResult.status())) {
            return List.of();
        }

        boolean clean = review.status() == ReviewStatus.APPROVED && applyResult.status() == ApplyStatus.COMPLETED;
        LearningConfidence baseline = clean ? LearningConfidence.MEDIUM : LearningConfidence.LOW;

        List<LearningItem> items = new ArrayList<>();
        collectBySource(evidence, "knowledge", MAX_ITEMS_PER_SOURCE).forEach(e -> items.add(reusableComponentItem(e, LearningSource.PROJECT_KNOWLEDGE, baseline)));
        collectBySource(evidence, "generation", MAX_ITEMS_PER_SOURCE).forEach(e -> items.add(reusableComponentItem(e, LearningSource.GENERATION, baseline)));
        collectBySource(evidence, "review", MAX_ITEMS_PER_SOURCE).forEach(e -> items.add(reviewRuleItem(e, baseline)));
        collectBySource(evidence, "apply", MAX_ITEMS_PER_SOURCE).forEach(e -> items.add(applyPatternItem(e, baseline)));
        collectBySource(evidence, "execution", MAX_ITEMS_PER_SOURCE).forEach(e -> items.add(commandPatternItem(e, baseline)));
        return List.copyOf(items);
    }

    private List<LearningEvidence> collectBySource(List<LearningEvidence> evidence, String source, int max) {
        List<LearningEvidence> matched = evidence.stream().filter(e -> e.source().equals(source)).toList();
        return matched.size() <= max ? matched : matched.subList(0, max);
    }

    private LearningItem reusableComponentItem(LearningEvidence evidence, LearningSource source, LearningConfidence baseline) {
        String title = "Componente reutilizado com sucesso";
        List<String> relatedFiles = evidence.relativePath() != null ? List.of(evidence.relativePath()) : List.of();
        String id = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, title, List.of(), relatedFiles);
        boolean reusable = baseline == LearningConfidence.MEDIUM;
        return new LearningItem(id, LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, source, title,
                evidence.description(), "Priorizar reutilização deste componente em cenários semelhantes",
                baseline, LearningApprovalStatus.PENDING, List.of(evidence), List.of(), relatedFiles,
                List.of(), List.of("reuso"), true, reusable, true);
    }

    private LearningItem reviewRuleItem(LearningEvidence evidence, LearningConfidence baseline) {
        String title = "Regra de review confirmada em execução aprovada";
        String id = LearningIdGenerator.generate(LearningType.REVIEW_RULE, LearningScope.FRAMEWORK, title,
                List.of(evidence.referenceId() == null ? "" : evidence.referenceId()), List.of());
        boolean reusable = baseline == LearningConfidence.MEDIUM;
        return new LearningItem(id, LearningType.REVIEW_RULE, LearningScope.FRAMEWORK, LearningSource.REVIEW, title,
                evidence.description(), "Manter esta regra de review ativa",
                baseline, LearningApprovalStatus.PENDING, List.of(evidence),
                List.of(evidence.referenceId() == null ? "" : evidence.referenceId()), List.of(), List.of(),
                List.of("review"), true, reusable, true);
    }

    private LearningItem applyPatternItem(LearningEvidence evidence, LearningConfidence baseline) {
        String title = "Padrão de aplicação de arquivo confirmado";
        List<String> relatedFiles = evidence.relativePath() != null ? List.of(evidence.relativePath()) : List.of();
        String id = LearningIdGenerator.generate(LearningType.PROJECT_STRUCTURE, LearningScope.PROJECT, title, List.of(), relatedFiles);
        boolean reusable = baseline == LearningConfidence.MEDIUM;
        return new LearningItem(id, LearningType.PROJECT_STRUCTURE, LearningScope.PROJECT, LearningSource.APPLY, title,
                evidence.description(), "Manter esta convenção de estrutura de arquivos",
                baseline, LearningApprovalStatus.PENDING, List.of(evidence), List.of(), relatedFiles,
                List.of(), List.of("estrutura"), true, reusable, true);
    }

    private LearningItem commandPatternItem(LearningEvidence evidence, LearningConfidence baseline) {
        String title = "Comando de teste válido confirmado";
        List<String> relatedComponents = evidence.referenceId() != null ? List.of(evidence.referenceId()) : List.of();
        String id = LearningIdGenerator.generate(LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, title, relatedComponents, List.of());
        boolean reusable = baseline == LearningConfidence.MEDIUM;
        boolean humanReviewRequired = baseline == LearningConfidence.LOW;
        return new LearningItem(id, LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, LearningSource.EXECUTION, title,
                evidence.description(), "Reutilizar este comando de execução de testes",
                baseline, LearningApprovalStatus.NOT_REQUIRED, List.of(evidence), relatedComponents,
                List.of(), List.of(), List.of("execucao"), true, reusable, humanReviewRequired);
    }
}
