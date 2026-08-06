package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyOperation - Testes Unitários")
class ApplyOperationTest {

    @Test
    @DisplayName("Deve conter exatamente CREATE, UPDATE, REUSE e NONE, sem DELETE")
    void deveConterOperacoesPermitidas() {
        assertThat(ApplyOperation.values())
                .containsExactly(ApplyOperation.CREATE, ApplyOperation.UPDATE, ApplyOperation.REUSE, ApplyOperation.NONE);
    }
}
