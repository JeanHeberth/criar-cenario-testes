package com.br.criarcenariotestes.business.autoqa.model.review;

/**
 * DTO não confiável (recebido da IA ou gerado pelo StaticReviewRuleEngine).
 * A validação semântica completa ocorre em CodeReviewValidator.
 */
public record ReviewIssue(
        String code,
        ReviewCategory category,
        ReviewSeverity severity,
        String relativePath,
        Integer line,
        String message,
        String evidence,
        String recommendation,
        boolean blocking
) {
    public ReviewIssue {
        code = code == null ? null : code.trim();
        relativePath = relativePath == null ? null : relativePath.trim();
        message = message == null ? null : message.trim();
        evidence = evidence == null ? null : evidence.trim();
        recommendation = recommendation == null ? null : recommendation.trim();
    }
}
