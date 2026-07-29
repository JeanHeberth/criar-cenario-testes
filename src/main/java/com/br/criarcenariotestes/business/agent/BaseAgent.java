package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.workflow.WorkflowContext;

public interface BaseAgent {
    
    void executar(WorkflowContext context);
    
    String getNome();
    
    default boolean isEnabled(WorkflowContext context) {
        return true;
    }
}
