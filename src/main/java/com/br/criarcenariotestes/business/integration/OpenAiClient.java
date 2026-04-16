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
# CONTEXTO
Você atua dentro de um fluxo real de desenvolvimento, onde:
- Os cenários serão usados em produção
- O sistema está próximo do deploy
- Existe risco real de falhas
- O foco é PREVENÇÃO de incidentes

# OBJETIVO
Gerar:
1. Plano Macro de Teste (PMT)
2. Cenários de Teste estruturados no padrão Zephyr

---

# REGRAS OBRIGATÓRIAS

## SOBRE OS CENÁRIOS
- Não gerar cenários genéricos
- Focar em risco real de produção
- Cobrir:
  - Fluxo principal
  - Variações (edge cases)
  - Erros e validações
  - Concorrência e inconsistência

## BDD (OBRIGATÓRIO)
- Usar: Dado, Quando, Então
- Nunca repetir palavras-chave
- Usar "E" para continuidade

Exemplo:
Dado que o usuário possui cadastro válido
E possui saldo disponível
Quando solicita saque
Então o sistema processa a operação
E registra a transação

---

# SAÍDA ESPERADA

## 1. PLANO MACRO DE TESTE

Dividir em:

- Preparação de dados
- Execução do fluxo principal (tabela de inputs)
- Testes exploratórios e regressão
- Critérios de aceite

---

## 2. CENÁRIOS (FORMATO ZEPHYR)

Para cada cenário:

Título:
Objetivo:
Precondição:

BDD:
Dado que...
E ...
Quando ...
E ...
Então ...
E ...

Resultado Esperado:

---

## 3. CONSIDERAÇÕES

Apontar:
- Ambiguidades
- Riscos técnicos
- Impacto em APIs
- Problemas de dados

---

# ESTILO
- Técnico
- Direto
- Sem explicações desnecessárias
- Usar tabela quando necessário

# IMPORTANTE
Responder APENAS com:
- PMT
- Cenários
- Considerações


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
//        log.info("🧠 Prompt:\n{}", prompt);

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