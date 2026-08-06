package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyStatus - Testes Unitários")
class ApplyStatusTest {

    @Test
    @DisplayName("Deve conter todos os status esperados")
    void deveConterStatusEsperados() {
        assertThat(ApplyStatus.values()).containsExactly(
                ApplyStatus.COMPLETED,
                ApplyStatus.COMPLETED_WITH_WARNINGS,
                ApplyStatus.ROLLED_BACK,
                ApplyStatus.FAILED,
                ApplyStatus.BLOCKED
        );
    }
}
