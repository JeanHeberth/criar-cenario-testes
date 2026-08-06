package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureCategory;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Coleta aprendizado determinístico a partir de execuções FAILED com
 * FailureAnalysisResult válido. Nunca marca item como reusable, nunca conclui
 * recorrência a partir de uma única execução (sempre scope PROJECT, sempre
 * PENDING + revisão humana) e nunca transforma falha operacional em
 * aprendizado funcional.
 */
public class NegativeLearningCollector {

    private static final int MAX_ITEMS = 10;
    private static final Set<FailureAnalysisStatus> VALID_ANALYZED_STATUS =
            EnumSet.of(FailureAnalysisStatus.ANALYZED, FailureAnalysisStatus.ANALYZED_WITH_WARNINGS);
    private static final Set<FailureCategory> REGENERATION_CATEGORIES = EnumSet.of(
            FailureCategory.COMPILATION_FAILURE, FailureCategory.SYNTAX_FAILURE,
            FailureCategory.IMPORT_FAILURE, FailureCategory.SETUP_FAILURE);
    private static final Set<FailureCategory> ANTI_PATTERN_CATEGORIES = EnumSet.of(
            FailureCategory.APPLICATION_DEFECT, FailureCategory.TEST_DEFECT);

    public List<LearningItem> collect(ExecutionResult execution, FailureAnalysisResult failureAnalysis,
                                       List<LearningEvidence> evidence) {
        Objects.requireNonNull(execution, "execution must not be null");
        Objects.requireNonNull(failureAnalysis, "failureAnalysis must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");

        if (execution.status() != ExecutionStatus.FAILED) {
            return List.of();
        }
        if (!failureAnalysis.valid() || !VALID_ANALYZED_STATUS.contains(failureAnalysis.status())) {
            return List.of();
        }
        if (failureAnalysis.findings().isEmpty()) {
            return List.of();
        }

        List<LearningItem> items = new ArrayList<>();
        List<FailureFinding> findings = failureAnalysis.findings();
        int limit = Math.min(findings.size(), MAX_ITEMS);
        for (int i = 0; i < limit; i++) {
            FailureFinding finding = findings.get(i);
            List<LearningEvidence> matching = evidence.stream()
                    .filter(e -> e.source().equals("failure-analysis") && finding.code().equals(e.referenceId()))
                    .toList();
            if (matching.isEmpty()) {
                continue;
            }
            items.add(buildItem(finding, matching));
        }
        return List.copyOf(items);
    }

    private LearningItem buildItem(FailureFinding finding, List<LearningEvidence> matchingEvidence) {
        LearningType type = resolveType(finding.category());
        String title = resolveTitle(type, finding);
        String recommendation = resolveRecommendation(type);
        List<String> relatedFailureCodes = List.of(finding.code());
        String id = LearningIdGenerator.generate(type, LearningScope.PROJECT, title, List.of(), finding.relatedFiles());

        return new LearningItem(id, type, LearningScope.PROJECT, LearningSource.FAILURE_ANALYSIS, title,
                finding.description(), recommendation, LearningConfidence.MEDIUM, LearningApprovalStatus.PENDING,
                matchingEvidence, List.of(), finding.relatedFiles(), relatedFailureCodes, List.of("falha"),
                false, false, true);
    }

    private LearningType resolveType(FailureCategory category) {
        if (category == FailureCategory.FLAKY_TEST) {
            return LearningType.RETRY_STRATEGY;
        }
        if (REGENERATION_CATEGORIES.contains(category)) {
            return LearningType.REGENERATION_STRATEGY;
        }
        if (ANTI_PATTERN_CATEGORIES.contains(category)) {
            return LearningType.ANTI_PATTERN;
        }
        return LearningType.FAILURE_PATTERN;
    }

    private String resolveTitle(LearningType type, FailureFinding finding) {
        return switch (type) {
            case RETRY_STRATEGY -> "Falha instável (flaky) detectada";
            case REGENERATION_STRATEGY -> "Falha que sugere necessidade de regeneração de arquivo";
            case ANTI_PATTERN -> "Padrão de falha associado a defeito de aplicação";
            default -> "Padrão de falha determinístico: " + finding.category();
        };
    }

    private String resolveRecommendation(LearningType type) {
        return switch (type) {
            case RETRY_STRATEGY -> "Considerar estratégia de retry para testes com este padrão";
            case REGENERATION_STRATEGY -> "Considerar regeneração do arquivo afetado";
            case ANTI_PATTERN -> "Investigar o comportamento da aplicação antes de reexecutar";
            default -> "Revisar a causa provável antes de reexecutar o cenário";
        };
    }
}
