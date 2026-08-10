package com.br.criarcenariotestes.business.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppCorsProperties - Testes Unitários (Fase 13.1B)")
class AppCorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(AppCorsProperties.class)
    @org.springframework.boot.autoconfigure.ImportAutoConfiguration(PropertyPlaceholderAutoConfiguration.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("allowedOrigins deve ser vazio por padrão, nunca nulo (fail-closed)")
    void allowedOriginsDeveSerVazioPorPadrao() {
        contextRunner.run(context -> {
            AppCorsProperties properties = context.getBean(AppCorsProperties.class);
            assertThat(properties.getAllowedOrigins()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Deve fazer bind de uma lista explícita (application.yml estilo lista)")
    void deveFazerBindDeListaExplicita() {
        contextRunner
                .withPropertyValues(
                        "app.cors.allowed-origins[0]=http://localhost:4200",
                        "app.cors.allowed-origins[1]=http://100.83.72.100:9999"
                )
                .run(context -> {
                    AppCorsProperties properties = context.getBean(AppCorsProperties.class);
                    assertThat(properties.getAllowedOrigins())
                            .containsExactly("http://localhost:4200", "http://100.83.72.100:9999");
                });
    }

    @Test
    @DisplayName("Deve fazer bind de uma única variável com valores separados por vírgula (padrão de variável de ambiente)")
    void deveFazerBindDeValorUnicoSeparadoPorVirgula() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=http://localhost:4200,http://100.83.72.100:9999")
                .run(context -> {
                    AppCorsProperties properties = context.getBean(AppCorsProperties.class);
                    assertThat(properties.getAllowedOrigins())
                            .containsExactly("http://localhost:4200", "http://100.83.72.100:9999");
                });
    }

    @Test
    @DisplayName("Deve fazer bind de uma única origem")
    void deveFazerBindDeUmaUnicaOrigem() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=http://localhost:4200")
                .run(context -> {
                    AppCorsProperties properties = context.getBean(AppCorsProperties.class);
                    assertThat(properties.getAllowedOrigins()).containsExactly("http://localhost:4200");
                });
    }

    @Test
    @DisplayName("Valor em branco explícito deve resultar em lista vazia, não em uma origem em branco")
    void valorEmBrancoDeveResultarEmListaVazia() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=")
                .run(context -> {
                    AppCorsProperties properties = context.getBean(AppCorsProperties.class);
                    assertThat(properties.getAllowedOrigins()).isEmpty();
                });
    }
}
