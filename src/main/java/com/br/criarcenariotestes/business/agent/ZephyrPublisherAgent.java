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
        int reaproveitados = 0;
        Map<String, Long> pastasResolvidas = new HashMap<>();
        Map<Long, Map<String, String>> casosExistentesPorPasta = new HashMap<>();
        java.util.Set<String> keysJaAssociadas = new java.util.HashSet<>();
        String jiraIssueId = resolverJiraIssueId(context.getRequest().jiraIssueKey());
        String testCycleKey = resolverTestCycleKey(context);

        for (CenarioItem item : cenarios) {
            try {
                Long folderId = resolverFolderId(item, context, pastasResolvidas);

                String testCaseKey = buscarCasoJaExistente(item, folderId, casosExistentesPorPasta);
                if (testCaseKey != null) {
                    log.info("Cenário '{}' já existe no Zephyr como {} - reaproveitando em vez de duplicar.",
                            item.getNome(), testCaseKey);
                    reaproveitados++;
                } else {
                    testCaseKey = zephyrClient.criarCasoDeTeste(item, folderId);
                    registrarCasoCriado(item, folderId, testCaseKey, casosExistentesPorPasta);
                }

                item.setZephyrTestCaseKey(testCaseKey);

                // Dois itens do mesmo lote podem apontar para a mesma key
                // (nomes repetidos pela IA). Vincular/adicionar ao ciclo mais
                // de uma vez criaria execuções duplicadas para o mesmo caso.
                if (keysJaAssociadas.add(testCaseKey)) {
                    if (jiraIssueId != null) {
                        try {
                            zephyrClient.linkarIssueJira(testCaseKey, jiraIssueId);
                        } catch (Exception e) {
                            log.warn("Caso de teste {} criado, mas falhou ao vincular à issue Jira '{}': {}",
                                    testCaseKey, context.getRequest().jiraIssueKey(), e.getMessage());
                        }
                    }

                    if (testCycleKey != null) {
                        try {
                            zephyrClient.adicionarExecucaoAoCiclo(testCaseKey, testCycleKey);
                        } catch (Exception e) {
                            log.warn("Caso de teste {} criado, mas falhou ao adicionar ao ciclo '{}': {}",
                                    testCaseKey, testCycleKey, e.getMessage());
                        }
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
        context.addMetadata("zephyr_reaproveitados", reaproveitados);

        log.info("Publicação no Zephyr concluída. sucesso={}, reaproveitados={}, falha={}, total={}",
                sucesso, reaproveitados, falha, cenarios.size());
    }

    /**
     * Evita duplicar no board um caso de teste que já existe na mesma pasta
     * com o mesmo nome (cada POST /cenario sobre o mesmo tema gera nomes
     * quase idênticos). A listagem é feita uma vez por pasta e cacheada.
     * Falha na consulta nunca bloqueia: no pior caso cria o caso de novo,
     * que é o comportamento anterior.
     */
    private String buscarCasoJaExistente(CenarioItem item, Long folderId,
                                          Map<Long, Map<String, String>> casosExistentesPorPasta) {
        if (folderId == null || !temTexto(item.getNome())) {
            return null;
        }

        try {
            // Cópia defensiva: o cache é mutado por registrarCasoCriado ao
            // longo do lote, e o client não garante devolver mapa mutável.
            Map<String, String> existentes = casosExistentesPorPasta
                    .computeIfAbsent(folderId, id -> new HashMap<>(zephyrClient.listarCasosDeTestePorPasta(id)));
            return existentes.get(ZephyrClient.normalizarParaComparacao(item.getNome()));
        } catch (Exception e) {
            log.warn("Falha ao consultar casos existentes na pasta {} - seguindo sem deduplicar. erro={}",
                    folderId, e.getMessage());
            casosExistentesPorPasta.put(folderId, new HashMap<>());
            return null;
        }
    }

    /** Mantém o cache coerente para os próximos itens do mesmo lote. */
    private void registrarCasoCriado(CenarioItem item, Long folderId, String testCaseKey,
                                      Map<Long, Map<String, String>> casosExistentesPorPasta) {
        if (folderId == null || !temTexto(item.getNome())) {
            return;
        }

        casosExistentesPorPasta
                .computeIfAbsent(folderId, k -> new HashMap<>())
                .putIfAbsent(ZephyrClient.normalizarParaComparacao(item.getNome()), testCaseKey);
    }

    /**
     * Monta o caminho hierárquico da pasta: "{pastaRaiz}/{folha}", onde a
     * raiz é a stack de automação ("Java", "Robot") vinda do pedido ou de
     * zephyr.root-folder, e a folha é CenarioItem#pasta (quando a IA
     * especificou) ou o título do pedido. Assim os casos ficam organizados
     * em "Java/Login", "Java/Compras" — em vez de dezenas de pastas soltas
     * na raiz do projeto. Sem raiz configurada, mantém o comportamento
     * anterior (só a folha). Resolvido no máximo uma vez por caminho por
     * chamada (cache local). Falha ao resolver/criar nunca bloqueia a
     * publicação do item — cai para "sem pasta" e segue.
     */
    private Long resolverFolderId(CenarioItem item, WorkflowContext context, Map<String, Long> pastasResolvidas) {
        String folha = temTexto(item.getPasta()) ? item.getPasta() : context.getRequest().titulo();
        if (!temTexto(folha)) {
            return null;
        }

        String raiz = temTexto(context.getRequest().pastaRaiz())
                ? context.getRequest().pastaRaiz()
                : zephyrProperties.getRootFolder();

        String caminho = temTexto(raiz) ? raiz.trim() + "/" + folha.trim() : folha.trim();

        if (pastasResolvidas.containsKey(caminho)) {
            return pastasResolvidas.get(caminho);
        }

        try {
            Long folderId = zephyrClient.resolverOuCriarFolder(caminho);
            pastasResolvidas.put(caminho, folderId);
            return folderId;
        } catch (Exception e) {
            log.warn("Falha ao resolver/criar pasta '{}' no Zephyr - publicando '{}' sem pasta. erro={}",
                    caminho, item.getNome(), e.getMessage());
            pastasResolvidas.put(caminho, null);
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

    /**
     * Resolvido no máximo uma vez por chamada (não por item), pelo mesmo
     * nome usado na pasta padrão (título do pedido) - agrupa tudo que uma
     * mesma geração produziu num único ciclo, em vez de cada caso ficar
     * solto. Complementa, não substitui, o link direto caso→issue: os dois
     * convivem de propósito (ver adicionarExecucaoAoCiclo no ZephyrClient).
     * Falha ao resolver/criar o ciclo nunca bloqueia a publicação.
     */
    private String resolverTestCycleKey(WorkflowContext context) {
        String nomeCiclo = context.getRequest().titulo();
        if (!temTexto(nomeCiclo)) {
            return null;
        }

        try {
            return zephyrClient.resolverOuCriarTestCycle(nomeCiclo);
        } catch (Exception e) {
            log.warn("Falha ao resolver/criar ciclo de teste '{}' no Zephyr - publicando sem ciclo. erro={}",
                    nomeCiclo, e.getMessage());
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
