package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaCreateExecutionRequest;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CenarioSalvoResolver - Testes Unitários")
class CenarioSalvoResolverTest {

    @Mock
    private CenarioRepository cenarioRepository;

    private CenarioSalvoResolver resolver;

    @BeforeEach
    void setUp() {
        // Builder real: é formatação pura e determinística, e mocká-lo
        // esconderia justamente o que este resolver entrega.
        resolver = new CenarioSalvoResolver(cenarioRepository, new CenarioSalvoTextoBuilder());
    }

    private Cenario cenarioComItens() {
        CenarioItem item = new CenarioItem();
        item.setNome("Tentar adicionar produto com estoque zero");
        item.setObjetivo("Impedir adição sem estoque");
        item.setPrecondicao("Produto com estoque 0");
        item.setScriptTeste("Dado que o produto está sem estoque\nQuando tento adicionar ao carrinho\nEntão recebo erro");
        item.setResultadoEsperado("Mensagem de erro clara");
        item.setZephyrTestCaseKey("SCRUM-T202");

        Cenario cenario = new Cenario();
        cenario.setId("68a1f0c2db2c9947d6eae6ab");
        cenario.setTitulo("Adicionar produto ao carrinho de compras");
        cenario.setRegraDeNegocio("O usuário deve conseguir adicionar produto disponível em estoque.");
        cenario.setCriteriosAceitacao("Produto sem estoque não pode ser adicionado.");
        cenario.setCenarios(List.of(item));
        return cenario;
    }

    @Test
    @DisplayName("Com cenarioId, deve montar o texto a partir do cenário salvo")
    void comCenarioIdDeveMontarTextoDoCenarioSalvo() {
        when(cenarioRepository.findById("68a1f0c2db2c9947d6eae6ab")).thenReturn(Optional.of(cenarioComItens()));

        String texto = resolver.resolverTexto(
                new AutoQaCreateExecutionRequest(null, "68a1f0c2db2c9947d6eae6ab", "/projeto"));

        assertThat(texto)
                .contains("Adicionar produto ao carrinho de compras")
                .contains("O usuário deve conseguir adicionar produto disponível em estoque.")
                .contains("Produto sem estoque não pode ser adicionado.")
                .contains("Tentar adicionar produto com estoque zero")
                .contains("Dado que o produto está sem estoque")
                .contains("SCRUM-T202");
    }

    @Test
    @DisplayName("Com scenario em texto, não deve consultar o repositório")
    void comScenarioTextoNaoDeveConsultarRepositorio() {
        String texto = resolver.resolverTexto(
                new AutoQaCreateExecutionRequest("meu cenário avulso", "/projeto"));

        assertThat(texto).isEqualTo("meu cenário avulso");
        verify(cenarioRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Informar os dois deve falhar - não há como saber qual vale")
    void informarOsDoisDeveFalhar() {
        assertThatThrownBy(() -> resolver.resolverTexto(
                new AutoQaCreateExecutionRequest("texto", "algum-id", "/projeto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não os dois");
    }

    @Test
    @DisplayName("Sem nenhum dos dois deve falhar dizendo o que informar")
    void semNenhumDosDoisDeveFalhar() {
        assertThatThrownBy(() -> resolver.resolverTexto(
                new AutoQaCreateExecutionRequest(null, null, "/projeto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cenarioId");
    }

    @Test
    @DisplayName("cenarioId inexistente deve falhar identificando o id")
    void cenarioIdInexistenteDeveFalhar() {
        when(cenarioRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolverTexto(
                new AutoQaCreateExecutionRequest(null, "nao-existe", "/projeto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    @DisplayName("Cenário salvo sem conteúdo deve falhar em vez de mandar texto vazio ao agente")
    void cenarioSemConteudoDeveFalhar() {
        when(cenarioRepository.findById("vazio")).thenReturn(Optional.of(new Cenario()));

        assertThatThrownBy(() -> resolver.resolverTexto(
                new AutoQaCreateExecutionRequest(null, "vazio", "/projeto")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não tem conteúdo");
    }

    @Test
    @DisplayName("Deve mandar todos os itens num texto só, não um por execução")
    void deveMandarTodosOsItensNumTextoSo() {
        Cenario cenario = cenarioComItens();
        CenarioItem segundo = new CenarioItem();
        segundo.setNome("Adicionar produto disponível ao carrinho");
        segundo.setScriptTeste("Dado que há estoque\nQuando adiciono\nEntão o item entra no carrinho");
        cenario.setCenarios(List.of(cenario.getCenarios().get(0), segundo));
        when(cenarioRepository.findById("id")).thenReturn(Optional.of(cenario));

        String texto = resolver.resolverTexto(new AutoQaCreateExecutionRequest(null, "id", "/projeto"));

        assertThat(texto)
                .contains("Cenários de teste a automatizar (2)")
                .contains("Tentar adicionar produto com estoque zero")
                .contains("Adicionar produto disponível ao carrinho");
    }
}
