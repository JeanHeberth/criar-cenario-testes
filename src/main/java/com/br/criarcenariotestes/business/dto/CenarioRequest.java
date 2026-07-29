package com.br.criarcenariotestes.business.dto;

import com.br.criarcenariotestes.business.workflow.WorkflowType;

public record CenarioRequest(
        String titulo,
        String regraDeNegocio,
        String agent,
        WorkflowType workflowType
) {
    public CenarioRequest(String titulo, String regraDeNegocio, String agent) {
        this(titulo, regraDeNegocio, agent, WorkflowType.COMPLETO);
    }
}

