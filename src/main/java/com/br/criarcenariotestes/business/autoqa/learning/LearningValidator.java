package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningValidationException;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validação pós-construção pura: não chama IA, não modifica o resultado —
 * retorna a mesma instância ou lança LearningValidationException. Última
 * linha de defesa contra scope proibido, aprovação indevida, reusable sem
 * evidência e substituição de aprendizado determinístico pela IA.
 */
public class LearningValidator {

    private static final Pattern ABSOLUTE_PATH = Pattern.compile("(?i)(/Users/|/home/|[A-Z]:\\\\)");
    private static final Pattern FORBIDDEN_CONTENT = Pattern.compile("(?i)\\b(diff|patch)\\b");

    public LearningResult validate(LearningResult result, List<LearningItem> deterministicItems) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(deterministicItems, "deterministicItems must not be null");

        for (LearningItem item : result.items()) {
            validateItem(item);
        }
        validateDeterministicPreserved(result, deterministicItems);
        validateStatusCoherence(result);

        return result;
    }

    private void validateItem(LearningItem item) {
        if (item.scope() == LearningScope.GLOBAL) {
            throw new LearningValidationException("scope GLOBAL is not allowed in this phase: " + item.id());
        }
        if (item.scope() == LearningScope.TEAM) {
            throw new LearningValidationException("scope TEAM is not allowed in this phase: " + item.id());
        }
        if (item.approvalStatus() == LearningApprovalStatus.APPROVED) {
            throw new LearningValidationException("approvalStatus APPROVED is not allowed in this phase: " + item.id());
        }
        if (item.approvalStatus() == LearningApprovalStatus.REJECTED) {
            throw new LearningValidationException("approvalStatus REJECTED is not allowed in this phase: " + item.id());
        }

        boolean projectOrFramework = item.scope() == LearningScope.PROJECT || item.scope() == LearningScope.FRAMEWORK;
        if (projectOrFramework) {
            if (item.approvalStatus() != LearningApprovalStatus.PENDING) {
                throw new LearningValidationException("PROJECT/FRAMEWORK item must start as PENDING: " + item.id());
            }
            if (!item.humanReviewRequired()) {
                throw new LearningValidationException("PROJECT/FRAMEWORK item requires humanReviewRequired=true: " + item.id());
            }
            if (item.confidence() == LearningConfidence.HIGH) {
                throw new LearningValidationException("PROJECT/FRAMEWORK item must not reach HIGH confidence in this phase: " + item.id());
            }
        }

        if ((item.confidence() == LearningConfidence.HIGH || item.confidence() == LearningConfidence.MEDIUM)
                && item.evidence().isEmpty()) {
            throw new LearningValidationException("HIGH/MEDIUM confidence requires evidence: " + item.id());
        }
        if ((item.confidence() == LearningConfidence.LOW || item.confidence() == LearningConfidence.UNKNOWN)
                && !item.humanReviewRequired()) {
            throw new LearningValidationException("LOW/UNKNOWN confidence requires humanReviewRequired=true: " + item.id());
        }
        if (item.reusable()) {
            if (item.confidence() != LearningConfidence.HIGH && item.confidence() != LearningConfidence.MEDIUM) {
                throw new LearningValidationException("reusable=true requires HIGH or MEDIUM confidence: " + item.id());
            }
            if (item.evidence().isEmpty()) {
                throw new LearningValidationException("reusable=true requires evidence: " + item.id());
            }
        }

        for (String path : item.relatedFiles()) {
            validateRelativePath(item, path);
        }

        validateTextContent(item, item.title());
        validateTextContent(item, item.description());
        validateTextContent(item, item.recommendation());
    }

    private void validateRelativePath(LearningItem item, String path) {
        if (path == null) {
            return;
        }
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("(?i)^[A-Z]:/.*")
                || normalized.startsWith("../") || normalized.contains("/../") || normalized.equals("..")) {
            throw new LearningValidationException("relatedFiles must be relative and must not contain path traversal: " + item.id());
        }
    }

    private void validateTextContent(LearningItem item, String text) {
        if (text == null) {
            return;
        }
        if (FORBIDDEN_CONTENT.matcher(text).find()) {
            throw new LearningValidationException("item text must not contain diff/patch content: " + item.id());
        }
        if (ABSOLUTE_PATH.matcher(text).find()) {
            throw new LearningValidationException("item text must not contain absolute path: " + item.id());
        }
    }

    private void validateDeterministicPreserved(LearningResult result, List<LearningItem> deterministicItems) {
        for (LearningItem deterministic : deterministicItems) {
            LearningItem surviving = result.items().stream()
                    .filter(i -> i.id().equals(deterministic.id()))
                    .findFirst()
                    .orElse(null);
            if (surviving == null) {
                throw new LearningValidationException("deterministic learning item was omitted: " + deterministic.id());
            }
            if (surviving.source() == LearningSource.AI_SUGGESTION) {
                throw new LearningValidationException("deterministic learning item was replaced by AI: " + deterministic.id());
            }
        }
    }

    private void validateStatusCoherence(LearningResult result) {
        switch (result.status()) {
            case COLLECTED -> {
                if (result.items().isEmpty()) {
                    throw new LearningValidationException("COLLECTED requires at least one item");
                }
            }
            case COLLECTED_WITH_WARNINGS -> {
                if (result.warnings().isEmpty()) {
                    throw new LearningValidationException("COLLECTED_WITH_WARNINGS requires at least one warning");
                }
            }
            case REVIEW_REQUIRED -> {
                boolean hasPending = result.items().stream().anyMatch(i -> i.approvalStatus() == LearningApprovalStatus.PENDING);
                if (!hasPending) {
                    throw new LearningValidationException("REVIEW_REQUIRED requires at least one PENDING item");
                }
            }
            case BLOCKED -> {
                if (result.warnings().isEmpty()) {
                    throw new LearningValidationException("BLOCKED requires at least one warning");
                }
            }
            case INVALID -> {
                if (result.valid()) {
                    throw new LearningValidationException("INVALID status requires valid=false");
                }
            }
            case SKIPPED -> {
                // sem exigências adicionais
            }
        }
    }
}
