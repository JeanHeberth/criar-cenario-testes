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
import java.util.HashMap;
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

        int maxOutputTokens = maxTokensOverride != null ? maxTokensOverride : properties.getMaxOutputTokens();

        String urlFinal = properties.getUrl()
                + "/"
                + properties.getModel()
                + ":generateContent?key="
                + properties.getApiKey();

        log.info("Gemini request. model='{}', url='{}', maxOutputTokens={}, systemPromptLength={}, historySize={}",
                properties.getModel(),
                properties.getUrl(),
                maxOutputTokens,
                systemPrompt == null ? 0 : systemPrompt.length(),
                history == null ? 0 : history.size());

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

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        // Evita truncar resposta e perder cenarios na saida.
        generationConfig.put("maxOutputTokens", maxOutputTokens);

        // Modelos 2.5 vêm com "thinking" ligado por padrão, e esses tokens de
        // raciocínio consomem o MESMO orçamento de maxOutputTokens - observado
        // na prática: resposta cortada em finishReason=MAX_TOKENS deixando o
        // cenário sem "Quando"/"Então" e reprovando na validação BDD. Geração
        // de cenário é tarefa de formatação estruturada, não de raciocínio
        // longo, então desligamos para o orçamento inteiro ir para o texto.
        if (properties.isDisableThinking()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(urlFinal, entity, String.class);
            log.info("✅ Gemini status: {}", response.getStatusCode());

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode primeiroCandidato = root.path("candidates").get(0);
            String result = primeiroCandidato
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
            // FASE15-BUG-006: mesmo raciocínio do OpenAiProvider - "MAX_TOKENS"
            // indica corte pelo limite de saída, apenas observabilidade.
            String finishReason = primeiroCandidato.path("finishReason").asText(null);

            log.info("Gemini response recebida. model='{}', responseLength={}, finishReason='{}', preview='{}'",
                    properties.getModel(),
                    result == null ? 0 : result.length(),
                    finishReason,
                    gerarPreview(result));

            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("Gemini truncou a resposta pelo limite de tokens de saída (finishReason='MAX_TOKENS'). " +
                        "maxOutputTokens={}, responseLength={}", maxOutputTokens, result == null ? 0 : result.length());
            }

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

    private String gerarPreview(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        String normalizado = valor.replaceAll("\\s+", " ").trim();
        return normalizado.length() <= 250 ? normalizado : normalizado.substring(0, 250) + "...";
    }
}