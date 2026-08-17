package com.br.criarcenariotestes.business.tracker;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolve o que o usuário informou (URL colada do navegador ou chave pura) em
 * uma {@link ReferenciaTarefa} completa.
 *
 * A URL é a entrada canônica de propósito: é a única representação
 * autossuficiente nos dois rastreadores. Uma chave "SCRUM-28" identifica a
 * issue inteira no Jira, mas o equivalente no Azure ("1234") não diz nem a
 * organização nem o projeto — só a URL carrega isso. Detectar o provedor a
 * partir da URL também evita ter que configurá-lo por ambiente, que é o que
 * impediria dois times com ferramentas diferentes na mesma instância.
 */
@Component
public class ReferenciaTarefaParser {

    /** https://algo.atlassian.net/browse/SCRUM-28 */
    private static final Pattern URL_JIRA_BROWSE = Pattern.compile(
            "^https?://[^/\\s]+/browse/([A-Za-z][A-Za-z0-9_]*)-(\\d+)/?.*$");

    /**
     * Board/backlog com a issue selecionada:
     * https://algo.atlassian.net/jira/software/projects/SCRUM/boards/1?selectedIssue=SCRUM-28
     */
    private static final Pattern URL_JIRA_SELECTED_ISSUE = Pattern.compile(
            "^https?://[^/\\s]+/.*[?&]selectedIssue=([A-Za-z][A-Za-z0-9_]*)-(\\d+).*$");

    /** https://dev.azure.com/minhaOrg/MeuProjeto/_workitems/edit/1234 */
    private static final Pattern URL_AZURE_DEV = Pattern.compile(
            "^https?://dev\\.azure\\.com/([^/\\s]+)/([^/\\s]+)/_workitems/edit/(\\d+)/?.*$");

    /** Formato legado: https://minhaOrg.visualstudio.com/MeuProjeto/_workitems/edit/1234 */
    private static final Pattern URL_AZURE_LEGADO = Pattern.compile(
            "^https?://([^./\\s]+)\\.visualstudio\\.com/([^/\\s]+)/_workitems/edit/(\\d+)/?.*$");

    /** Chave solta do Jira, mantida porque já era o formato aceito antes. */
    private static final Pattern CHAVE_JIRA = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9_]*)-(\\d+)$");

    /** Id solto de work item — reconhecido só para dar erro explicativo. */
    private static final Pattern ID_NUMERICO = Pattern.compile("^\\d+$");

    /**
     * @return vazio quando nada foi informado (campo opcional); nunca vazio
     *         para entrada preenchida — texto irreconhecível vira exceção.
     */
    public Optional<ReferenciaTarefa> parsear(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return Optional.empty();
        }

        String texto = entrada.trim();

        Matcher jiraBrowse = URL_JIRA_BROWSE.matcher(texto);
        if (jiraBrowse.matches()) {
            return Optional.of(montarJira(jiraBrowse.group(1), jiraBrowse.group(2), texto));
        }

        Matcher jiraSelecionada = URL_JIRA_SELECTED_ISSUE.matcher(texto);
        if (jiraSelecionada.matches()) {
            return Optional.of(montarJira(jiraSelecionada.group(1), jiraSelecionada.group(2), texto));
        }

        Matcher azure = URL_AZURE_DEV.matcher(texto);
        if (azure.matches()) {
            return Optional.of(montarAzure(azure.group(1), azure.group(2), azure.group(3), texto));
        }

        Matcher azureLegado = URL_AZURE_LEGADO.matcher(texto);
        if (azureLegado.matches()) {
            return Optional.of(montarAzure(azureLegado.group(1), azureLegado.group(2), azureLegado.group(3), texto));
        }

        Matcher chave = CHAVE_JIRA.matcher(texto);
        if (chave.matches()) {
            return Optional.of(montarJira(chave.group(1), chave.group(2), texto));
        }

        // Um id numérico solto é ambíguo por natureza: não há como saber a
        // organização nem o projeto do Azure a partir dele. Dizer isso é mais
        // útil que a mensagem genérica de formato inválido.
        if (ID_NUMERICO.matcher(texto).matches()) {
            throw new ReferenciaTarefaInvalidaException(
                    "Referência de tarefa '" + texto + "' é apenas um número e não identifica a tarefa por completo. "
                            + "Para Azure DevOps informe a URL completa do work item "
                            + "(https://dev.azure.com/{organizacao}/{projeto}/_workitems/edit/" + texto + ").");
        }

        throw new ReferenciaTarefaInvalidaException(
                "Referência de tarefa '" + texto + "' não reconhecida. Informe a URL da tarefa "
                        + "(ex.: https://suaempresa.atlassian.net/browse/SCRUM-28 ou "
                        + "https://dev.azure.com/org/projeto/_workitems/edit/1234) ou a chave do Jira (ex.: SCRUM-28).");
    }

    private ReferenciaTarefa montarJira(String projeto, String numero, String entradaOriginal) {
        // A chave do Jira é sempre maiúscula; o usuário pode colar minúscula.
        String projetoNormalizado = projeto.toUpperCase();
        return new ReferenciaTarefa(
                ProvedorTarefa.JIRA,
                null,
                projetoNormalizado,
                projetoNormalizado + "-" + numero,
                entradaOriginal
        );
    }

    private ReferenciaTarefa montarAzure(String organizacao, String projeto, String id, String entradaOriginal) {
        return new ReferenciaTarefa(
                ProvedorTarefa.AZURE_DEVOPS,
                organizacao,
                // A URL vem percent-encoded quando o projeto tem espaço.
                java.net.URLDecoder.decode(projeto, java.nio.charset.StandardCharsets.UTF_8),
                id,
                entradaOriginal
        );
    }
}
