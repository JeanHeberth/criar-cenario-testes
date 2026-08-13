package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedundancyReviewAgent - Testes Unitários")
class RedundancyReviewAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private CenarioTextoParser cenarioTextoParser;

    private RedundancyReviewAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        agent = new RedundancyReviewAgent(aiProviderResolver, cenarioTextoParser, new GeneratedScenariosValidator());

        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request, WorkflowType.COMPLETO);

        CenarioItem cenario1 = new CenarioItem();
        cenario1.setNome("Login com email válido");
        cenario1.setScriptTeste("1. Informar email\n2. Clicar em entrar");

        CenarioItem cenario2 = new CenarioItem();
        cenario2.setNome("Login com e-mail válido");
        cenario2.setScriptTeste("1. Digitar e-mail\n2. Clicar em login");

        context.setCenarios(List.of(cenario1, cenario2));
    }

    @Test
    @DisplayName("Deve revisar e remover redundâncias com sucesso")
    void deveRevisarERemoverRedundancias() {
        // Arrange
        CenarioItem cenarioOtimizado = new CenarioItem();
        cenarioOtimizado.setNome("Login com credenciais válidas [email/senha]");
        cenarioOtimizado.setScriptTeste(
                "Dado que o usuário está na tela de login\n"
                        + "Quando ele informa e-mail e senha válidos\n"
                        + "Então o login é realizado com sucesso");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn("---\nNome: CT001\n---");
        when(aiProvider.getName()).thenReturn("Gemini");
        when(cenarioTextoParser.parsear(anyString())).thenReturn(List.of(cenarioOtimizado));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isNotNull();
        assertThat(context.getCenariosRevisados()).hasSize(1);
        assertThat(context.getMetadata("revisao_provider")).isEqualTo("Gemini");
        assertThat(context.getMetadata("cenarios_originais")).isEqualTo(2);
        assertThat(context.getMetadata("cenarios_revisados")).isEqualTo(1);

        verify(aiProviderResolver, times(1)).getActiveProvider();
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Redundancy Reviewer");
    }

    @Test
    @DisplayName("Deve ser habilitado no workflow COMPLETO")
    void deveSerHabilitadoNoWorkflowCompleto() {
        // Arrange
        context.setWorkflowType(WorkflowType.COMPLETO);

        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve ser habilitado no workflow REVISAO")
    void deveSerHabilitadoNoWorkflowRevisao() {
        // Arrange
        context.setWorkflowType(WorkflowType.REVISAO);

        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve ser desabilitado no workflow RAPIDO")
    void deveSerDesabilitadoNoWorkflowRapido() {
        // Arrange
        context.setWorkflowType(WorkflowType.RAPIDO);

        // Assert
        assertThat(agent.isEnabled(context)).isFalse();
    }

    @Test
    @DisplayName("Deve pular revisão se não houver cenários")
    void devePularRevisaoSeNaoHouverCenarios() {
        // Arrange
        context.setCenarios(new ArrayList<>());

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isEmpty();
        verify(aiProviderResolver, never()).getActiveProvider();
    }

    @Test
    @DisplayName("Deve manter cenários originais em caso de erro")
    void deveManterCenariosOriginaisEmCasoDeErro() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro"));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isEqualTo(context.getCenarios());
    }

    @Test
    @DisplayName("FASE15-BUG-002: deve manter cenários originais (fail closed) quando a revisão retornar um plano de arquivos")
    void deveManterCenariosOriginaisQuandoRevisaoRetornarPlanoDeArquivos() {
        // Arrange
        String planoDeGeracao = """
                📋 Plano de Geração
                - Pasta base: `x/`
                - Arquivos a criar:
                  - `a.md`
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(planoDeGeracao);

        // Act
        agent.executar(context);

        // Assert: nunca deve inventar cenários a partir de um plano mal-formado.
        assertThat(context.getCenariosRevisados()).isEqualTo(context.getCenarios());
        verify(cenarioTextoParser, never()).parsear(anyString());
    }

    @Test
    @DisplayName("FASE15-BUG-002: deve manter cenários originais (fail closed) quando a revisão não extrair nenhum cenário")
    void deveManterCenariosOriginaisQuandoRevisaoNaoExtrairNenhumCenario() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn("---\nNome: CT001\n---");
        when(cenarioTextoParser.parsear(anyString())).thenReturn(List.of());

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isEqualTo(context.getCenarios());
    }

    @Test
    @DisplayName("FASE15-BUG-003: estrutura Dado/Quando/Então deve sobreviver à revisão")
    void estruturaBddDevePermanecerAposRevisao() {
        // Arrange - entrada em BDD válido
        CenarioItem cenarioBdd = new CenarioItem();
        cenarioBdd.setNome("Login com credenciais válidas");
        cenarioBdd.setScriptTeste(
                "Dado que o usuário está na tela de login\n"
                        + "Quando ele informa credenciais válidas\n"
                        + "Então o login é realizado com sucesso");
        context.setCenarios(List.of(cenarioBdd));

        String respostaRevisada = "---\nNome: Login com credenciais válidas\nPassos:\n"
                + "Dado que o usuário está na tela de login\n"
                + "Quando ele informa credenciais válidas\n"
                + "Então o login é realizado com sucesso\n---";

        CenarioItem cenarioRevisado = new CenarioItem();
        cenarioRevisado.setNome("Login com credenciais válidas");
        cenarioRevisado.setScriptTeste(
                "Dado que o usuário está na tela de login\n"
                        + "Quando ele informa credenciais válidas\n"
                        + "Então o login é realizado com sucesso");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaRevisada);
        when(cenarioTextoParser.parsear(respostaRevisada)).thenReturn(List.of(cenarioRevisado));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).hasSize(1);
        assertThat(context.getCenariosRevisados().get(0).getScriptTeste())
                .contains("Dado", "Quando", "Então");
    }

    @Test
    @DisplayName("FASE15-BUG-003: scriptTeste não deve incorporar o texto do Resultado Esperado ao final")
    void scriptTesteNaoDeveIncorporarResultadoEsperado() {
        // Arrange - resposta bem-formada do reviewer, com Passos e Resultado Esperado
        // em campos separados (formato real esperado do agente)
        String respostaRevisada = """
                ---
                Nome: Login com credenciais válidas
                Passos:
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                Então o login é processado
                Resultado esperado: Usuário autenticado e redirecionado para a página inicial
                ---
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaRevisada);

        // usa o parser real só para este teste, para provar que campos bem separados
        // na resposta não vazam um no outro após a revisão
        CenarioTextoParser parserReal = new CenarioTextoParser();
        RedundancyReviewAgent agentComParserReal =
                new RedundancyReviewAgent(aiProviderResolver, parserReal, new GeneratedScenariosValidator());

        // Act
        agentComParserReal.executar(context);

        // Assert
        CenarioItem revisado = context.getCenariosRevisados().get(0);
        assertThat(revisado.getScriptTeste())
                .as("Passos não deve conter o texto do Resultado Esperado colado ao final")
                .doesNotContain("Usuário autenticado e redirecionado para a página inicial");
        assertThat(revisado.getResultadoEsperado())
                .isEqualTo("Usuário autenticado e redirecionado para a página inicial");
    }

    // ===== FASE15-BUG-003A: validação pós-Reviewer =====

    @Test
    @DisplayName("FASE15-BUG-003A: item editorial pós-revisão (ex.: 'Observações de otimização') deve ser descartado sem invalidar os cenários reais")
    void deveDescartarItemEditorialSemInvalidarCenariosReais() {
        // Arrange
        CenarioItem cenarioValido1 = new CenarioItem();
        cenarioValido1.setNome("Login com credenciais válidas");
        cenarioValido1.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado");

        CenarioItem cenarioValido2 = new CenarioItem();
        cenarioValido2.setNome("Login com e-mail inexistente");
        cenarioValido2.setScriptTeste("Dado que o e-mail não existe\nQuando o usuário tenta logar\nEntão a mensagem genérica é exibida");

        CenarioItem itemEditorial = new CenarioItem();
        itemEditorial.setNome("Observações de otimização:");
        itemEditorial.setScriptTeste("");
        itemEditorial.setResultadoEsperado("");

        String respostaRevisada = "---\n(resposta com 2 cenários válidos + observações finais)\n---";

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaRevisada);
        when(cenarioTextoParser.parsear(respostaRevisada))
                .thenReturn(List.of(cenarioValido1, cenarioValido2, itemEditorial));

        // Act
        agent.executar(context);

        // Assert - só os 2 cenários reais permanecem; nada foi fail-closed
        assertThat(context.getCenariosRevisados()).hasSize(2);
        assertThat(context.getCenariosRevisados())
                .extracting(CenarioItem::getNome)
                .containsExactly("Login com credenciais válidas", "Login com e-mail inexistente");
    }

    @Test
    @DisplayName("FASE15-BUG-003A: cenário real estruturalmente corrompido (com conteúdo, mas sem Quando) deve fail closed a revisão inteira")
    void deveFailClosedQuandoCenarioRealVierCorrompido() {
        // Arrange
        CenarioItem cenarioValido = new CenarioItem();
        cenarioValido.setNome("Login com credenciais válidas");
        cenarioValido.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado");

        CenarioItem cenarioCorrompido = new CenarioItem();
        cenarioCorrompido.setNome("Login bloqueado");
        // tem conteúdo real, mas falta "Quando" -> não é conteúdo editorial, é um
        // cenário real corrompido
        cenarioCorrompido.setScriptTeste("Dado usuário com três tentativas inválidas\nEntão a conta é bloqueada");

        String respostaRevisada = "---\n(resposta com 1 cenário válido + 1 corrompido)\n---";

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaRevisada);
        when(cenarioTextoParser.parsear(respostaRevisada))
                .thenReturn(List.of(cenarioValido, cenarioCorrompido));

        // Act
        agent.executar(context);

        // Assert - fail closed: mantém os cenários ORIGINAIS (pré-revisão), não os revisados
        assertThat(context.getCenariosRevisados()).isEqualTo(context.getCenarios());
    }
}
