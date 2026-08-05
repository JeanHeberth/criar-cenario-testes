package com.br.criarcenariotestes.business.autoqa.model.generation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedFileHash - Testes Unitários")
class GeneratedFileHashTest {

    @Test
    @DisplayName("Deve criar hash válido")
    void deveCriarHashValido() {
        GeneratedFileHash hash = new GeneratedFileHash("SHA-256", "abc123");

        assertThat(hash.algorithm()).isEqualTo("SHA-256");
        assertThat(hash.hex()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("Deve remover espaços do algoritmo e do hex")
    void deveTrimarAlgoritmoEHex() {
        GeneratedFileHash hash = new GeneratedFileHash("  SHA-256  ", "  abc123  ");

        assertThat(hash.algorithm()).isEqualTo("SHA-256");
        assertThat(hash.hex()).isEqualTo("abc123");
    }
}
