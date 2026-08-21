package com.br.criarcenariotestes.business.autoqa.model.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextoFlexivelDeserializer - Testes Unitários")
class TextoFlexivelDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ScenarioAnalysisResult ler(String json) throws Exception {
        return mapper.readValue(json, ScenarioAnalysisResult.class);
    }

    @Test
    @DisplayName("Deve ler lista de strings simples - o formato do schema")
    void deveLerListaDeStringsSimples() throws Exception {
        ScenarioAnalysisResult r = ler("""
                {"preconditions": ["usuário logado", "produto em estoque"]}
                """);

        assertThat(r.preconditions()).containsExactly("usuário logado", "produto em estoque");
    }

    @Test
    @DisplayName("Deve extrair o texto quando o modelo promove a string a objeto")
    void deveExtrairTextoDeObjeto() throws Exception {
        // Sem isto, a resposta INTEIRA era descartada no parse e a análise
        // falhava por um detalhe de formato que não muda o conteúdo.
        ScenarioAnalysisResult r = ler("""
                {"preconditions": [{"description": "usuário logado"}, {"descricao": "produto em estoque"}]}
                """);

        assertThat(r.preconditions()).containsExactly("usuário logado", "produto em estoque");
    }

    @Test
    @DisplayName("Deve aceitar as demais chaves de texto que o modelo usa")
    void deveAceitarOutrasChavesDeTexto() throws Exception {
        ScenarioAnalysisResult r = ler("""
                {"entities": [{"name": "Carrinho"}, {"value": "Produto"}, {"texto": "Estoque"}]}
                """);

        assertThat(r.entities()).containsExactly("Carrinho", "Produto", "Estoque");
    }

    @Test
    @DisplayName("Deve tolerar mistura de string e objeto na mesma lista")
    void deveTolerarMistura() throws Exception {
        ScenarioAnalysisResult r = ler("""
                {"warnings": ["direto", {"description": "via objeto"}]}
                """);

        assertThat(r.warnings()).containsExactly("direto", "via objeto");
    }

    @Test
    @DisplayName("Deve converter número e booleano em texto")
    void deveConverterEscalares() throws Exception {
        ScenarioAnalysisResult r = ler("""
                {"dependencies": [42, true]}
                """);

        assertThat(r.dependencies()).containsExactly("42", "true");
    }

    @Test
    @DisplayName("Objeto sem nenhum campo de texto deve continuar sendo rejeitado")
    void objetoSemCampoDeTextoDeveSerRejeitado() {
        // A tolerância é para formato, não para conteúdo ausente: resposta
        // genuinamente malformada continua falhando.
        assertThatThrownBy(() -> ler("""
                {"preconditions": [{"quantidade": 3}]}
                """))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    @DisplayName("Lista com objeto aninhado dentro de steps também deve funcionar")
    void deveFuncionarDentroDeSteps() throws Exception {
        ScenarioAnalysisResult r = ler("""
                {"steps": [{"action": "adicionar", "expectedResult": "ok",
                            "dependencies": [{"description": "produto cadastrado"}]}]}
                """);

        assertThat(r.steps()).hasSize(1);
        assertThat(r.steps().get(0).dependencies()).containsExactly("produto cadastrado");
    }
}
