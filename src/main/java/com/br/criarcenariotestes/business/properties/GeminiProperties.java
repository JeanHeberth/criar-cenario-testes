package com.br.criarcenariotestes.business.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {

    private String apiKey;
    private String model;
    private String url;
    /**
     * Nome alinhado a OpenAiProperties/ClaudeProperties de propósito: o campo
     * se chamava maxOutputTokens enquanto o YAML trazia "max-tokens", e o
     * binding relaxado do Spring mapeia essa chave para "maxTokens". Não
     * casava com nada, então a configuração era ignorada em SILÊNCIO e o valor
     * real sempre foi este default — nem quem escreveu o application.yml
     * conseguia mudá-lo.
     */
    private Integer maxTokens = 8000;
    // Desliga o "thinking" dos modelos 2.5, cujos tokens de raciocínio
    // consomem o mesmo orçamento de maxTokens e truncam a resposta.
    private boolean disableThinking = true;
}