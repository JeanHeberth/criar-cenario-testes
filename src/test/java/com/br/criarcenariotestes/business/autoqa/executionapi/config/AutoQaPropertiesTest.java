package com.br.criarcenariotestes.business.autoqa.executionapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoQaProperties - Testes Unitários")
class AutoQaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(AutoQaProperties.class)
    @org.springframework.boot.autoconfigure.ImportAutoConfiguration(PropertyPlaceholderAutoConfiguration.class)
    static class TestConfig {
    }

    @Test
    @DisplayName("Deve usar defaults seguros quando nenhuma propriedade for informada")
    void deveUsarDefaultsSegurosQuandoNadaInformado() {
        contextRunner.run(context -> {
            AutoQaProperties properties = context.getBean(AutoQaProperties.class);
            assertThat(properties.isAllowCommandExecution()).isFalse();
            assertThat(properties.isAllowFileApplication()).isFalse();
            assertThat(properties.isSensitiveActionsEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("Deve fazer bind das propriedades do application.yml (prefixo auto-qa)")
    void deveFazerBindDasPropriedades() {
        contextRunner
                .withPropertyValues(
                        "auto-qa.enabled=true",
                        "auto-qa.allowed-roots[0]=/projeto/permitido",
                        "auto-qa.max-files=500",
                        "auto-qa.max-file-size-kb=500",
                        "auto-qa.max-total-content-kb=5000",
                        "auto-qa.max-generation-retries=3",
                        "auto-qa.generated-directory=.auto-qa/generated",
                        "auto-qa.backup-directory=.auto-qa/backups",
                        "auto-qa.allow-command-execution=false",
                        "auto-qa.allow-file-application=false",
                        "auto-qa.sensitive-actions-enabled=false",
                        "auto-qa.retention-days=30",
                        "auto-qa.max-concurrent-executions=5"
                )
                .run(context -> {
                    AutoQaProperties properties = context.getBean(AutoQaProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getAllowedRoots()).containsExactly("/projeto/permitido");
                    assertThat(properties.getMaxFiles()).isEqualTo(500);
                    assertThat(properties.getMaxFileSizeKb()).isEqualTo(500);
                    assertThat(properties.getMaxTotalContentKb()).isEqualTo(5000);
                    assertThat(properties.getMaxGenerationRetries()).isEqualTo(3);
                    assertThat(properties.getGeneratedDirectory()).isEqualTo(".auto-qa/generated");
                    assertThat(properties.getBackupDirectory()).isEqualTo(".auto-qa/backups");
                    assertThat(properties.getRetentionDays()).isEqualTo(30);
                    assertThat(properties.getMaxConcurrentExecutions()).isEqualTo(5);
                });
    }

    @Test
    @DisplayName("Deve permitir habilitar ações sensíveis explicitamente")
    void devePermitirHabilitarAcoesSensiveisExplicitamente() {
        contextRunner
                .withPropertyValues(
                        "auto-qa.allow-command-execution=true",
                        "auto-qa.allow-file-application=true",
                        "auto-qa.sensitive-actions-enabled=true"
                )
                .run(context -> {
                    AutoQaProperties properties = context.getBean(AutoQaProperties.class);
                    assertThat(properties.isAllowCommandExecution()).isTrue();
                    assertThat(properties.isAllowFileApplication()).isTrue();
                    assertThat(properties.isSensitiveActionsEnabled()).isTrue();
                });
    }

    @Test
    @DisplayName("allowedRoots deve ser vazio por padrão, nunca nulo")
    void allowedRootsDeveSerVazioPorPadrao() {
        contextRunner.run(context -> {
            AutoQaProperties properties = context.getBean(AutoQaProperties.class);
            assertThat(properties.getAllowedRoots()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("enabled deve ser true por padrão")
    void enabledDeveSerTruePorPadrao() {
        contextRunner.run(context -> {
            AutoQaProperties properties = context.getBean(AutoQaProperties.class);
            assertThat(properties.isEnabled()).isTrue();
        });
    }
}
