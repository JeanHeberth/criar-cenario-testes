package com.br.criarcenariotestes.business.autoqa.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProcessFactory - Testes Unitários")
class ProcessFactoryTest {

    @Test
    @DisplayName("Deve retornar o processo configurado, sem iniciar um processo real")
    void deveRetornarProcessoConfigurado() throws IOException {
        FakeProcess fake = FakeProcess.completed(0, "ok", "");
        ProcessFactory factory = new FakeProcessFactory(fake);

        Process result = factory.start(new ProcessBuilder("comando-inexistente"));

        assertThat(result).isSameAs(fake);
    }

    @Test
    @DisplayName("Deve propagar falha de início de processo")
    void devePropagarFalhaDeInicio() {
        ProcessFactory factory = FakeProcessFactory.failingToStart(new IOException("não encontrado"));

        assertThatThrownBy(() -> factory.start(new ProcessBuilder("comando-inexistente")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("Deve receber o ProcessBuilder com executable e arguments já separados")
    void deveReceberProcessBuilderComArgumentosSeparados() throws IOException {
        FakeProcessFactory factory = new FakeProcessFactory(FakeProcess.completed(0, "", ""));

        factory.start(new ProcessBuilder("pytest", "-q", "tests/"));

        assertThat(factory.lastProcessBuilder().command()).containsExactly("pytest", "-q", "tests/");
    }
}
