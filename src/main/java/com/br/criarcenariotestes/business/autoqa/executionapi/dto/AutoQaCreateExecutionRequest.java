package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Origem do que automatizar: um cenário já salvo (o da "Lista de Cenários de
 * Teste") ou um texto digitado direto.
 *
 * {@code cenarioId} é o caminho normal do produto — os cenários já foram
 * gerados, revisados e publicados no Zephyr, e reaproveitá-los evita
 * redigitar. {@code scenario} continua aceito para uso avulso, e porque era o
 * contrato anterior.
 *
 * A validação de "exatamente um dos dois" é feita no controller, e não com
 * {@code @NotBlank} em cada campo: anotação por campo não consegue expressar
 * exclusividade entre eles.
 */
public record AutoQaCreateExecutionRequest(
        String scenario,
        String cenarioId,

        /** Continua obrigatório: sem pasta não há onde criar a automação. */
        @NotBlank String projectPath,

        /**
         * Canal da automação (WEB_UI, API, MOBILE...), informado pelo usuário.
         *
         * Opcional: em branco, o discovery deduz a partir do framework do
         * projeto. Existe porque essa dedução falha em projeto novo ou sem
         * dependência reconhecível, e aí a IA marca "canal não informado" como
         * ambiguidade BLOQUEANTE — o pipeline para antes do planejamento.
         * Quando informado, é a fonte autoritativa: quem está criando o teste
         * sabe se quer UI ou API melhor que qualquer heurística.
         */
        String automationType,

        /**
         * Framework a usar (PLAYWRIGHT, CYPRESS, SELENIDE...), informado pelo
         * usuário.
         *
         * Opcional: em branco, vale o que o discovery detectar. Existe porque
         * projeto novo — ou com só JUnit no build, sem nenhuma dependência de
         * automação — não dá o que detectar, e a geração é barrada com
         * "unsupported-framework" antes de qualquer chamada de IA.
         *
         * Eixo distinto de {@link #automationType}: o framework decide
         * linguagem, extensão e convenções; o canal decide qual API dele usar.
         */
        String automationFramework
) {
    /** Mantém compatível quem já construía com (scenario, projectPath). */
    public AutoQaCreateExecutionRequest(String scenario, String projectPath) {
        this(scenario, null, projectPath, null, null);
    }

    /** Mantém compatível quem já construía sem o canal de automação. */
    public AutoQaCreateExecutionRequest(String scenario, String cenarioId, String projectPath) {
        this(scenario, cenarioId, projectPath, null, null);
    }

    /** Mantém compatível quem já construía sem o framework. */
    public AutoQaCreateExecutionRequest(String scenario, String cenarioId, String projectPath, String automationType) {
        this(scenario, cenarioId, projectPath, automationType, null);
    }

    public boolean temAutomationType() {
        return automationType != null && !automationType.isBlank();
    }

    public boolean temAutomationFramework() {
        return automationFramework != null && !automationFramework.isBlank();
    }

    public boolean temCenarioId() {
        return cenarioId != null && !cenarioId.isBlank();
    }

    public boolean temScenario() {
        return scenario != null && !scenario.isBlank();
    }
}
