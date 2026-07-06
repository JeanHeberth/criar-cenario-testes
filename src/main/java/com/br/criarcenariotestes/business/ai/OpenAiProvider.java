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

import java.util.ArrayList;
import java.util.HashMap;
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
        return gerarRespostaComHistorico(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt)));
    }

    @Override
    public String gerarRespostaComHistorico(String systemPrompt, List<Map<String, String>> history) {
        validarConfiguracao();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // Histórico completo (OpenAI já aceita role: user/assistant diretamente)
        messages.addAll(history);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        // Evita truncar resposta e perder cenarios na saida.
        requestBody.put("max_tokens", properties.getMaxTokens());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getUrl(), entity, String.class);
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