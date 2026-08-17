package com.br.criarcenariotestes.business.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.claude")
public class ClaudeProperties {

    private String apiKey;
    private String model = "claude-opus-5";
    private Integer maxTokens = 16000;

    /**
     * Nível de esforço (low | medium | high | xhigh | max). Controla
     * profundidade de raciocínio e gasto total de tokens. Geração de cenário é
     * tarefa de formatação estruturada, não raciocínio longo — "medium" entrega
     * a mesma qualidade com bem menos tokens que o padrão "high" da API.
     */
    private String effort = "medium";

    /**
     * No Claude o thinking divide o mesmo orçamento de maxTokens com o texto da
     * resposta. Desligar seria o análogo do que fizemos no Gemini, mas aqui tem
     * efeito colateral conhecido: com thinking desligado o modelo pode vazar
     * tags {@code <thinking>} no texto visível — e o CenarioTextoParser quebraria
     * com isso. Por isso mantemos ligado e controlamos custo pelo effort.
     */
    private boolean thinkingEnabled = true;

    /**
     * Folga somada ao limite de tokens pedido pelo chamador quando o thinking
     * está ligado, já que raciocínio e texto final competem pelo mesmo teto.
     * Sem essa folga, um override de 8000 tokens (TestScenarioAgent) pode ser
     * consumido pelo raciocínio e devolver cenários truncados.
     */
    private Integer thinkingHeadroomTokens = 8000;

    /** Timeout da requisição. Respostas com thinking podem levar minutos. */
    private Integer timeoutSeconds = 600;
}
