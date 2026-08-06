package com.br.criarcenariotestes.business.autoqa.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutionCommandId - Testes Unitários")
class ExecutionCommandIdTest {

    @Test
    @DisplayName("Deve conter exatamente os 11 identificadores previstos")
    void deveConterIdentificadoresPrevistos() {
        assertThat(ExecutionCommandId.values()).containsExactly(
                ExecutionCommandId.NPM_TEST,
                ExecutionCommandId.NPM_TEST_E2E,
                ExecutionCommandId.PLAYWRIGHT_TEST,
                ExecutionCommandId.CYPRESS_RUN,
                ExecutionCommandId.CYPRESS_SCRIPT_RUN,
                ExecutionCommandId.GRADLE_WRAPPER_TEST,
                ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST,
                ExecutionCommandId.MAVEN_WRAPPER_TEST,
                ExecutionCommandId.MAVEN_TEST,
                ExecutionCommandId.ROBOT_TEST,
                ExecutionCommandId.PYTEST
        );
    }
}
