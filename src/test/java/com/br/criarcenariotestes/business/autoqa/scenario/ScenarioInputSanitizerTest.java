package com.br.criarcenariotestes.business.autoqa.scenario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioInputSanitizer - Testes Unitários")
class ScenarioInputSanitizerTest {

    private final ScenarioInputSanitizer sanitizer = new ScenarioInputSanitizer();

    @Test
    @DisplayName("Deve redigir senha com igual")
    void deveRedigirSenhaComIgual() {
        assertThat(sanitizer.sanitize("senha=MinhaSenha123")).contains("senha=[REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir senha com dois pontos")
    void deveRedigirSenhaComDoisPontos() {
        assertThat(sanitizer.sanitize("password: MinhaSenha123")).contains("password: [REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir token")
    void deveRedigirToken() {
        assertThat(sanitizer.sanitize("token=abc123")).contains("token=[REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir api key")
    void deveRedigirApiKey() {
        assertThat(sanitizer.sanitize("api_key=super-secret")).contains("api_key=[REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir bearer token")
    void deveRedigirBearerToken() {
        assertThat(sanitizer.sanitize("Authorization: Bearer abc.def.ghi"))
                .contains("Authorization: Bearer [REDACTED]");
    }

    @Test
    @DisplayName("Deve preservar texto sem segredo")
    void devePreservarTextoSemSegredo() {
        String scenario = ScenarioAnalysisTestData.innocentScenario();

        assertThat(sanitizer.sanitize(scenario)).isEqualTo(scenario);
    }

    @Test
    @DisplayName("Deve redigir credenciais em URL")
    void deveRedigirCredenciaisEmUrl() {
        assertThat(sanitizer.sanitize(ScenarioAnalysisTestData.urlCredentialScenario()))
                .contains("https://usuario:[REDACTED]@host.local/servico");
    }
}
