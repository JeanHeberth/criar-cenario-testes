package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionStatus - Testes Unitários")
class ExecutionStatusTest {

    @Test
    @DisplayName("Deve conter PASSED, FAILED, TIMED_OUT, BLOCKED, ERROR e CANCELLED")
    void deveConterStatusPrevistos() {
        assertThat(ExecutionStatus.values()).containsExactly(
                ExecutionStatus.PASSED,
                ExecutionStatus.FAILED,
                ExecutionStatus.TIMED_OUT,
                ExecutionStatus.BLOCKED,
                ExecutionStatus.ERROR,
                ExecutionStatus.CANCELLED
        );
    }
}
