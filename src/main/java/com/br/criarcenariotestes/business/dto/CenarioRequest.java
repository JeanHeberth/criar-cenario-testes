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
        String jiraIssueKey,

        // Opcional: pasta raiz no Zephyr, tipicamente a stack de automação
        // ("Java", "Robot", "Postman"). Os casos são criados em
        // "{pastaRaiz}/{titulo}" (ex.: "Java/Login com credenciais válidas"),
        // em vez de soltos na raiz do projeto. Default vem de
        // zephyr.root-folder quando não informada aqui.
        String pastaRaiz
) {
    public CenarioRequest(String titulo, String regraDeNegocio, String agent) {
        this(titulo, regraDeNegocio, agent, WorkflowType.COMPLETO, null, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType) {
        this(titulo, regraDeNegocio, agent, workflowType, null, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType, String jiraIssueKey) {
        this(titulo, regraDeNegocio, agent, workflowType, jiraIssueKey, null);
    }
}

