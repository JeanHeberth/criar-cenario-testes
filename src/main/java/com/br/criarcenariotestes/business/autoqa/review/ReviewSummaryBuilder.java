package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Deriva deterministicamente o status (por arquivo e global), humanReviewRequired e valid
 * a partir do CONJUNTO FINAL de issues (estáticas + IA já mescladas pelo CodeReviewService).
 * O "status" auto-reportado pela IA nunca é usado como fonte da verdade aqui.
 */
@Component
public class ReviewSummaryBuilder {

    public FileReviewStatus deriveFileStatus(List<ReviewIssue> issues) {
        Objects.requireNonNull(issues, "issues must not be null");
        if (hasSeverity(issues, ReviewSeverity.CRITICAL)) return FileReviewStatus.BLOCKED;
        if (hasSeverity(issues, ReviewSeverity.HIGH)) return FileReviewStatus.CHANGES_REQUIRED;
        if (hasSeverity(issues, ReviewSeverity.MEDIUM) || hasSeverity(issues, ReviewSeverity.LOW)) return FileReviewStatus.APPROVED_WITH_WARNINGS;
        return FileReviewStatus.APPROVED;
    }

    public ReviewStatus deriveGlobalStatus(List<FileReviewResult> files, List<ReviewIssue> globalIssues) {
        Objects.requireNonNull(files, "files must not be null");
        Objects.requireNonNull(globalIssues, "globalIssues must not be null");
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.CRITICAL)) return ReviewStatus.BLOCKED;
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.HIGH)) return ReviewStatus.CHANGES_REQUIRED;
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.MEDIUM)
                || anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.LOW)) {
            return ReviewStatus.APPROVED_WITH_WARNINGS;
        }
        return ReviewStatus.APPROVED;
    }

    public boolean deriveHumanReviewRequired(List<FileReviewResult> files, List<ReviewIssue> globalIssues) {
        return anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.CRITICAL);
    }

    public boolean deriveValid(ReviewStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return status != ReviewStatus.INVALID;
    }

    public String buildAgentSummary(CodeReviewResult result) {
        Objects.requireNonNull(result, "result must not be null");
        long totalIssues = result.globalIssues().size()
                + result.files().stream().mapToLong(f -> f.issues().size()).sum();
        return "Revisão concluída: " + result.status() + " / " + result.files().size() + " arquivos / " + totalIssues + " issues";
    }

    private boolean anyFileOrGlobalHasSeverity(List<FileReviewResult> files, List<ReviewIssue> globalIssues, ReviewSeverity severity) {
        return hasSeverity(globalIssues, severity)
                || files.stream().anyMatch(f -> hasSeverity(f.issues(), severity));
    }

    private boolean hasSeverity(List<ReviewIssue> issues, ReviewSeverity severity) {
        return issues.stream().anyMatch(i -> i != null && i.severity() == severity);
    }
}
