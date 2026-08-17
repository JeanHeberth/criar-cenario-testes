package com.br.criarcenariotestes.business.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.br.criarcenariotestes.business.properties.ClaudeProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
public class ClaudeProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);

    /**
     * Níveis de esforço em que a API rejeita (HTTP 400) thinking desligado.
     * Ver ClaudeProperties#thinkingEnabled.
     */
    private static final List<String> EFFORTS_INCOMPATIVEIS_COM_THINKING_DESLIGADO =
            List.of("xhigh", "max");

    private final ClaudeProperties properties;

    /**
     * Criado sob demanda (e reaproveitado) em vez de virar @Bean: a validação
     * de configuração acontece na chamada, como nos demais providers, então a
     * aplicação sobe normalmente mesmo sem ANTHROPIC_API_KEY quando o provider
     * ativo é outro.
     */
    private volatile AnthropicClient client;

    @Override
    public String getName() {
        return "claude";
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

    private String gerarRespostaComHistorico(String systemPrompt,
                                             List<Map<String, String>> history,
                                             Integer maxTokensOverride) {
        validarConfiguracao();

        int maxTokens = resolverMaxTokens(maxTokensOverride);
        String effort = resolverEffort();

        log.info("Claude request. model='{}', maxTokens={}, effort='{}', thinking={}, systemPromptLength={}, historySize={}",
                properties.getModel(),
                maxTokens,
                effort,
                properties.isThinkingEnabled(),
                systemPrompt == null ? 0 : systemPrompt.length(),
                history == null ? 0 : history.size());

        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(properties.getModel())
                .maxTokens(maxTokens)
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.of(effort))
                        .build());

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.system(systemPrompt);
        }

        if (properties.isThinkingEnabled()) {
            builder.thinking(ThinkingConfigAdaptive.builder().build());
        } else {
            builder.thinking(ThinkingConfigDisabled.builder().build());
        }

        // A API exige que a conversa comece por "user"; se o histórico vier
        // vazio mandamos uma mensagem mínima em vez de estourar um 400 opaco.
        if (history == null || history.isEmpty()) {
            builder.addUserMessage("");
        } else {
            for (Map<String, String> msg : history) {
                String conteudo = msg.get("content");
                if (conteudo == null) {
                    conteudo = "";
                }
                if ("assistant".equalsIgnoreCase(msg.get("role"))) {
                    builder.addAssistantMessage(conteudo);
                } else {
                    builder.addUserMessage(conteudo);
                }
            }
        }

        try {
            Message response = getClient().messages().create(builder.build());

            String stopReason = response.stopReason()
                    .map(Object::toString)
                    .orElse(null);
            String result = extrairTexto(response);

            log.info("Claude response recebida. model='{}', responseLength={}, stopReason='{}', preview='{}'",
                    properties.getModel(),
                    result == null ? 0 : result.length(),
                    stopReason,
                    gerarPreview(result));

            // Mesmo racional do OpenAiProvider/GeminiProvider: "max_tokens"
            // significa corte pelo limite de saída, não conclusão natural.
            // Apenas observabilidade - a validação estrutural continua sendo
            // quem reprova o conteúdo.
            if (stopReason != null && stopReason.toLowerCase().contains("max_tokens")) {
                log.warn("Claude truncou a resposta pelo limite de tokens de saída (stopReason='{}'). " +
                        "maxTokens={}, responseLength={}", stopReason, maxTokens, result == null ? 0 : result.length());
            }

            // Recusa por política: HTTP 200 com content vazio ou parcial. Sem
            // este tratamento explícito o erro apareceria como "resposta vazia"
            // e mandaria quem investiga procurar bug de parsing.
            if (stopReason != null && stopReason.toLowerCase().contains("refusal")) {
                String detalhe = response.stopDetails()
                        .map(Object::toString)
                        .orElse("sem detalhes");
                log.error("Claude recusou a requisição por política. detalhes={}", detalhe);
                throw new RuntimeException("Claude recusou a requisição (stop_reason=refusal): " + detalhe);
            }

            if (result == null || result.isBlank()) {
                throw new RuntimeException("Resposta vazia do Claude");
            }

            return result;

        } catch (RuntimeException e) {
            log.error("❌ Erro ao chamar Claude", e);
            throw new RuntimeException("Erro ao comunicar com Claude", e);
        }
    }

    /**
     * No Claude o raciocínio consome o mesmo orçamento de maxTokens que o texto
     * final. Quando o chamador pede um teto específico (ex.: o gerador de
     * cenários pede 8000) somamos a folga de thinking, senão o raciocínio come
     * o orçamento e a resposta volta truncada no meio de um cenário.
     */
    int resolverMaxTokens(Integer maxTokensOverride) {
        int base = maxTokensOverride != null ? maxTokensOverride : properties.getMaxTokens();
        if (!properties.isThinkingEnabled()) {
            return base;
        }
        int folga = properties.getThinkingHeadroomTokens() != null
                ? properties.getThinkingHeadroomTokens() : 0;
        return Math.max(base + folga, properties.getMaxTokens());
    }

    /**
     * Thinking desligado é rejeitado com HTTP 400 em effort xhigh/max. Em vez
     * de deixar a chamada falhar por uma combinação de configuração inválida,
     * rebaixamos para "high" e registramos o ajuste.
     */
    String resolverEffort() {
        String effort = properties.getEffort() == null || properties.getEffort().isBlank()
                ? "medium" : properties.getEffort().trim().toLowerCase();

        if (!properties.isThinkingEnabled() && EFFORTS_INCOMPATIVEIS_COM_THINKING_DESLIGADO.contains(effort)) {
            log.warn("Effort '{}' é incompatível com thinking desligado (a API rejeita com HTTP 400). " +
                    "Rebaixando para 'high'.", effort);
            return "high";
        }

        return effort;
    }

    private String extrairTexto(Message response) {
        StringJoiner joiner = new StringJoiner("\n");
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(texto -> joiner.add(texto.text()));
        }
        return joiner.toString();
    }

    private AnthropicClient getClient() {
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    local = AnthropicOkHttpClient.builder()
                            .apiKey(properties.getApiKey())
                            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                            .build();
                    client = local;
                }
            }
        }
        return local;
    }

    private void validarConfiguracao() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY não configurada");
        }
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            throw new IllegalStateException("Modelo Claude não configurado");
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
