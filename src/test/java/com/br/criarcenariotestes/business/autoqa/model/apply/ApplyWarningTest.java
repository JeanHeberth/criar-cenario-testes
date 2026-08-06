package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplyWarning - Testes Unitários")
class ApplyWarningTest {

    @Test
    @DisplayName("Deve remover espaços de code e description")
    void deveRemoverEspacos() {
        ApplyWarning warning = new ApplyWarning("  CODE  ", "  desc  ", "CRITICAL", true);

        assertThat(warning.code()).isEqualTo("CODE");
        assertThat(warning.description()).isEqualTo("desc");
        assertThat(warning.severity()).isEqualTo("CRITICAL");
        assertThat(warning.blocking()).isTrue();
    }

    @Test
    @DisplayName("Deve usar INFO como severidade padrão quando nula")
    void deveUsarInfoComoPadrao() {
        ApplyWarning warning = new ApplyWarning("CODE", "desc", null, false);

        assertThat(warning.severity()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Deve usar INFO como severidade padrão quando em branco")
    void deveUsarInfoQuandoEmBranco() {
        ApplyWarning warning = new ApplyWarning("CODE", "desc", "   ", false);

        assertThat(warning.severity()).isEqualTo("INFO");
    }
}
