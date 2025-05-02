package com.br.criarcenariotestes.business.integration;

import com.br.criarcenariotestes.business.config.OpenAiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final OpenAiConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String gerarCenarioIA(String titulo, String regra) {
        String prompt = String.format(
                "Crie um cenário de teste BDD no estilo Gherkin. Escreva em português.\n\n" +
                        "Título: %s\n" +
                        "Regra de negócio: %s\n\n " +
                        "Retorne apenas o cenário formatado no estilo:\n" +
                        "Dado que ...\n " +
                        "Quando ...\n " +
                        "Então ...",
                titulo, regra);

        log.info("🔑 API KEY configurada: {}", config.getApiKey() != null && !config.getApiKey().isBlank() ? "OK" : "FALTANDO!");
        log.info("📡 Enviando requisição para: {}", config.getUrl());
        log.info("🧠 Prompt:\n{}", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(config.getUrl(), entity, String.class);

            log.info("✅ Status OpenAI: {}", response.getStatusCode());
            log.debug("📨 Corpo da resposta: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = mapper.readTree(response.getBody());
                String result = root.path("choices").get(0).path("message").path("content").asText();

                return result != null && !result.isBlank() ? result : "Resposta vazia da OpenAI";
            }

            return "Falha ao chamar OpenAI: " + response.getStatusCode();

        } catch (Exception e) {
            log.error("❌ Erro ao chamar OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao comunicar com a IA", e);
        }
    }
}
