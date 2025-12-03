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
Você é um Analista de Testes Sênior e especialista na metodologia BDD.

Sua tarefa é gerar **múltiplos cenários de teste funcionais** (fluxo principal, alternativo e exceções) cobrindo a regra de negócio e o título fornecidos.

### Diretrizes BDD e de Qualidade
1.  **Foco Comportamental:** Os cenários devem focar no **comportamento do sistema** do ponto de vista do usuário/negócio.
2.  **Passos Gherkin (DSL):** Os passos 'Dado/Quando/Então' devem ser escritos em um nível de **linguagem de domínio** (negócio), e não em passos de interface de usuário (clicar em botão, digitar).
3.  **Passos Claros:** Certifique-se de que a cláusula 'Quando' descreva a ação que dispara o comportamento, e a cláusula 'Então' descreva o resultado observável esperado.
4.  **Pré-Condição:** A 'Pré-condição' deve descrever o estado inicial do sistema e/ou os dados necessários para a execução do teste.
5.  **Variedade:** Garanta pelo menos 1 cenário de Sucesso (Fluxo Principal), 1 de Alternativa (Variação/Edge Case) e 1 de Exceção (Erro/Validação).

### Formato de Saída
Você deve gerar **múltiplos blocos**, um para cada cenário. Cada bloco deve ser **separado por três hífens (---)**.

Para **cada cenário**, use a estrutura e os campos obrigatórios abaixo (em português):

Nome: [Nome do Cenário (Claro e Descritivo)]
Objetivo: [Descrição concisa do que o teste valida]
Precondição: [O estado inicial do sistema ou dados necessários]
Script de Teste (Passo-a-Passo): [Os passos Gherkin: Dado que... Quando... Então...]
Script de Teste (Passo-a-Passo) - Resultado: [O resultado esperado, começando com "Então..."]
Componente: [O módulo ou funcionalidade principal]
Rótulos: [Palavras-chave separadas por vírgula (ex: Regressão, FluxoPrincipal, Erro)]
Propósito: [Breve explicação de por que este cenário é importante]
Pasta: [O caminho/módulo onde o cenário deve ser armazenado]
Proprietário: [Sugestão de nome do QA responsável]
Cobertura: [Número da História/Requisito coberto (ex: #1234)]
Status: Aguardando execução
---
[INÍCIO DO PRÓXIMO BLOCO DE CENÁRIO]

Título da Feature (Tema): %s
Regra de Negócio (Critérios de Aceite): %s

Responda APENAS com os blocos de cenários formatados.
""", titulo, regra);


        String respostaCompleta = enviarPrompt(prompt);

        // Divide os blocos por separador '---'
        String[] blocos = respostaCompleta.split("---");
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