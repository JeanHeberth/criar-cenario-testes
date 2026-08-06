package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CommandSpecification - Testes Unitários")
class CommandSpecificationTest {

    @Test
    @DisplayName("Deve criar CommandSpecification válida")
    void deveCriarCommandSpecificationValida() {
        CommandSpecification spec = new CommandSpecification(
                ExecutionCommandId.GRADLE_WRAPPER_TEST, "./gradlew", List.of("test"),
                "criar-cenario-testes", Duration.ofMinutes(10), Map.of("PATH", "/usr/bin"), ExecutionCommandType.TEST
        );

        assertThat(spec.commandId()).isEqualTo(ExecutionCommandId.GRADLE_WRAPPER_TEST);
        assertThat(spec.executable()).isEqualTo("./gradlew");
        assertThat(spec.arguments()).containsExactly("test");
        assertThat(spec.workingDirectoryReference()).isEqualTo("criar-cenario-testes");
        assertThat(spec.timeout()).isEqualTo(Duration.ofMinutes(10));
        assertThat(spec.type()).isEqualTo(ExecutionCommandType.TEST);
    }

    @Test
    @DisplayName("Deve rejeitar commandId nulo")
    void deveRejeitarCommandIdNulo() {
        assertThatThrownBy(() -> new CommandSpecification(null, "./gradlew", List.of("test"), "proj",
                Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable nulo")
    void deveRejeitarExecutableNulo() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, null, List.of(), "proj",
                Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar executable em branco")
    void deveRejeitarExecutableEmBranco() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, "   ", List.of(), "proj",
                Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar timeout nulo")
    void deveRejeitarTimeoutNulo() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "proj",
                null, Map.of(), ExecutionCommandType.TEST))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar type nulo")
    void deveRejeitarTypeNulo() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "proj",
                Duration.ofMinutes(1), Map.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar arguments e environment imutáveis")
    void deveRetornarColecoesImutaveis() {
        CommandSpecification spec = new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of("-q"),
                "proj", Duration.ofMinutes(1), Map.of("PATH", "/usr/bin"), ExecutionCommandType.TEST);

        assertThatThrownBy(() -> spec.arguments().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> spec.environment().put("K", "V")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar environment contendo chave sensível")
    void deveRejeitarEnvironmentComChaveSensivel() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "proj",
                Duration.ofMinutes(1), Map.of("OPENAI_API_KEY", "sk-123"), ExecutionCommandType.TEST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sensível");
    }

    @Test
    @DisplayName("Deve rejeitar environment contendo TOKEN no nome")
    void deveRejeitarEnvironmentComToken() {
        assertThatThrownBy(() -> new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "proj",
                Duration.ofMinutes(1), Map.of("GITHUB_TOKEN", "ghp_123"), ExecutionCommandType.TEST))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve aceitar environment apenas com chaves permitidas")
    void deveAceitarEnvironmentComChavesPermitidas() {
        CommandSpecification spec = new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(), "proj",
                Duration.ofMinutes(1), Map.of("PATH", "/usr/bin", "TMPDIR", "/tmp"), ExecutionCommandType.TEST);

        assertThat(spec.environment()).containsKeys("PATH", "TMPDIR");
    }

    @Test
    @DisplayName("Não deve expor path absoluto na referência do working directory")
    void naoDeveExporPathAbsoluto() {
        CommandSpecification spec = new CommandSpecification(ExecutionCommandId.PYTEST, "pytest", List.of(),
                "meu-projeto", Duration.ofMinutes(1), Map.of(), ExecutionCommandType.TEST);

        assertThat(spec.workingDirectoryReference()).doesNotContain("/Users").doesNotContain("/home");
    }
}
