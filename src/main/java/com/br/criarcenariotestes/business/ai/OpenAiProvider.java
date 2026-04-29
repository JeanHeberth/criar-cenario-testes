package com.br.criarcenariotestes.business.ai;

import com.br.criarcenariotestes.business.properties.OpenAiProperties;
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
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final OpenAiProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() {
        return "openai";
    }

    @Override
    public String gerarResposta(String systemPrompt, String userPrompt) {
        validarConfiguracao();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    properties.getUrl(),
                    entity,
                    String.class
            );

            log.info("✅ OpenAI status: {}", response.getStatusCode());

            JsonNode root = mapper.readTree(response.getBody());

            String result = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (result == null || result.isBlank()) {
                throw new RuntimeException("Resposta vazia da OpenAI");
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Erro ao chamar OpenAI", e);
            throw new RuntimeException("Erro ao comunicar com OpenAI", e);
        }
    }

    private void validarConfiguracao() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada");
        }

        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new IllegalStateException("Modelo OpenAI não configurado");
        }

        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("URL OpenAI não configurada");
        }
    }
}