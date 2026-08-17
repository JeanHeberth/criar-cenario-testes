package com.br.criarcenariotestes.business.tracker;

/**
 * Rastreador de tarefas de onde veio a referência informada no pedido.
 * Detectado a partir da URL, nunca configurado — é o que permite times
 * diferentes usarem ferramentas diferentes na mesma instância da API.
 */
public enum ProvedorTarefa {
    JIRA,
    AZURE_DEVOPS
}
