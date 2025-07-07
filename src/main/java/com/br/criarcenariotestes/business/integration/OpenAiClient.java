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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final OpenAiConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Novo método: gera vários cenários.
     */
    public String gerarCenariosIA(String titulo, String regra) {
        String prompt = String.format("""
                Você é um analista de testes sênior. Com base no título e na regra de negócio abaixo, gere:
                
                1. Critérios de aceitação claros e objetivos, numerados (ex: CA1, CA2...)
                2. cenários possíveis de teste cobrindo fluxo principal, alternativo e exceções.
                
                Escreva em português, de forma clara, concisa e profissional.
                Estruture com formato markdown e apresente o conteúdo conforme o modelo abaixo:
                
                
                
                Título: Cadastrar Usuário.
                Regra de Negócio: O sistema deve permitir o cadastro de novos usuários com e-mail único.
                
                
                Cenários de Teste:
                
                Cenário 1 -

                ---

                Cenário 2 -

                ---
                
                Título: %s  
                Regra de Negócio: %s
                """, titulo, regra);

        String respostaCompleta = enviarPrompt(prompt);

        // Divide os blocos por duas quebras de linha
        String[] blocos = respostaCompleta.split("\\n\\n+");
        List<String> cenarios = new ArrayList<>();

        for (String bloco : blocos) {
            String textoLimpo = bloco.trim();
            if (!textoLimpo.isBlank()) {
                cenarios.add(textoLimpo);
            }
        }

        return respostaCompleta;
    }

    /**
     * Método interno para chamada à OpenAI.
     */
    private String enviarPrompt(String prompt) {
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
