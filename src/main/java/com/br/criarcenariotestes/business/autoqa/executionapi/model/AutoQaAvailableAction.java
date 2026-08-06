package com.br.criarcenariotestes.business.autoqa.executionapi.model;

/**
 * Ações que o frontend pode oferecer ao usuário, sempre calculadas no backend
 * por AutoQaAvailableActionResolver. O frontend nunca decide sozinho se uma
 * ação é permitida.
 */
public enum AutoQaAvailableAction {
    START,
    CONTINUE,
    GENERATE,
    APPROVE_FILE_UPDATE,
    APPLY,
    APPROVE_EXECUTION,
    EXECUTE,
    CANCEL,
    RETRY,
    VIEW_GENERATED_FILES,
    VIEW_DIFF,
    VIEW_LOGS,
    VIEW_LEARNING,
    NONE
}
