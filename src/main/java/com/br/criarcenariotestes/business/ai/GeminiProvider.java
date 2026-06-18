package com.br.criarcenariotestes.business.ai;

import com.br.criarcenariotestes.business.properties.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GeminiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final GeminiProperties properties;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public String gerarResposta(String systemPrompt, String userPrompt) {
        return gerarRespostaComHistorico(systemPrompt,
                List.of(Map.of("role", "user", "content", userPrompt)));
    }

    @Override
    public String gerarRespostaComHistorico(String systemPrompt, List<Map<String, String>> history) {
        validarConfiguracao();

        String urlFinal = properties.getUrl()
                + "/"
                + properties.getModel()
                + ":generateContent?key="
                + properties.getApiKey();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Monta system_instruction + contents para multi-turn
        List<Map<String, Object>> contents = new ArrayList<>();

        // System prompt como primeira mensagem de usuário
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
            // Confirmação do modelo para iniciar conversa
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("text", "Entendido. Estou pronto para ajudar."))
            ));
        }

        // Histórico de mensagens (converte "assistant" -> "model" para Gemini)
        for (Map<String, String> msg : history) {
            String role = "assistant".equalsIgnoreCase(msg.get("role")) ? "model" : "user";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.get("content")))
            ));
        }

        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", Map.of("temperature", 0.7)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(urlFinal, entity, String.class);
            log.info("✅ Gemini status: {}", response.getStatusCode());

            JsonNode root = mapper.readTree(response.getBody());
            String result = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            if (result == null || result.isBlank()) {
                throw new RuntimeException("Resposta vazia do Gemini");
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Erro ao chamar Gemini", e);
            throw new RuntimeException("Erro ao comunicar com Gemini", e);
        }
    }

    private void validarConfiguracao() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY não configurada");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new IllegalStateException("Modelo Gemini não configurado");
        }

        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("URL Gemini não configurada");
        }
    }
}