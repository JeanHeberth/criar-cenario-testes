package com.br.criarcenariotestes.business.integration;

import com.br.criarcenariotestes.business.config.OpenAiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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
     * O prompt foi otimizado para o Gemini (Analista Sênior BDD).
     */
    public String gerarCenariosIA(String titulo, String regra) {
        String prompt = String.format("""
                Você é um Analista de Testes Sênior e especialista na metodologia BDD.
                
                Sua tarefa é gerar **múltiplos cenários de teste funcionais** (fluxo principal, alternativo e exceções) cobrindo a regra de negócio e o título fornecidos.
                
                ### Diretrizes BDD e de Qualidade
                1.  **Foco Comportamental:** Os cenários devem focar no **comportamento do sistema** do ponto de vista do usuário/negócio.
                2.  **Passos Gherkin (DSL):** Os passos 'Dado/Quando/Então' devem ser escritos em um nível de **linguagem de domínio** (negócio), e não em passos de interface de usuário (clicar em botão, digitar).
                3.  **Passos Claros:** Certifique-se de que a cláusula 'Quando' descreva a ação que dispara o comportamento, e a cláusula 'Então' descreve o resultado observável esperado.
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

        // O parsing deve ser mais robusto para lidar com retornos de erro
        if (respostaCompleta.startsWith("Falha ao chamar Gemini")) {
            return respostaCompleta; // Retorna a mensagem de erro detalhada
        }

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
     * Método interno para chamada à API do Gemini.
     */
    private String enviarPrompt(String prompt) {
        log.info("📡 Preparando requisição para Gemini...");

        // 1. Definição do Header
        HttpHeaders headers = new HttpHeaders();
        // Garante que o JSON de saída esteja em UTF-8
        headers.setContentType(new MediaType("application", "json", java.nio.charset.Charset.forName("UTF-8")));

        // 2. Criação do Corpo da Requisição (JSON Body) - Padrão Gemini
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        // Removido o 'config' para evitar o erro 400 inicial
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        // 3. Montagem da URL (Substituição do {model} + Chave de API)
        String urlBase = config.getUrl().replace("{model}", config.getModel());
        String urlComChave = String.format("%s?key=%s", urlBase, config.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(urlComChave, entity, String.class);

            log.info("✅ Status Gemini Recebido: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = mapper.readTree(response.getBody());

                // Parsing da resposta do Gemini: 'candidates[0].content.parts[0].text'
                JsonNode candidateNode = root.path("candidates").get(0);

                // Verifica se há um node de conteúdo antes de tentar o parsing
                if (candidateNode == null || candidateNode.path("content") == null) {
                    log.error("Resposta Gemini válida, mas sem conteúdo de texto. Corpo: {}", response.getBody());
                    return "Resposta Gemini vazia ou incompleta.";
                }

                String result = candidateNode
                        .path("content")
                        .path("parts").get(0)
                        .path("text").asText();

                return result != null && !result.isBlank() ? result : "Resposta vazia do Gemini";
            }

            // SE NÃO FOR 2XX (ex: 3xx Redirecionamento - improvável)
            log.error("❌ Resposta Gemini Não-Sucesso. Status: {}. Corpo: {}",
                    response.getStatusCode(), response.getBody());
            return "Falha ao chamar Gemini: Status " + response.getStatusCode();

        } catch (HttpClientErrorException e) {
            // Captura erros 4xx (Bad Request, Unauthorized, Forbidden)
            log.error("❌ ERRO HTTP (Cliente): Código {} - Corpo da Resposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return "Falha ao chamar Gemini (HTTP Erro " + e.getStatusCode() + " - " + e.getResponseBodyAsString().substring(0, Math.min(e.getResponseBodyAsString().length(), 100)) + "...)";
        } catch (ResourceAccessException e) {
            // Captura erros de rede (timeout, conexão, SSL)
            log.error("❌ ERRO DE REDE/CONEXÃO: {}", e.getMessage());
            return "Falha ao chamar Gemini (Erro de Conexão/Rede)";
        } catch (Exception e) {
            log.error("❌ ERRO INESPERADO no parsing ou I/O: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao processar a IA", e);
        }
    }
}