package com.br.criarcenariotestes.business.autoqa.model.context;

/**
 * Registro imutável de problema encontrado durante o workflow Auto QA.
 */
public record WorkflowIssue(

        IssueSeverity severity,

        String step,

        String code,

        String message,

        String suggestion

) {

    public enum IssueSeverity {
        INFO, WARNING, ERROR, BLOCKER
    }

    public static WorkflowIssue blocker(String step, String code, String message) {
        return new WorkflowIssue(IssueSeverity.BLOCKER, step, code, message, null);
    }

    public static WorkflowIssue error(String step, String code, String message) {
        return new WorkflowIssue(IssueSeverity.ERROR, step, code, message, null);
    }

    public static WorkflowIssue warning(String step, String code, String message, String suggestion) {
        return new WorkflowIssue(IssueSeverity.WARNING, step, code, message, suggestion);
    }

    public boolean isBlocker() {
        return severity == IssueSeverity.BLOCKER;
    }

    public boolean isError() {
        return severity == IssueSeverity.ERROR || isBlocker();
    }
}
