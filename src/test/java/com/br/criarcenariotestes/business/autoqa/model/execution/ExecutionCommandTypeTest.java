package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionCommandType - Testes Unitários")
class ExecutionCommandTypeTest {

    @Test
    @DisplayName("Deve conter TEST, BUILD_AND_TEST, DISCOVERY e UNKNOWN")
    void deveConterTiposPrevistos() {
        assertThat(ExecutionCommandType.values()).containsExactly(
                ExecutionCommandType.TEST,
                ExecutionCommandType.BUILD_AND_TEST,
                ExecutionCommandType.DISCOVERY,
                ExecutionCommandType.UNKNOWN
        );
    }
}
