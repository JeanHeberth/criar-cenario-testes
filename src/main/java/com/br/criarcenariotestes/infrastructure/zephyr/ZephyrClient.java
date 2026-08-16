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

    public String criarCasoDeTeste(CenarioItem item, Long folderId) {
        validarConfiguracao();

        HttpHeaders headers = criarHeaders();
        Map<String, Object> corpoCriacao = montarCorpoCriacao(item, folderId);

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

    private Map<String, Object> montarCorpoCriacao(CenarioItem item, Long folderId) {
        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", zephyrProperties.getProjectKey());
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
     * Resolve o id de uma pasta de casos de teste pelo nome, criando-a no
     * Zephyr se ainda não existir. Cacheável pelo chamador (uma pasta por
     * nome, não por item) — cada chamada aqui bate na API do Zephyr.
     */
    public Long resolverOuCriarFolder(String nomePasta) {
        validarConfiguracao();

        if (isBlank(nomePasta)) {
            return null;
        }

        Long existente = buscarFolderExistente(nomePasta);
        if (existente != null) {
            return existente;
        }

        return criarFolder(nomePasta);
    }

    @SuppressWarnings("unchecked")
    private Long buscarFolderExistente(String nomePasta) {
        HttpHeaders headers = criarHeaders();
        String url = baseUrl() + "/folders?projectKey=" + zephyrProperties.getProjectKey()
                + "&folderType=TEST_CASE&maxResults=50";

        while (url != null) {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return null;
            }

            List<Map<String, Object>> valores = (List<Map<String, Object>>) body.get("values");
            if (valores != null) {
                for (Map<String, Object> pasta : valores) {
                    if (nomePasta.equalsIgnoreCase(String.valueOf(pasta.get("name")))) {
                        Object id = pasta.get("id");
                        return id == null ? null : Long.valueOf(id.toString());
                    }
                }
            }

            Object proxima = body.get("next");
            url = proxima == null ? null : proxima.toString();
        }

        return null;
    }

    private Long criarFolder(String nomePasta) {
        HttpHeaders headers = criarHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("projectKey", zephyrProperties.getProjectKey());
        body.put("name", nomePasta);
        body.put("folderType", "TEST_CASE");

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
            log.info("Pasta criada no Zephyr. nome='{}', id={}", nomePasta, folderId);
            return folderId;
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(
                    "Falha ao criar pasta '" + nomePasta + "' no Zephyr (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(),
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
    private String sanitizarNome(String nome) {
        if (isBlank(nome)) {
            return "Cenário sem nome";
        }

        String semQuebrasDeLinha = nome.replaceAll("\\s*\\R+\\s*", " ").trim();
        if (semQuebrasDeLinha.isBlank()) {
            return "Cenário sem nome";
        }

        return semQuebrasDeLinha.length() > 255 ? semQuebrasDeLinha.substring(0, 255) : semQuebrasDeLinha;
    }
}
