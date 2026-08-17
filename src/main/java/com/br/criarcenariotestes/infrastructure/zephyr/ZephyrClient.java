package com.br.criarcenariotestes.infrastructure.zephyr;

import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente REST para o Zephyr Scale Cloud (v2) — publica um CenarioItem como
 * caso de teste real. A formatação de texto (Markdown) continua sendo feita
 * separadamente por ZephyrFormatterAgent; este cliente só cuida da chamada
 * de rede.
 *
 * Escopo v1: envia apenas campos que não exigem resolução nome->ID, com uma
 * exceção — pasta (folderId), resolvida/criada sob demanda via
 * {@link #resolverOuCriarFolder(String)} porque é o único desses campos que
 * o usuário efetivamente precisa pra organizar o board (componentId,
 * ownerId continuam de fora: dependeriam de lookups contra nomes livres
 * gerados pela IA, que raramente batem com os cadastrados no projeto).
 * "labels" não precisa de lookup: o Zephyr cria labels novas
 * automaticamente quando ainda não existem.
 */
@Component
@RequiredArgsConstructor
public class ZephyrClient {

    private static final Logger log = LoggerFactory.getLogger(ZephyrClient.class);

    private final ZephyrProperties zephyrProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Projeto de destino da operação: o informado no pedido quando houver,
     * senão o configurado no ambiente.
     *
     * Existe porque zephyr.project-key é global por ambiente, o que limita a
     * instância a atender um único time. Cada método público aceita um
     * projectKey opcional para que times diferentes possam publicar em
     * projetos diferentes sem subir uma instância por time.
     */
    private String projectKeyEfetivo(String projectKeyDoPedido) {
        return isBlank(projectKeyDoPedido) ? zephyrProperties.getProjectKey() : projectKeyDoPedido.trim();
    }

    public String criarCasoDeTeste(CenarioItem item, Long folderId) {
        return criarCasoDeTeste(item, folderId, null);
    }

    public String criarCasoDeTeste(CenarioItem item, Long folderId, String projectKey) {
        validarConfiguracao();

        HttpHeaders headers = criarHeaders();
        Map<String, Object> corpoCriacao = montarCorpoCriacao(item, folderId, projectKeyEfetivo(projectKey));

        String testCaseKey;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl() + "/testcases",
                    new HttpEntity<>(corpoCriacao, headers),
                    Map.class
            );

            Object key = response.getBody() == null ? null : response.getBody().get("key");
            if (key == null) {
                throw new IllegalStateException("Zephyr não retornou 'key' ao criar o caso de teste");
            }

            testCaseKey = key.toString();
            log.info("Caso de teste criado no Zephyr. key='{}', nome='{}'", testCaseKey, item.getNome());
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao criar caso de teste no Zephyr (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
                    ex
            );
        }

        adicionarPassos(testCaseKey, item, headers);

        return testCaseKey;
    }

    /**
     * Vincula um caso de teste já criado a uma issue do Jira (aba
     * "Traceability" no Zephyr). Exige o id NUMÉRICO interno da issue
     * (resolvido via JiraClient#buscarIssueId), não a key - a API do Zephyr
     * não aceita key aqui.
     */
    public void linkarIssueJira(String testCaseKey, String jiraIssueId) {
        validarConfiguracao();

        HttpHeaders headers = criarHeaders();
        Map<String, Object> body = Map.of("issueId", Long.valueOf(jiraIssueId));

        try {
            restTemplate.postForEntity(
                    baseUrl() + "/testcases/" + testCaseKey + "/links/issues",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("Caso de teste {} vinculado à issue Jira id={}", testCaseKey, jiraIssueId);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao vincular caso de teste " + testCaseKey + " à issue Jira id=" + jiraIssueId
                            + " (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private void adicionarPassos(String testCaseKey, CenarioItem item, HttpHeaders headers) {
        String descricao = item.getScriptTeste();
        String resultadoEsperado = item.getResultadoEsperado();

        if (isBlank(descricao) && isBlank(resultadoEsperado)) {
            return;
        }

        Map<String, Object> inline = new HashMap<>();
        if (!isBlank(descricao)) {
            inline.put("description", descricao);
        }
        if (!isBlank(resultadoEsperado)) {
            inline.put("expectedResult", resultadoEsperado);
        }

        Map<String, Object> passo = Map.of("inline", inline);
        // OVERWRITE, não APPEND: o Zephyr cria o caso de teste já com um passo
        // padrão vazio. APPEND deixava esse passo vazio + o nosso lado a lado
        // (Steps (2), sendo o primeiro "Nenhum/Nenhum/Nenhum"). OVERWRITE
        // substitui pelo nosso único passo real.
        Map<String, Object> corpo = Map.of("mode", "OVERWRITE", "items", List.of(passo));

        try {
            restTemplate.postForEntity(
                    baseUrl() + "/testcases/" + testCaseKey + "/teststeps",
                    new HttpEntity<>(corpo, headers),
                    Void.class
            );
        } catch (Exception ex) {
            // O caso de teste já existe no Zephyr com a key retornada acima -
            // não vale a pena derrubar a publicação inteira só porque os
            // passos (complemento) falharam ao ser anexados.
            log.warn("Caso de teste {} criado, mas falhou ao anexar passos: {}", testCaseKey, ex.getMessage());
        }
    }

    private Map<String, Object> montarCorpoCriacao(CenarioItem item, Long folderId, String projectKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", projectKey);
        body.put("name", sanitizarNome(item.getNome()));
        body.put("statusName", zephyrProperties.getDefaultStatusName());
        body.put("priorityName", zephyrProperties.getDefaultPriorityName());

        if (!isBlank(item.getObjetivo())) {
            body.put("objective", item.getObjetivo());
        }
        if (!isBlank(item.getPrecondicao())) {
            body.put("precondition", item.getPrecondicao());
        }
        if (folderId != null) {
            body.put("folderId", folderId);
        }

        List<String> labels = extrairLabels(item.getRotulos());
        if (!labels.isEmpty()) {
            body.put("labels", labels);
        }

        return body;
    }

    /**
     * Resolve o id de uma pasta de casos de teste a partir de um CAMINHO
     * hierárquico ("Java/Login"), criando cada nível que ainda não existir.
     * Um nome simples ("Login") continua funcionando como caminho de um
     * nível só. A busca por nível considera o parentId, então "Java/Login" e
     * "Robot/Login" resolvem para pastas distintas mesmo tendo folhas
     * homônimas. Cacheável pelo chamador (uma vez por caminho, não por item).
     */
    public Long resolverOuCriarFolder(String caminhoPasta) {
        return resolverOuCriarFolder(caminhoPasta, null);
    }

    public Long resolverOuCriarFolder(String caminhoPasta, String projectKey) {
        validarConfiguracao();

        if (isBlank(caminhoPasta)) {
            return null;
        }

        String projeto = projectKeyEfetivo(projectKey);
        List<Map<String, Object>> todasAsPastas = listarTodasAsPastas(projeto);

        Long parentId = null;
        for (String nivel : caminhoPasta.split("/")) {
            String nome = nivel.trim();
            if (nome.isEmpty()) {
                continue;
            }

            Long existente = buscarFolderNoNivel(todasAsPastas, nome, parentId);

            if (existente == null && !zephyrProperties.isAllowFolderCreation()) {
                throw new PastaInexistenteException(
                        "Pasta '" + nome + "' do caminho '" + caminhoPasta + "' não existe no projeto "
                                + projeto + " e a criação automática está desabilitada "
                                + "(zephyr.allow-folder-creation=false). Crie a pasta no Zephyr ou ajuste o "
                                + "caminho no pedido.");
            }

            parentId = existente != null ? existente : criarFolder(nome, parentId, projeto);
        }

        return parentId;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listarTodasAsPastas(String projectKey) {
        HttpHeaders headers = criarHeaders();
        String url = baseUrl() + "/folders?projectKey=" + projectKey
                + "&folderType=TEST_CASE&maxResults=100";

        List<Map<String, Object>> todas = new ArrayList<>();

        while (url != null) {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }

            List<Map<String, Object>> valores = (List<Map<String, Object>>) body.get("values");
            if (valores != null) {
                todas.addAll(valores);
            }

            Object proxima = body.get("next");
            url = proxima == null ? null : proxima.toString();
        }

        return todas;
    }

    /**
     * Compara nome E parentId: sem o parentId, "Java/Login" acharia uma
     * "Login" solta na raiz (ou dentro de "Robot") e aninharia errado.
     */
    private Long buscarFolderNoNivel(List<Map<String, Object>> todasAsPastas, String nome, Long parentId) {
        for (Map<String, Object> pasta : todasAsPastas) {
            if (!nome.equalsIgnoreCase(String.valueOf(pasta.get("name")))) {
                continue;
            }

            Object parentBruto = pasta.get("parentId");
            Long parentDaPasta = parentBruto == null ? null : Long.valueOf(parentBruto.toString());

            if (java.util.Objects.equals(parentDaPasta, parentId)) {
                Object id = pasta.get("id");
                return id == null ? null : Long.valueOf(id.toString());
            }
        }
        return null;
    }

    private Long criarFolder(String nomePasta, Long parentId, String projectKey) {
        HttpHeaders headers = criarHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", projectKey);
        body.put("name", nomePasta);
        body.put("folderType", "TEST_CASE");
        if (parentId != null) {
            body.put("parentId", parentId);
        }

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl() + "/folders",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Object id = response.getBody() == null ? null : response.getBody().get("id");
            if (id == null) {
                throw new IllegalStateException("Zephyr não retornou 'id' ao criar a pasta '" + nomePasta + "'");
            }

            Long folderId = Long.valueOf(id.toString());
            log.info("Pasta criada no Zephyr. nome='{}', id={}, parentId={}", nomePasta, folderId, parentId);
            return folderId;
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao criar pasta '" + nomePasta + "' no Zephyr (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    /**
     * Mapa nome-normalizado -> key dos casos de teste já existentes numa
     * pasta, usado para não recriar um caso que já está lá (cada POST
     * /cenario gera nomes muito parecidos; sem isso o board acumula
     * duplicatas como "Login com e-mail não cadastrado" repetido). Uma
     * chamada por pasta, não por item — o chamador cacheia por folderId.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> listarCasosDeTestePorPasta(Long folderId) {
        return listarCasosDeTestePorPasta(folderId, null);
    }

    public Map<String, String> listarCasosDeTestePorPasta(Long folderId, String projectKey) {
        validarConfiguracao();

        if (folderId == null) {
            return Map.of();
        }

        HttpHeaders headers = criarHeaders();
        String url = baseUrl() + "/testcases?projectKey=" + projectKeyEfetivo(projectKey)
                + "&folderId=" + folderId + "&maxResults=100";

        Map<String, String> porNome = new HashMap<>();

        while (url != null) {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }

            List<Map<String, Object>> valores = (List<Map<String, Object>>) body.get("values");
            if (valores != null) {
                for (Map<String, Object> caso : valores) {
                    Object nome = caso.get("name");
                    Object key = caso.get("key");
                    if (nome != null && key != null) {
                        porNome.putIfAbsent(normalizarParaComparacao(nome.toString()), key.toString());
                    }
                }
            }

            Object proxima = body.get("next");
            url = proxima == null ? null : proxima.toString();
        }

        return porNome;
    }

    /**
     * Comparação de nome tolerante a diferenças cosméticas de formatação da
     * IA (caixa, espaçamento múltiplo) — sem isso, "Login com  e-mail" e
     * "login com e-mail" seriam tratados como cenários diferentes.
     *
     * static de propósito: é função pura e precisa ser a MESMA regra usada
     * ao montar o mapa aqui e ao consultá-lo no ZephyrPublisherAgent. Como
     * método de instância, um mock não configurado retornaria null para
     * todo nome, fazendo cenários distintos colidirem na mesma chave e
     * serem tratados como duplicatas.
     */
    public static String normalizarParaComparacao(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        return sanitizarNome(nome).toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /**
     * Resolve a key de um ciclo de teste pelo nome, criando-o no Zephyr se
     * ainda não existir. Cacheável pelo chamador (um ciclo por geração, não
     * por item) — igual pasta, mas resolvido uma única vez por lote em vez
     * de uma vez por nome distinto (ver ZephyrPublisherAgent).
     */
    public String resolverOuCriarTestCycle(String nomeCiclo) {
        return resolverOuCriarTestCycle(nomeCiclo, null);
    }

    public String resolverOuCriarTestCycle(String nomeCiclo, String projectKey) {
        validarConfiguracao();

        if (isBlank(nomeCiclo)) {
            return null;
        }

        String projeto = projectKeyEfetivo(projectKey);

        String existente = buscarTestCycleExistente(nomeCiclo, projeto);
        if (existente != null) {
            return existente;
        }

        return criarTestCycle(nomeCiclo, projeto);
    }

    @SuppressWarnings("unchecked")
    private String buscarTestCycleExistente(String nomeCiclo, String projectKey) {
        HttpHeaders headers = criarHeaders();
        String url = baseUrl() + "/testcycles?projectKey=" + projectKey + "&maxResults=50";

        while (url != null) {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return null;
            }

            List<Map<String, Object>> valores = (List<Map<String, Object>>) body.get("values");
            if (valores != null) {
                for (Map<String, Object> ciclo : valores) {
                    if (nomeCiclo.equalsIgnoreCase(String.valueOf(ciclo.get("name")))) {
                        Object key = ciclo.get("key");
                        return key == null ? null : key.toString();
                    }
                }
            }

            Object proxima = body.get("next");
            url = proxima == null ? null : proxima.toString();
        }

        return null;
    }

    private String criarTestCycle(String nomeCiclo, String projectKey) {
        HttpHeaders headers = criarHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", projectKey);
        body.put("name", nomeCiclo);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl() + "/testcycles",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Object key = response.getBody() == null ? null : response.getBody().get("key");
            if (key == null) {
                throw new IllegalStateException("Zephyr não retornou 'key' ao criar o ciclo de teste '" + nomeCiclo + "'");
            }

            String cycleKey = key.toString();
            log.info("Ciclo de teste criado no Zephyr. nome='{}', key={}", nomeCiclo, cycleKey);
            return cycleKey;
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao criar ciclo de teste '" + nomeCiclo + "' no Zephyr (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    /**
     * Registra o caso de teste como uma execução dentro do ciclo (mantém
     * também o link direto caso→issue feito por linkarIssueJira - as duas
     * associações coexistem de propósito: o ciclo agrupa visualmente tudo
     * que uma mesma geração produziu, o link direto garante que o caso
     * apareça em "Casos de Teste cobertos" na issue mesmo sem abrir o ciclo).
     */
    public void adicionarExecucaoAoCiclo(String testCaseKey, String testCycleKey) {
        adicionarExecucaoAoCiclo(testCaseKey, testCycleKey, null);
    }

    public void adicionarExecucaoAoCiclo(String testCaseKey, String testCycleKey, String projectKey) {
        validarConfiguracao();

        HttpHeaders headers = criarHeaders();
        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", projectKeyEfetivo(projectKey));
        body.put("testCaseKey", testCaseKey);
        body.put("testCycleKey", testCycleKey);
        // Obrigatório: a API rejeita com 400 "statusName: must not be null".
        body.put("statusName", zephyrProperties.getDefaultExecutionStatusName());

        try {
            restTemplate.postForEntity(
                    baseUrl() + "/testexecutions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("Caso de teste {} adicionado ao ciclo {}", testCaseKey, testCycleKey);
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao adicionar caso de teste " + testCaseKey + " ao ciclo " + testCycleKey
                            + " (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private List<String> extrairLabels(String rotulos) {
        if (isBlank(rotulos)) {
            return List.of();
        }

        List<String> labels = new ArrayList<>();
        for (String rotulo : rotulos.split(",")) {
            String trimmed = rotulo.trim();
            if (!trimmed.isEmpty()) {
                labels.add(trimmed);
            }
        }
        return labels;
    }

    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(zephyrProperties.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String baseUrl() {
        String url = zephyrProperties.getBaseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private void validarConfiguracao() {
        if (isBlank(zephyrProperties.getApiToken())) {
            throw new IllegalStateException("ZEPHYR_API_TOKEN não configurado");
        }
        if (isBlank(zephyrProperties.getProjectKey())) {
            throw new IllegalStateException("ZEPHYR_PROJECT_KEY não configurado");
        }
        if (isBlank(zephyrProperties.getBaseUrl())) {
            throw new IllegalStateException("Zephyr base-url não configurada");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * O Zephyr rejeita "name" com quebra de linha (regex "^(?!\\s*$).+" não
     * casa string multi-linha) — visto na prática: 30/30 falhas de criação
     * quando o parser upstream gerou nomes com \n embutido. Nunca confiamos
     * cegamente no que a IA/parser produziu como nome; normalizamos aqui,
     * na borda de saída, independente do que causou o formato malformado.
     */
    private static String sanitizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "Cenário sem nome";
        }

        String semQuebrasDeLinha = nome.replaceAll("\\s*\\R+\\s*", " ").trim();
        if (semQuebrasDeLinha.isBlank()) {
            return "Cenário sem nome";
        }

        return semQuebrasDeLinha.length() > 255 ? semQuebrasDeLinha.substring(0, 255) : semQuebrasDeLinha;
    }
}
