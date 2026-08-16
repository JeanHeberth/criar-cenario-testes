package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import com.br.criarcenariotestes.infrastructure.zephyr.ZephyrClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publica cada cenário gerado como um caso de teste real no Zephyr Scale,
 * gravando a key retornada (ex.: "SCRUM-T123") em
 * CenarioItem#zephyrTestCaseKey.
 *
 * Desabilitado por padrão (zephyr.enabled=false via isEnabled()) - só roda
 * quando o time configurar ZEPHYR_API_TOKEN/ZEPHYR_PROJECT_KEY. Falha de
 * publicação em um cenário específico nunca derruba os demais nem o
 * workflow como um todo: a geração via IA já terminou com sucesso antes
 * deste agente rodar, e uma instabilidade do Zephyr não pode mascarar isso
 * como falha da geração.
 */
@Component
@RequiredArgsConstructor
public class ZephyrPublisherAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ZephyrPublisherAgent.class);

    private final ZephyrClient zephyrClient;
    private final ZephyrProperties zephyrProperties;
    private final JiraClient jiraClient;

    @Override
    public boolean isEnabled(WorkflowContext context) {
        return zephyrProperties.isEnabled();
    }

    @Override
    public void executar(WorkflowContext context) {
        List<CenarioItem> cenarios = context.getCenariosFinais();

        if (cenarios == null || cenarios.isEmpty()) {
            log.warn("Nenhum cenário para publicar no Zephyr.");
            return;
        }

        int sucesso = 0;
        int falha = 0;
        Map<String, Long> pastasResolvidas = new HashMap<>();
        String jiraIssueId = resolverJiraIssueId(context.getRequest().jiraIssueKey());

        for (CenarioItem item : cenarios) {
            try {
                Long folderId = resolverFolderId(item, context, pastasResolvidas);
                String testCaseKey = zephyrClient.criarCasoDeTeste(item, folderId);
                item.setZephyrTestCaseKey(testCaseKey);

                if (jiraIssueId != null) {
                    try {
                        zephyrClient.linkarIssueJira(testCaseKey, jiraIssueId);
                    } catch (Exception e) {
                        log.warn("Caso de teste {} criado, mas falhou ao vincular à issue Jira '{}': {}",
                                testCaseKey, context.getRequest().jiraIssueKey(), e.getMessage());
                    }
                }

                sucesso++;
            } catch (Exception e) {
                log.error("Falha ao publicar cenário '{}' no Zephyr: {}", item.getNome(), e.getMessage(), e);
                falha++;
            }
        }

        context.addMetadata("zephyr_publicados", sucesso);
        context.addMetadata("zephyr_falhas", falha);

        log.info("Publicação no Zephyr concluída. sucesso={}, falha={}, total={}", sucesso, falha, cenarios.size());
    }

    /**
     * Usa CenarioItem#pasta quando a IA especificou uma; senão cai no título
     * do pedido original (ex.: "Login com credenciais válidas") como nome de
     * pasta padrão — assim os casos de um mesmo POST /cenario sempre caem
     * juntos em alguma pasta, nunca soltos na raiz. Resolvido no máximo uma
     * vez por nome de pasta por chamada (cache local), já que uma mesma
     * geração tipicamente reaproveita o mesmo nome em todos os itens.
     * Falha ao resolver/criar a pasta nunca bloqueia a publicação do
     * item — cai para "sem pasta" e segue.
     */
    private Long resolverFolderId(CenarioItem item, WorkflowContext context, Map<String, Long> pastasResolvidas) {
        String nomePasta = temTexto(item.getPasta()) ? item.getPasta() : context.getRequest().titulo();
        if (!temTexto(nomePasta)) {
            return null;
        }

        if (pastasResolvidas.containsKey(nomePasta)) {
            return pastasResolvidas.get(nomePasta);
        }

        try {
            Long folderId = zephyrClient.resolverOuCriarFolder(nomePasta);
            pastasResolvidas.put(nomePasta, folderId);
            return folderId;
        } catch (Exception e) {
            log.warn("Falha ao resolver/criar pasta '{}' no Zephyr - publicando '{}' sem pasta. erro={}",
                    nomePasta, item.getNome(), e.getMessage());
            pastasResolvidas.put(nomePasta, null);
            return null;
        }
    }

    /**
     * Resolvido no máximo uma vez por chamada (não por item) - todos os
     * itens de uma mesma geração compartilham a mesma issue Jira opcional.
     * Falha ao resolver nunca bloqueia a publicação: os casos de teste são
     * criados normalmente, só sem vínculo com o Jira.
     */
    private String resolverJiraIssueId(String jiraIssueKey) {
        if (!temTexto(jiraIssueKey)) {
            return null;
        }

        try {
            return jiraClient.buscarIssueId(jiraIssueKey);
        } catch (Exception e) {
            log.warn("Falha ao resolver id da issue Jira '{}' - publicando sem vínculo. erro={}",
                    jiraIssueKey, e.getMessage());
            return null;
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Override
    public String getNome() {
        return "Zephyr Publisher";
    }
}
