package com.br.criarcenariotestes.business.autoqa.scenario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão do falso positivo que travava o workflow: o detector de código
 * reprovava prosa em português por causa do ponto e vírgula.
 */
@DisplayName("ScenarioAnalysisValidator - detecção de código")
class ScenarioAnalysisValidatorCodigoTest {

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(?i)\\b(class|import|public|private|protected|return|function|def|let|const|var|new)\\b|[{}]|=>|;\\s*$",
            Pattern.MULTILINE);

    private boolean pareceCodigo(String texto) {
        return CODE_PATTERN.matcher(texto).find();
    }

    @Test
    @DisplayName("Ponto e vírgula em prosa não é código - era o que reprovava a análise inteira")
    void pontoEVirgulaEmProsaNaoEhCodigo() {
        assertThat(pareceCodigo("O usuário adiciona o produto ao carrinho; o sistema valida o estoque.")).isFalse();
        assertThat(pareceCodigo("Pré-condição: produto cadastrado; estoque igual a zero.")).isFalse();
    }

    @Test
    @DisplayName("Ponto e vírgula fechando linha continua sendo código")
    void pontoEVirgulaFechandoLinhaEhCodigo() {
        assertThat(pareceCodigo("int quantidade = 1;")).isTrue();
        assertThat(pareceCodigo("RestAssured.baseURI = \"http://localhost\";\nmais texto")).isTrue();
    }

    @Test
    @DisplayName("Palavras-chave e símbolos de código continuam sendo detectados")
    void codigoDeVerdadeContinuaSendoDetectado() {
        assertThat(pareceCodigo("import io.restassured.RestAssured")).isTrue();
        assertThat(pareceCodigo("public void teste()")).isTrue();
        assertThat(pareceCodigo("if (x) { y }")).isTrue();
        assertThat(pareceCodigo("() => valor")).isTrue();
    }

    @Test
    @DisplayName("Texto de análise legítimo deve passar")
    void textoLegitimoDevePassar() {
        assertThat(pareceCodigo("Validar a mensagem de erro exibida ao usuário.")).isFalse();
        assertThat(pareceCodigo("A quantidade máxima permitida por item é 1 unidade.")).isFalse();
        assertThat(pareceCodigo("Dado que o produto está sem estoque, quando o usuário adiciona, então recebe erro.")).isFalse();
    }

    private static final java.util.regex.Pattern UNIX_ABSOLUTE_PATH = java.util.regex.Pattern.compile(
            "(?<!\\S)/(?:Users|home|root|var|etc|tmp|opt|usr|bin|sbin|private|mnt|media|srv|proc|dev"
                    + "|Applications|Library|System|Volumes)(?:/[^\\s]*)?");

    private boolean pareceCaminhoDeDisco(String texto) {
        return UNIX_ABSOLUTE_PATH.matcher(texto).find();
    }

    @Test
    @DisplayName("Rota HTTP não é caminho de disco - sem isso, nenhum cenário de API passava")
    void rotaHttpNaoEhCaminhoDeDisco() {
        assertThat(pareceCaminhoDeDisco("Quando faço GET para /cenario/workflows")).isFalse();
        assertThat(pareceCaminhoDeDisco("chamar /api/v1/produtos")).isFalse();
        assertThat(pareceCaminhoDeDisco("a rota /login deve retornar 200")).isFalse();
    }

    @Test
    @DisplayName("Caminho de filesystem continua sendo barrado - é o que a regra protege")
    void caminhoDeFilesystemContinuaBarrado() {
        assertThat(pareceCaminhoDeDisco("/Users/jeanheberth/Development/projeto")).isTrue();
        assertThat(pareceCaminhoDeDisco("o arquivo em /etc/passwd")).isTrue();
        assertThat(pareceCaminhoDeDisco("salvo em /tmp/saida.txt")).isTrue();
        assertThat(pareceCaminhoDeDisco("/home/usuario/.ssh/id_rsa")).isTrue();
    }

    private static final java.util.regex.Pattern COMMAND_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(curl|npm|yarn|pnpm|mvn|gradle|git|docker|kubectl|chmod|ssh)\\s+"
                    + "(-{1,2}\\w|https?://|\\d{3,4}\\s|(?:test|run|install|build|clone|push|pull|exec|start|"
                    + "compose|apply|add|commit|init|clean|deploy)\\b)");

    private boolean pareceComando(String texto) {
        return COMMAND_PATTERN.matcher(texto).find();
    }

    @Test
    @DisplayName("Mencionar a ferramenta não é comando - descrever o projeto reprovava a análise")
    void mencionarFerramentaNaoEhComando() {
        assertThat(pareceComando("O projeto usa gradle como build.")).isFalse();
        assertThat(pareceComando("A API pode ser testada com curl.")).isFalse();
        assertThat(pareceComando("Executar os testes do projeto (Gradle).")).isFalse();
    }

    @Test
    @DisplayName("Comando de verdade continua sendo barrado")
    void comandoDeVerdadeContinuaBarrado() {
        assertThat(pareceComando("gradle test --tests CarrinhoTest")).isTrue();
        assertThat(pareceComando("$ curl -X POST http://localhost")).isTrue();
        assertThat(pareceComando("  chmod 777 /etc")).isTrue();
        assertThat(pareceComando("texto antes\ndocker run imagem")).isTrue();
    }
}
