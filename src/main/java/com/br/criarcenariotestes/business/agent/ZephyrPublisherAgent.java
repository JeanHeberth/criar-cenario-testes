package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.business.tracker.FolderStrategyResolver;
import com.br.criarcenariotestes.business.tracker.ProvedorTarefa;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefa;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefaParser;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import com.br.criarcenariotestes.infrastructure.zephyr.PastaInexistenteException;
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
    private final ReferenciaTarefaParser referenciaTarefaParser;
    private final FolderStrategyResolver folderStrategyResolver;

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

        ReferenciaTarefa referencia = resolverReferenciaTarefa(context);
        String projectKey = resolverProjectKey(context, referencia);
        String pastaRaiz = resolverPastaRaiz(context, referencia);
        String jiraIssueId = resolverJiraIssueId(referencia);
        String testCycleKey = resolverTestCycleKey(context, projectKey);

        for (CenarioItem item : cenarios) {
            try {
                Long folderId = resolverFolderId(item, context, pastasResolvidas, projectKey, pastaRaiz);

                String testCaseKey = buscarCasoJaExistente(item, folderId, casosExistentesPorPasta, projectKey);
                if (testCaseKey != null) {
                    log.info("Cenário '{}' já existe no Zephyr como {} - reaproveitando em vez de duplicar.",
                            item.getNome(), testCaseKey);
                    reaproveitados++;
                } else {
                    testCaseKey = zephyrClient.criarCasoDeTeste(item, folderId, projectKey);
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
                            log.warn("Caso de teste {} criado, mas falhou ao vincular à tarefa '{}': {}",
                                    testCaseKey, referencia == null ? null : referencia.identificador(), e.getMessage());
                        }
                    }

                    if (testCycleKey != null) {
                        try {
                            zephyrClient.adicionarExecucaoAoCiclo(testCaseKey, testCycleKey, projectKey);
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
                                          Map<Long, Map<String, String>> casosExistentesPorPasta,
                                          String projectKey) {
        if (folderId == null || !temTexto(item.getNome())) {
            return null;
        }

        try {
            // Cópia defensiva: o cache é mutado por registrarCasoCriado ao
            // longo do lote, e o client não garante devolver mapa mutável.
            Map<String, String> existentes = casosExistentesPorPasta
                    .computeIfAbsent(folderId, id -> new HashMap<>(zephyrClient.listarCasosDeTestePorPasta(id, projectKey)));
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
    private Long resolverFolderId(CenarioItem item, WorkflowContext context,
                                  Map<String, Long> pastasResolvidas, String projectKey,
                                  String pastaRaizDerivada) {
        String folha = temTexto(item.getPasta()) ? item.getPasta() : context.getRequest().titulo();
        if (!temTexto(folha)) {
            return null;
        }

        String raiz = temTexto(pastaRaizDerivada) ? pastaRaizDerivada : zephyrProperties.getRootFolder();

        String caminho = temTexto(raiz) ? raiz.trim() + "/" + folha.trim() : folha.trim();

        if (pastasResolvidas.containsKey(caminho)) {
            return pastasResolvidas.get(caminho);
        }

        try {
            Long folderId = zephyrClient.resolverOuCriarFolder(caminho, projectKey);
            pastasResolvidas.put(caminho, folderId);
            return folderId;
        } catch (PastaInexistenteException e) {
            // Deliberadamente NÃO cai para "sem pasta" como as demais falhas:
            // quem desliga a criação automática quer justamente evitar caso de
            // teste solto no board. Falhar este item (os outros seguem) é o
            // resultado alinhado à configuração.
            throw e;
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
    private String resolverJiraIssueId(ReferenciaTarefa referencia) {
        if (referencia == null) {
            return null;
        }

        // O vínculo é feito pela API do Zephyr Scale, que é addon do Jira e só
        // aceita issue do Jira. Um work item do Azure precisa de outro caminho
        // (Azure Test Plans), ainda não implementado — publicar sem vínculo e
        // dizer isso é melhor que falhar a geração inteira ou silenciar.
        if (referencia.provedor() != ProvedorTarefa.JIRA) {
            log.warn("Referência '{}' é do {} - vínculo de tarefa ainda não suportado fora do Jira. "
                            + "Os casos de teste serão publicados sem vínculo.",
                    referencia.identificador(), referencia.provedor());
            return null;
        }

        try {
            return jiraClient.buscarIssueId(referencia.identificador());
        } catch (Exception e) {
            log.warn("Falha ao resolver id da issue Jira '{}' - publicando sem vínculo. erro={}",
                    referencia.identificador(), e.getMessage());
            return null;
        }
    }

    /**
     * Resolve a referência da tarefa informada no pedido. Falha ao interpretar
     * nunca derruba a publicação: a geração via IA já terminou com sucesso, e
     * uma referência malformada só custa o vínculo — não os casos de teste.
     */
    private ReferenciaTarefa resolverReferenciaTarefa(WorkflowContext context) {
        try {
            return referenciaTarefaParser.parsear(context.getRequest().taskRef()).orElse(null);
        } catch (Exception e) {
            log.warn("Referência de tarefa inválida - publicando sem vínculo. erro={}", e.getMessage());
            return null;
        }
    }

    /**
     * Projeto de destino no Zephyr, em ordem de precedência: o informado
     * explicitamente no pedido, o derivado da referência da tarefa, e por
     * último o configurado no ambiente (zephyr.project-key).
     *
     * A derivação vale só para Jira, onde a chave carrega o projeto
     * ("SCRUM-28" -> "SCRUM") e o Zephyr Scale compartilha o projeto do Jira.
     * É heurística, e por isso o campo explícito do pedido tem precedência:
     * times que usam um projeto Jira guarda-chuva com o Zephyr em outro lugar
     * precisam poder sobrescrever.
     */
    /**
     * Pasta raiz (stack de automação), em ordem de precedência: a informada
     * explicitamente no pedido, a derivada da tarefa pela estratégia
     * configurada, e por último zephyr.root-folder.
     *
     * O explícito vem primeiro pelo mesmo motivo do projectKey: a derivação é
     * uma regra do time, e quem faz um pedido pontual fora do padrão precisa
     * poder dizer para onde vai sem editar configuração.
     */
    private String resolverPastaRaiz(WorkflowContext context, ReferenciaTarefa referencia) {
        String doPedido = context.getRequest().pastaDestino();
        if (temTexto(doPedido)) {
            return doPedido.trim();
        }

        return folderStrategyResolver.resolverPastaRaiz(referencia);
    }

    private String resolverProjectKey(WorkflowContext context, ReferenciaTarefa referencia) {
        String doPedido = context.getRequest().projectKey();
        if (temTexto(doPedido)) {
            return doPedido.trim();
        }

        if (referencia != null && referencia.provedor() == ProvedorTarefa.JIRA) {
            return referencia.projeto();
        }

        return null;
    }

    /**
     * Resolvido no máximo uma vez por chamada (não por item), pelo mesmo
     * nome usado na pasta padrão (título do pedido) - agrupa tudo que uma
     * mesma geração produziu num único ciclo, em vez de cada caso ficar
     * solto. Complementa, não substitui, o link direto caso→issue: os dois
     * convivem de propósito (ver adicionarExecucaoAoCiclo no ZephyrClient).
     * Falha ao resolver/criar o ciclo nunca bloqueia a publicação.
     */
    private String resolverTestCycleKey(WorkflowContext context, String projectKey) {
        String nomeCiclo = context.getRequest().titulo();
        if (!temTexto(nomeCiclo)) {
            return null;
        }

        try {
            return zephyrClient.resolverOuCriarTestCycle(nomeCiclo, projectKey);
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
