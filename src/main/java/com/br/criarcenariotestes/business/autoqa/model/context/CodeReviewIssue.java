package com.br.criarcenariotestes.business.autoqa.model.context;

public record CodeReviewIssue(
        Severity severity,
        String file,
        Integer line,
        String code,
        String message,
        String suggestion
) {
    public enum Severity {
        INFO, WARNING, ERROR
    }
}
