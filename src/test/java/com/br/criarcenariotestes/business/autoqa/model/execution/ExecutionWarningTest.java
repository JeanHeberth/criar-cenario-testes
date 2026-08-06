package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionWarning - Testes Unitários")
class ExecutionWarningTest {

    @Test
    @DisplayName("Deve remover espaços de code e description")
    void deveRemoverEspacos() {
        ExecutionWarning warning = new ExecutionWarning("  TIMEOUT_REACHED  ", "  timeout  ", true);

        assertThat(warning.code()).isEqualTo("TIMEOUT_REACHED");
        assertThat(warning.description()).isEqualTo("timeout");
        assertThat(warning.blocking()).isTrue();
    }

    @Test
    @DisplayName("Deve permitir blocking=false para avisos informativos")
    void devePermitirBlockingFalso() {
        ExecutionWarning warning = new ExecutionWarning("STDOUT_TRUNCATED", "saída truncada", false);

        assertThat(warning.blocking()).isFalse();
    }
}
