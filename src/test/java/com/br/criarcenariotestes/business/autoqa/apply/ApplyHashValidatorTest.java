package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyIoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApplyHashValidator - Testes Unitários")
class ApplyHashValidatorTest {

    private final ApplyHashValidator validator = new ApplyHashValidator();

    @Test
    @DisplayName("Deve calcular SHA-256 conhecido para string vazia")
    void deveCalcularShaDeStringVazia() {
        assertThat(validator.sha256(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Deve calcular o mesmo hash para o mesmo conteúdo")
    void deveCalcularMesmoHashParaMesmoConteudo() {
        String h1 = validator.sha256("conteudo idêntico");
        String h2 = validator.sha256("conteudo idêntico");

        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64);
    }

    @Test
    @DisplayName("Deve calcular hashes diferentes para conteúdos diferentes")
    void deveCalcularHashesDiferentes() {
        assertThat(validator.sha256("a")).isNotEqualTo(validator.sha256("b"));
    }

    @Test
    @DisplayName("Deve calcular hash de arquivo em disco igual ao hash do conteúdo em memória")
    void deveCalcularHashDeArquivo(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("arquivo.txt");
        Files.writeString(file, "conteudo do arquivo", StandardCharsets.UTF_8);

        assertThat(validator.sha256OfFile(file)).isEqualTo(validator.sha256("conteudo do arquivo"));
    }

    @Test
    @DisplayName("Deve lançar ApplyIoException para arquivo inexistente")
    void deveLancarApplyIoExceptionParaArquivoInexistente(@TempDir Path dir) {
        Path missing = dir.resolve("nao-existe.txt");

        assertThatThrownBy(() -> validator.sha256OfFile(missing))
                .isInstanceOf(ApplyIoException.class);
    }

    @Test
    @DisplayName("matches deve retornar true apenas quando hashes são iguais e não nulos")
    void deveCompararHashesCorretamente() {
        assertThat(validator.matches("abc", "abc")).isTrue();
        assertThat(validator.matches("abc", "def")).isFalse();
        assertThat(validator.matches(null, "abc")).isFalse();
    }
}
