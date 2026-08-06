package com.br.criarcenariotestes.business.autoqa.executionapi.model;

/**
 * Status geral do workflow persistido (distinto de AutoQaStage — etapa atual —
 * e de AutoQaOperationStatus — lock da operação assíncrona em andamento).
 */
public enum AutoQaWorkflowStatus {
    CREATED,
    RUNNING,
    WAITING_GENERATION_APPROVAL,
    WAITING_APPLY_APPROVAL,
    WAITING_EXECUTION_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
