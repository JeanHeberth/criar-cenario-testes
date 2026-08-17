package com.br.criarcenariotestes.business.dto;

import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.fasterxml.jackson.annotation.JsonAlias;

public record CenarioRequest(
        String titulo,
        String regraDeNegocio,
        String agent,
        WorkflowType workflowType,

        /**
         * Opcional: referência da tarefa a que os casos de teste gerados devem
         * ficar vinculados. Aceita a URL colada do navegador — que é a forma
         * canônica, por ser a única autossuficiente em qualquer rastreador —
         * ou a chave do Jira. Ver ReferenciaTarefaParser.
         *
         * Nome neutro (e não "jiraIssueKey") porque este é contrato público
         * consumido por front, Jenkins e testes: renomear depois, quando um
         * time trouxer Azure DevOps, custaria coordenar todos eles. O alias
         * mantém quem já envia o nome antigo funcionando.
         */
        @JsonAlias("jiraIssueKey")
        String taskRef,

        /**
         * Opcional: pasta de destino no repositório de testes, tipicamente a
         * stack de automação ("Java", "Robot", "Postman"). Os casos são
         * criados em "{pastaDestino}/{titulo}" em vez de soltos na raiz.
         * Default vem de zephyr.root-folder quando não informada.
         */
        @JsonAlias("pastaRaiz")
        String pastaDestino,

        /**
         * Opcional: projeto de destino no repositório de testes. Sobrescreve
         * zephyr.project-key, que é global por ambiente e por isso limita a
         * instância a atender um único time.
         *
         * Quando ausente, é derivado da taskRef: no Jira o projeto está na
         * própria chave ("SCRUM-28" -> "SCRUM"). No Azure DevOps não está no
         * id, e por isso vem da URL — outro motivo para a URL ser a entrada
         * canônica.
         */
        String projectKey
) {
    public CenarioRequest(String titulo, String regraDeNegocio, String agent) {
        this(titulo, regraDeNegocio, agent, WorkflowType.COMPLETO, null, null, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType) {
        this(titulo, regraDeNegocio, agent, workflowType, null, null, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType, String taskRef) {
        this(titulo, regraDeNegocio, agent, workflowType, taskRef, null, null);
    }

    public CenarioRequest(String titulo, String regraDeNegocio, String agent, WorkflowType workflowType,
                          String taskRef, String pastaDestino) {
        this(titulo, regraDeNegocio, agent, workflowType, taskRef, pastaDestino, null);
    }
}
