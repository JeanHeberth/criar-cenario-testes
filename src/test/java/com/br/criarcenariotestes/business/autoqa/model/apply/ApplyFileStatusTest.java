package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyFileStatus - Testes Unitários")
class ApplyFileStatusTest {

    @Test
    @DisplayName("Deve conter todos os status esperados")
    void deveConterStatusEsperados() {
        assertThat(ApplyFileStatus.values()).containsExactly(
                ApplyFileStatus.APPLIED,
                ApplyFileStatus.SKIPPED,
                ApplyFileStatus.BACKED_UP,
                ApplyFileStatus.CONFLICT,
                ApplyFileStatus.ROLLED_BACK,
                ApplyFileStatus.FAILED
        );
    }
}
