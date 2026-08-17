package com.br.criarcenariotestes.business.tracker;

/**
 * Endereço completo de uma tarefa no rastreador do time, resolvido a partir
 * do que o usuário informou (tipicamente a URL colada do navegador).
 *
 * O motivo de existir como objeto, e não como uma String de "chave": no Jira
 * a chave carrega o projeto embutido ("SCRUM-28" -> projeto "SCRUM"), mas no
 * Azure DevOps o work item é um id numérico global ("1234") que não diz nada
 * sobre organização nem projeto. Qualquer código que tente deduzir o destino
 * a partir do formato da chave funciona só no Jira e quebra no Azure — por
 * isso o parser resolve tudo uma vez, aqui, e o resto do sistema lê campos
 * nomeados em vez de fatiar strings.
 */
public record ReferenciaTarefa(
        ProvedorTarefa provedor,

        /** Só Azure DevOps; null no Jira, que não tem esse nível. */
        String organizacao,

        /** Jira: a chave do projeto ("SCRUM"). Azure: o nome do projeto. */
        String projeto,

        /** Jira: a chave da issue ("SCRUM-28"). Azure: o id do work item ("1234"). */
        String identificador,

        /** O que o usuário informou, preservado para log e rastreabilidade. */
        String entradaOriginal
) {
}
