package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSuggestion;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewWarning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO exclusivo para a resposta bruta e não confiável da IA. Nunca é registrado
 * no AutoQaContext e nunca possui executionId — quem materializa o CodeReviewResult
 * final é sempre o CodeReviewService.
 */
public record CodeReviewAiResponse(
        List<AiFileReview> files,
        List<ReviewIssue> globalIssues,
        List<ReviewSuggestion> suggestions,
        List<String> passedRules,
        List<String> skippedRules,
        List<ReviewWarning> warnings,
        ReviewStatus status,
        ReviewConfidence confidence,
        boolean humanReviewRequired,
        boolean valid
) {
    public CodeReviewAiResponse {
        files = copyList(files);
        globalIssues = copyList(globalIssues);
        suggestions = copyList(suggestions);
        passedRules = copyList(passedRules);
        skippedRules = copyList(skippedRules);
        warnings = copyList(warnings);
        confidence = confidence == null ? ReviewConfidence.UNKNOWN : confidence;
    }

    public record AiFileReview(
            String relativePath,
            FileReviewStatus status,
            List<ReviewIssue> issues,
            List<ReviewSuggestion> suggestions,
            List<String> passedRules,
            List<String> skippedRules,
            ReviewConfidence confidence,
            boolean valid
    ) {
        public AiFileReview {
            relativePath = relativePath == null ? null : relativePath.trim();
            issues = copyList(issues);
            suggestions = copyList(suggestions);
            passedRules = copyList(passedRules);
            skippedRules = copyList(skippedRules);
            confidence = confidence == null ? ReviewConfidence.UNKNOWN : confidence;
        }
    }

    private static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
