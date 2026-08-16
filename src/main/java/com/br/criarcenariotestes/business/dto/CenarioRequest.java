package com.br.criarcenariotestes.business.dto;

import com.br.criarcenariotestes.business.workflow.WorkflowType;

public record CenarioRequest(
        String titulo,
        String regraDeNegocio,
        String agent,
        WorkflowType workflowType,
        // Opcional: quando informada, ZephyrPublisherAgent vincula cada caso
        // de teste criado a essa issue do Jira (POST /testcases/{key}/links/issues).
        // Sem ela, a publicação segue igual a antes, sem vínculo nenhum.
        String jiraIssueKey
) {
    public CenarioRequest(String titulo, String regraDeNegocio, String agent) {
        this(titulo, regraDeNegocio, agent, WorkflowType.COMPLETO, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType) {
        this(titulo, regraDeNegocio, agent, workflowType, null);
    }
}

