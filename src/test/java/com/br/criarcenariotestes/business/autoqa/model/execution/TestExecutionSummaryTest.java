package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TestExecutionSummary - Testes Unitários")
class TestExecutionSummaryTest {

    @Test
    @DisplayName("Deve criar summary válido")
    void deveCriarSummaryValido() {
        TestExecutionSummary summary = new TestExecutionSummary("PLAYWRIGHT", 10, 8, 2, 0, 0,
                List.of("login.spec.ts"), List.of());

        assertThat(summary.total()).isEqualTo(10);
        assertThat(summary.passed()).isEqualTo(8);
        assertThat(summary.failed()).isEqualTo(2);
        assertThat(summary.failedTests()).containsExactly("login.spec.ts");
    }

    @Test
    @DisplayName("Deve rejeitar total negativo")
    void deveRejeitarTotalNegativo() {
        assertThatThrownBy(() -> new TestExecutionSummary("PLAYWRIGHT", -1, 0, 0, 0, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar passed negativo")
    void deveRejeitarPassedNegativo() {
        assertThatThrownBy(() -> new TestExecutionSummary("PLAYWRIGHT", 0, -1, 0, 0, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar failed negativo")
    void deveRejeitarFailedNegativo() {
        assertThatThrownBy(() -> new TestExecutionSummary("PLAYWRIGHT", 0, 0, -1, 0, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar skipped negativo")
    void deveRejeitarSkippedNegativo() {
        assertThatThrownBy(() -> new TestExecutionSummary("PLAYWRIGHT", 0, 0, 0, -1, 0, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar errors negativo")
    void deveRejeitarErrorsNegativo() {
        assertThatThrownBy(() -> new TestExecutionSummary("PLAYWRIGHT", 0, 0, 0, 0, -1, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve permitir summary vazio (zero em tudo)")
    void devePermitirSummaryVazio() {
        TestExecutionSummary summary = new TestExecutionSummary(null, 0, 0, 0, 0, 0, List.of(), List.of());

        assertThat(summary.total()).isZero();
        assertThat(summary.framework()).isNull();
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        TestExecutionSummary summary = new TestExecutionSummary("PYTEST", 1, 1, 0, 0, 0, List.of(), List.of());

        assertThatThrownBy(() -> summary.failedTests().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> summary.warnings().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
