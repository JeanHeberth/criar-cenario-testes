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

        String url = jiraProperties.getBaseUrl()
                + jiraProperties.getIssueEndpoint()
                + "/"
                + taskKey
                + "?fields=attachment";

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
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }

    private HttpHeaders criarHeadersBinario() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + gerarBasicAuth());
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        return headers;
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

