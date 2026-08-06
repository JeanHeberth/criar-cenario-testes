package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyIoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AtomicFileApplicationService - Testes Unitários")
class AtomicFileApplicationServiceTest {

    private final AtomicFileApplicationService service = new AtomicFileApplicationService();

    @Test
    @DisplayName("CREATE deve escrever novo arquivo com conteúdo UTF-8")
    void createDeveEscreverNovoArquivo(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("novo/Foo.java");

        service.writeCreate(target, "conteúdo é UTF-8");

        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("conteúdo é UTF-8");
    }

    @Test
    @DisplayName("CREATE deve rejeitar quando o alvo já existe, nunca sobrescrevendo")
    void createDeveRejeitarQuandoAlvoJaExiste(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("Foo.java");
        Files.writeString(target, "original");

        assertThatThrownBy(() -> service.writeCreate(target, "novo"))
                .isInstanceOf(ApplyIoException.class);
        assertThat(Files.readString(target)).isEqualTo("original");
    }

    @Test
    @DisplayName("CREATE não deve deixar arquivo temporário para trás")
    void createNaoDeveDeixarTemporario(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("Foo.java");

        service.writeCreate(target, "conteudo");

        try (Stream<Path> files = Files.list(dir)) {
            assertThat(files).noneMatch(p -> p.getFileName().toString().contains(".tmp"));
        }
    }

    @Test
    @DisplayName("UPDATE deve substituir conteúdo de arquivo existente")
    void updateDeveSubstituirConteudo(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("Foo.java");
        Files.writeString(target, "versão antiga");

        service.writeUpdate(target, "versão nova");

        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("versão nova");
    }

    @Test
    @DisplayName("UPDATE deve rejeitar quando o alvo não existe")
    void updateDeveRejeitarQuandoAlvoNaoExiste(@TempDir Path dir) {
        Path target = dir.resolve("NaoExiste.java");

        assertThatThrownBy(() -> service.writeUpdate(target, "conteudo"))
                .isInstanceOf(ApplyIoException.class);
    }

    @Test
    @DisplayName("UPDATE não deve deixar arquivo temporário para trás")
    void updateNaoDeveDeixarTemporario(@TempDir Path dir) throws IOException {
        Path target = dir.resolve("Foo.java");
        Files.writeString(target, "v1");

        service.writeUpdate(target, "v2");

        try (Stream<Path> files = Files.list(dir)) {
            assertThat(files).noneMatch(p -> p.getFileName().toString().contains(".tmp"));
        }
    }

    @Test
    @DisplayName("CREATE deve rejeitar target nulo")
    void createDeveRejeitarTargetNulo() {
        assertThatThrownBy(() -> service.writeCreate(null, "x")).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("CREATE deve rejeitar content nulo")
    void createDeveRejeitarContentNulo(@TempDir Path dir) {
        assertThatThrownBy(() -> service.writeCreate(dir.resolve("x.txt"), null)).isInstanceOf(NullPointerException.class);
    }
}
