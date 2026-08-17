package com.br.criarcenariotestes.infrastructure.jira;

import com.br.criarcenariotestes.business.properties.JiraProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@RequiredArgsConstructor
public class JiraClient {

    private final JiraProperties jiraProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode buscarIssueComAnexos(String taskKey) {
        validarConfiguracao();

        String url = montarBaseIssueUrl() + "/" + taskKey + "?fields=attachment";

        HttpHeaders headers = criarHeadersJson();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(NOT_FOUND, "Task Jira nao encontrada: " + taskKey, ex);
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Falha ao consultar Jira", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Erro inesperado ao consultar Jira", ex);
        }
    }

    /**
     * Resolve só o id numérico interno da issue (ex.: 10001) a partir da
     * key (ex.: "SCRUM-29") - é o id que a API do Zephyr Scale exige pra
     * vincular um caso de teste a uma issue, key não serve pra isso.
     */
    public String buscarIssueId(String taskKey) {
        validarConfiguracao();

        String url = montarBaseIssueUrl() + "/" + taskKey + "?fields=key";

        HttpHeaders headers = criarHeadersJson();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode issue = objectMapper.readTree(response.getBody());
            String id = issue.path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new ResponseStatusException(BAD_GATEWAY, "Jira nao retornou 'id' para a task " + taskKey);
            }
            return id;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(NOT_FOUND, "Task Jira nao encontrada: " + taskKey, ex);
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Falha ao consultar Jira", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Erro inesperado ao consultar Jira", ex);
        }
    }

    /**
     * Lê os campos da issue usados para decidir a pasta de destino no
     * repositório de testes (ver FolderStrategyResolver). Uma chamada por
     * geração, não por cenário.
     */
    public DadosDaIssue buscarDadosDaIssue(String taskKey) {
        validarConfiguracao();

        String url = montarBaseIssueUrl() + "/" + taskKey + "?fields=summary,components,labels";

        HttpHeaders headers = criarHeadersJson();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            JsonNode issue = objectMapper.readTree(response.getBody());
            JsonNode fields = issue.path("fields");

            return new DadosDaIssue(
                    issue.path("key").asText(null),
                    issue.path("id").asText(null),
                    fields.path("summary").asText(null),
                    extrairNomes(fields.path("components"), "name"),
                    extrairNomes(fields.path("labels"), null)
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(NOT_FOUND, "Task Jira nao encontrada: " + taskKey, ex);
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Falha ao consultar Jira", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Erro inesperado ao consultar Jira", ex);
        }
    }

    /** Components vêm como objetos com "name"; labels vêm como strings puras. */
    private List<String> extrairNomes(JsonNode array, String campo) {
        if (array == null || !array.isArray()) {
            return List.of();
        }

        List<String> nomes = new java.util.ArrayList<>();
        for (JsonNode no : array) {
            String valor = campo == null ? no.asText(null) : no.path(campo).asText(null);
            if (valor != null && !valor.isBlank()) {
                nomes.add(valor);
            }
        }
        return nomes;
    }

    public byte[] baixarAnexo(String attachmentContentUrl) {
        validarConfiguracao();

        HttpHeaders headers = criarHeadersBinario();

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    attachmentContentUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );

            return response.getBody() == null ? new byte[0] : response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(NOT_FOUND, "Anexo Jira nao encontrado", ex);
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Falha ao baixar anexo no Jira", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Erro inesperado ao baixar anexo", ex);
        }
    }

    private HttpHeaders criarHeadersJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + gerarBasicAuth());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders criarHeadersBinario() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + gerarBasicAuth());
        headers.set(HttpHeaders.ACCEPT, "*/*");
        // Necessario para download de anexo no Jira Cloud
        headers.set("X-Atlassian-Token", "no-check");
        return headers;
    }

    private String montarBaseIssueUrl() {
        String baseUrl = jiraProperties.getBaseUrl().endsWith("/")
                ? jiraProperties.getBaseUrl().substring(0, jiraProperties.getBaseUrl().length() - 1)
                : jiraProperties.getBaseUrl();

        return baseUrl + jiraProperties.getIssueEndpoint();
    }

    private String gerarBasicAuth() {
        String valor = jiraProperties.getEmail() + ":" + jiraProperties.getApiToken();
        return Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    private void validarConfiguracao() {
        if (jiraProperties.getBaseUrl() == null || jiraProperties.getBaseUrl().isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "JIRA_BASE_URL nao configurada");
        }

        if (jiraProperties.getEmail() == null || jiraProperties.getEmail().isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "JIRA_EMAIL nao configurado");
        }

        if (jiraProperties.getApiToken() == null || jiraProperties.getApiToken().isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "JIRA_API_TOKEN nao configurado");
        }
    }
}
