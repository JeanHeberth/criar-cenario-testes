package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationHashService - Testes Unitários")
class GenerationHashServiceTest {

    private GenerationHashService service;

    @BeforeEach
    void setUp() {
        service = new GenerationHashService();
    }

    @Test
    @DisplayName("Deve calcular SHA-256 corretamente")
    void deveCalcularSha256() {
        GeneratedFileHash hash = service.sha256("conteudo");
        assertThat(hash.algorithm()).isEqualTo("SHA-256");
        assertThat(hash.hex()).hasSize(64);
    }

    @Test
    @DisplayName("Deve ser determinístico para o mesmo conteúdo")
    void deveSerDeterministico() {
        GeneratedFileHash h1 = service.sha256("mesmo conteudo");
        GeneratedFileHash h2 = service.sha256("mesmo conteudo");
        assertThat(h1.hex()).isEqualTo(h2.hex());
    }

    @Test
    @DisplayName("Deve mudar o hash quando o conteúdo muda")
    void deveMudarComConteudo() {
        GeneratedFileHash h1 = service.sha256("conteudo A");
        GeneratedFileHash h2 = service.sha256("conteudo B");
        assertThat(h1.hex()).isNotEqualTo(h2.hex());
    }

    @Test
    @DisplayName("Deve usar hexadecimal minúsculo")
    void deveUsarHexLowercase() {
        GeneratedFileHash hash = service.sha256("Conteudo com Maiusculas");
        assertThat(hash.hex()).isEqualTo(hash.hex().toLowerCase());
        assertThat(hash.hex()).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Deve rejeitar conteúdo nulo")
    void deveRejeitarConteudoNulo() {
        assertThatThrownBy(() -> service.sha256(null)).isInstanceOf(NullPointerException.class);
    }
}
