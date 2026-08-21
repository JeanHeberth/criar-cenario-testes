package com.br.criarcenariotestes.business.autoqa.navegacao;

import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaProjectPathNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NavegacaoPastasService - Testes Unitários")
class NavegacaoPastasServiceTest {

    @TempDir
    Path raiz;

    private AutoQaProperties properties;
    private NavegacaoPastasService service;

    @BeforeEach
    void setUp() {
        properties = new AutoQaProperties();
        service = new NavegacaoPastasService(new ProjectPathSecurityValidator(properties));
    }

    private void autorizar(Path... roots) {
        properties.setAllowedRoots(java.util.Arrays.stream(roots).map(Path::toString).toList());
    }

    @Test
    @DisplayName("Sem allowed-roots configurada, não deve navegar nada (fail-closed)")
    void semRaizesConfiguradasNaoDeveNavegar() {
        NavegacaoPastasResponse resposta = service.listar(null);

        assertThat(resposta.pastas()).isEmpty();
        assertThat(resposta.caminhoAtual()).isNull();
        assertThat(resposta.selecionavel()).isFalse();
    }

    @Test
    @DisplayName("Sem caminho, deve listar as raízes autorizadas")
    void semCaminhoDeveListarRaizes() throws IOException {
        Path projetos = Files.createDirectory(raiz.resolve("projetos"));
        autorizar(projetos);

        NavegacaoPastasResponse resposta = service.listar(null);

        assertThat(resposta.pastas()).extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("projetos");
        // Ainda não se está dentro de uma pasta - nada a selecionar aqui.
        assertThat(resposta.selecionavel()).isFalse();
    }

    @Test
    @DisplayName("Deve listar apenas subpastas, nunca arquivos")
    void deveListarApenasSubpastas() throws IOException {
        Files.createDirectory(raiz.resolve("api"));
        Files.createDirectory(raiz.resolve("front"));
        Files.writeString(raiz.resolve("README.md"), "não deve aparecer");
        autorizar(raiz);

        NavegacaoPastasResponse resposta = service.listar(raiz.toString());

        assertThat(resposta.pastas()).extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("api", "front");
    }

    @Test
    @DisplayName("Não deve listar pastas ocultas - só poluem a escolha do projeto")
    void naoDeveListarPastasOcultas() throws IOException {
        Files.createDirectory(raiz.resolve("projeto"));
        Files.createDirectory(raiz.resolve(".git"));
        autorizar(raiz);

        assertThat(service.listar(raiz.toString()).pastas())
                .extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("projeto");
    }

    @Test
    @DisplayName("Caminho fora das raízes autorizadas deve ser rejeitado")
    void caminhoForaDasRaizesDeveSerRejeitado() throws IOException {
        Path permitida = Files.createDirectory(raiz.resolve("permitida"));
        Path proibida = Files.createDirectory(raiz.resolve("proibida"));
        autorizar(permitida);

        assertThatThrownBy(() -> service.listar(proibida.toString()))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Numa raiz autorizada, não deve permitir subir além dela")
    void naRaizNaoDeveTerPai() throws IOException {
        Path projetos = Files.createDirectory(raiz.resolve("projetos"));
        autorizar(projetos);

        assertThat(service.listar(projetos.toString()).caminhoPai()).isNull();
    }

    @Test
    @DisplayName("Dentro de uma subpasta, o pai deve ser navegável")
    void dentroDeSubpastaPaiDeveSerNavegavel() throws IOException {
        Path projetos = Files.createDirectory(raiz.resolve("projetos"));
        Path api = Files.createDirectory(projetos.resolve("api"));
        autorizar(projetos);

        NavegacaoPastasResponse resposta = service.listar(api.toString());

        // toRealPath() no esperado: o serviço devolve o caminho real, e no
        // macOS o diretório temporário fica sob /var, que é symlink para
        // /private/var. Comparar com o caminho não resolvido falharia aqui
        // por causa do ambiente, não do comportamento.
        assertThat(resposta.caminhoPai()).isEqualTo(projetos.toRealPath().toString());
        assertThat(resposta.selecionavel()).isTrue();
    }

    @Test
    @DisplayName("Deve ordenar as pastas ignorando maiúsculas")
    void deveOrdenarIgnorandoCaixa() throws IOException {
        Files.createDirectory(raiz.resolve("Zebra"));
        Files.createDirectory(raiz.resolve("api"));
        Files.createDirectory(raiz.resolve("Beta"));
        autorizar(raiz);

        assertThat(service.listar(raiz.toString()).pastas())
                .extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("api", "Beta", "Zebra");
    }

    @Test
    @DisplayName("Deve devolver o caminho real, com symlink resolvido")
    void deveDevolverCaminhoRealComSymlinkResolvido() throws IOException {
        Path real = Files.createDirectory(raiz.resolve("real"));
        Path link = raiz.resolve("atalho");
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException | IOException e) {
            return; // filesystem sem symlink: nada a verificar
        }
        autorizar(raiz);

        assertThat(service.listar(link.toString()).caminhoAtual())
                .isEqualTo(real.toRealPath().toString());
    }

    @Test
    @DisplayName("Múltiplas raízes autorizadas devem aparecer todas")
    void multiplasRaizesDevemAparecer() throws IOException {
        Path a = Files.createDirectory(raiz.resolve("alpha"));
        Path b = Files.createDirectory(raiz.resolve("bravo"));
        autorizar(a, b);

        assertThat(service.listar(null).pastas())
                .extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("alpha", "bravo");
    }

    @Test
    @DisplayName("Raiz configurada inexistente deve ser ignorada, não quebrar a navegação")
    void raizInexistenteDeveSerIgnorada() throws IOException {
        Path existente = Files.createDirectory(raiz.resolve("existente"));
        properties.setAllowedRoots(List.of(existente.toString(), raiz.resolve("nao-existe").toString()));

        assertThat(service.listar(null).pastas())
                .extracting(NavegacaoPastasResponse.PastaNavegavel::nome)
                .containsExactly("existente");
    }
}
