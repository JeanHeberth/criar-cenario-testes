package com.br.criarcenariotestes.business.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O limite de tokens de saída precisa ser CONFIGURÁVEL de fato.
 *
 * <p>O campo do Gemini se chamava maxOutputTokens enquanto o YAML trazia
 * "max-tokens": o binding relaxado mapeia essa chave para "maxTokens" e não
 * casava com nada, então a configuração era ignorada em SILÊNCIO. O efeito foi
 * caro — respostas truncadas em MAX_TOKENS derrubando execuções inteiras, com
 * a chamada já paga, e nenhuma alteração no application.yml surtindo efeito.
 *
 * <p>Usa ApplicationContextRunner em vez de @SpringBootTest porque o alvo é o
 * BINDING, não a aplicação: sobe só a classe de propriedades, sem Mongo nem
 * chaves de API.
 */
class LimiteDeTokensConfiguravelTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class));

    @Test
    void aChaveDoYamlDeveChegarAoCampoDoGemini() {
        runner.withUserConfiguration(GeminiProperties.class)
                .withPropertyValues("ai.gemini.max-tokens=12345")
                .run(context -> assertThat(context.getBean(GeminiProperties.class).getMaxTokens())
                        .as("ai.gemini.max-tokens precisa vincular — antes era ignorada em silêncio")
                        .isEqualTo(12345));
    }

    @Test
    void aChaveDoYamlDeveChegarAoCampoDoOpenAi() {
        runner.withUserConfiguration(OpenAiProperties.class)
                .withPropertyValues("ai.openai.max-tokens=54321")
                .run(context -> assertThat(context.getBean(OpenAiProperties.class).getMaxTokens())
                        .isEqualTo(54321));
    }

    @Test
    void semConfiguracaoOGeminiUsaUmTetoCompativelComRespostasGrandes() {
        // 4000 truncava a análise de cenário (14 mil caracteres) no meio do JSON.
        runner.withUserConfiguration(GeminiProperties.class)
                .run(context -> assertThat(context.getBean(GeminiProperties.class).getMaxTokens())
                        .isGreaterThanOrEqualTo(8000));
    }
}
