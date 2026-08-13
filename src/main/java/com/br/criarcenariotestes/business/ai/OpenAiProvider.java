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
        return gerarResposta(systemPrompt, userPrompt, null);
    }

    @Override
    public String gerarResposta(String systemPrompt, String userPrompt, Integer maxTokensOverride) {
        return gerarRespostaComHistorico(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt)), maxTokensOverride);
    }

    @Override
    public String gerarRespostaComHistorico(String systemPrompt, List<Map<String, String>> history) {
        return gerarRespostaComHistorico(systemPrompt, history, null);
    }

    private String gerarRespostaComHistorico(String systemPrompt, List<Map<String, String>> history, Integer maxTokensOverride) {
        validarConfiguracao();

        int maxTokens = maxTokensOverride != null ? maxTokensOverride : properties.getMaxTokens();

        log.info("OpenAI request. model='{}', url='{}', maxTokens={}, systemPromptLength={}, historySize={}",
                properties.getModel(),
                properties.getUrl(),
                maxTokens,
                systemPrompt == null ? 0 : systemPrompt.length(),
                history == null ? 0 : history.size());

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
        requestBody.put("max_tokens", maxTokens);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getUrl(), entity, String.class);
            log.info("✅ OpenAI status: {}", response.getStatusCode());

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode primeiraEscolha = root.path("choices").get(0);
            String result = primeiraEscolha
                    .path("message")
                    .path("content")
                    .asText();
            // FASE15-BUG-006: finish_reason == "length" indica que a resposta foi
            // cortada pelo limite de tokens de saída, não que o modelo concluiu
            // naturalmente. Apenas observabilidade - não substitui a validação
            // estrutural existente (GeneratedScenariosValidator continua obrigatório).
            String finishReason = primeiraEscolha.path("finish_reason").asText(null);

            log.info("OpenAI response recebida. model='{}', responseLength={}, finishReason='{}', preview='{}'",
                    properties.getModel(),
                    result == null ? 0 : result.length(),
                    finishReason,
                    gerarPreview(result));

            if ("length".equals(finishReason)) {
                log.warn("OpenAI truncou a resposta pelo limite de tokens de saída (finish_reason='length'). " +
                        "maxTokens={}, responseLength={}", maxTokens, result == null ? 0 : result.length());
            }

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

    private String gerarPreview(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        String normalizado = valor.replaceAll("\\s+", " ").trim();
        return normalizado.length() <= 250 ? normalizado : normalizado.substring(0, 250) + "...";
    }
}