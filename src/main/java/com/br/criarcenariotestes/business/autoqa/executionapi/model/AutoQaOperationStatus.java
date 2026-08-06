package com.br.criarcenariotestes.business.autoqa.executionapi.model;

/**
 * Status da última operação assíncrona disparada para a execução. IN_PROGRESS
 * é o lock de concorrência: nenhuma nova ação é aceita enquanto este for o
 * valor persistido. Nunca se confunde com AutoQaWorkflowStatus (FSM geral) ou
 * AutoQaStage (etapa atual).
 */
public enum AutoQaOperationStatus {
    IDLE,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED
}
