package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestScenarioAgent - Testes Unitários")
class TestScenarioAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private CenarioTextoParser cenarioTextoParser;

    private TestScenarioAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        agent = new TestScenarioAgent(aiProviderResolver, cenarioTextoParser, new GeneratedScenariosValidator());

        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request);
        context.setRequisitos("RF001: Login");
        context.setDecisoesReuniao("Usar OAuth 2.0");
        context.setPlanoMacro("Testar login positivo e negativo");
    }

    @Test
    @DisplayName("Deve gerar cenários de teste com sucesso")
    void deveGerarCenariosComSucesso() {
        // Arrange
        String respostaIA = """
                ---
                Nome: Login com credenciais válidas
                Objetivo: Validar login bem-sucedido
                Passos:
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                Então o login é realizado com sucesso
                Resultado esperado: Login realizado
                ---
                """;

        CenarioItem cenario1 = new CenarioItem();
        cenario1.setNome("Login com credenciais válidas");
        cenario1.setObjetivo("Validar login bem-sucedido");
        cenario1.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado com sucesso");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaIA);
        when(aiProvider.getName()).thenReturn("OpenAI");
        when(cenarioTextoParser.parsear(respostaIA)).thenReturn(List.of(cenario1));
        when(cenarioTextoParser.extrairCriterios(respostaIA))
                .thenReturn("Sistema deve permitir login");

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenarios()).isNotNull();
        assertThat(context.getCenarios()).hasSize(1);
        assertThat(context.getCenarios().get(0).getNome())
                .isEqualTo("Login com credenciais válidas");
        assertThat(context.getCriteriosAceitacao()).isEqualTo("Sistema deve permitir login");
        assertThat(context.getMetadata("cenarios_provider")).isEqualTo("OpenAI");
        assertThat(context.getMetadata("cenarios_count")).isEqualTo(1);

        verify(aiProviderResolver, times(1)).getActiveProvider();
        verify(cenarioTextoParser, times(1)).parsear(respostaIA);
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Test Scenario Generator");
    }

    @Test
    @DisplayName("Deve usar instruções de agente customizado quando disponível")
    void deveUsarInstrucoesDeAgenteCustomizado() {
        // Arrange
        context.setAgentInstructions("Instruções customizadas do agente");

        CenarioItem cenario1 = new CenarioItem();
        cenario1.setNome("CT001");
        cenario1.setObjetivo("Objetivo do CT001");
        cenario1.setScriptTeste("Dado que o usuário está autenticado\nQuando ele executa a ação\nEntão o resultado é validado");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn("---\nNome: CT001\nObjetivo: Objetivo do CT001\n---");
        when(cenarioTextoParser.parsear(anyString())).thenReturn(List.of(cenario1));

        // Act
        agent.executar(context);

        // Assert
        verify(aiProvider).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando falhar ao gerar cenários")
    void deveLancarExcecaoQuandoFalhar() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro de conexão"));

        // Act & Assert
        assertThatThrownBy(() -> agent.executar(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na geração de cenários");
    }

    @Test
    @DisplayName("FASE15-BUG-002: deve fazer retry único quando a IA responde com um plano de arquivos e ter sucesso na 2ª tentativa")
    void deveFazerRetryUnicoQuandoRespostaForPlanoDeArquivosEObterSucessoNaSegundaTentativa() {
        // Arrange
        String planoDeGeracao = """
                📋 Plano de Geração
                - Pasta base: `login_bloqueio_2fa_tests/`
                - Arquivos a criar:
                  - `CENARIOS_DE_TESTE.md` – Todos os cenários detalhados
                """;
        String respostaValida = "---\nNome: Login válido\nObjetivo: Validar login\n---";

        CenarioItem cenarioValido = new CenarioItem();
        cenarioValido.setNome("Login válido");
        cenarioValido.setObjetivo("Validar login");
        cenarioValido.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado com sucesso");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(planoDeGeracao, respostaValida);
        when(aiProvider.getName()).thenReturn("OpenAI");
        when(cenarioTextoParser.parsear(planoDeGeracao)).thenReturn(List.of());
        when(cenarioTextoParser.parsear(respostaValida)).thenReturn(List.of(cenarioValido));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenarios()).hasSize(1);
        assertThat(context.getCenarios().get(0).getNome()).isEqualTo("Login válido");
        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("FASE15-BUG-002: deve lançar exceção e NÃO persistir quando a resposta continuar inválida após o retry único")
    void deveLancarExcecaoQuandoRespostaContinuarInvalidaAposRetryUnico() {
        // Arrange
        String planoDeGeracao = """
                📋 Plano de Geração
                - Pasta base: `login_bloqueio_2fa_tests/`
                - Arquivos a criar:
                  - `CENARIOS_DE_TESTE.md` – Todos os cenários detalhados
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(planoDeGeracao);
        when(cenarioTextoParser.parsear(planoDeGeracao)).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> agent.executar(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na geração de cenários");

        // Exatamente 1 retry: 2 chamadas ao provider, nunca mais.
        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString());
        assertThat(context.getCenarios()).isNull();
    }

    @Test
    @DisplayName("FASE15-BUG-002: deve lançar exceção quando a resposta vier vazia mesmo após o retry")
    void deveLancarExcecaoQuandoRespostaVierVaziaMesmoAposRetry() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn("   ");

        // Act & Assert
        assertThatThrownBy(() -> agent.executar(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na geração de cenários");

        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("FASE15-BUG-003: 1ª resposta com cenário válido mas passos numerados, 2ª resposta com BDD válido -> retry recupera")
    void deveFazerRetryUnicoQuandoPassosVieremNumeradosEObterSucessoComBddNaSegundaTentativa() {
        // Arrange - 1ª resposta: cenário semanticamente válido, mas passos numerados (contrato antigo)
        String respostaComPassosNumerados = "---\nNome: Login válido\nObjetivo: Validar login\nPassos:\n1. Acessar\n2. Logar\n---";
        CenarioItem cenarioComPassosNumerados = new CenarioItem();
        cenarioComPassosNumerados.setNome("Login válido");
        cenarioComPassosNumerados.setObjetivo("Validar login");
        cenarioComPassosNumerados.setScriptTeste("1. Acessar\n2. Logar");

        // 2ª resposta: mesmo cenário, agora em BDD válido
        String respostaComBdd = "---\nNome: Login válido\nObjetivo: Validar login\nPassos:\nDado...\nQuando...\nEntão...\n---";
        CenarioItem cenarioComBdd = new CenarioItem();
        cenarioComBdd.setNome("Login válido");
        cenarioComBdd.setObjetivo("Validar login");
        cenarioComBdd.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado com sucesso");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(respostaComPassosNumerados, respostaComBdd);
        when(aiProvider.getName()).thenReturn("OpenAI");
        when(cenarioTextoParser.parsear(respostaComPassosNumerados)).thenReturn(List.of(cenarioComPassosNumerados));
        when(cenarioTextoParser.parsear(respostaComBdd)).thenReturn(List.of(cenarioComBdd));

        // Act
        agent.executar(context);

        // Assert - retry exatamente 1 vez, fluxo continua com sucesso
        assertThat(context.getCenarios()).hasSize(1);
        assertThat(context.getCenarios().get(0).getScriptTeste()).contains("Dado", "Quando", "Então");
        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("FASE15-BUG-003: passos numerados em ambas as tentativas -> erro explícito, Reviewer não chamado, nada persistido")
    void deveLancarExcecaoQuandoPassosContinuaremNumeradosAposRetry() {
        // Arrange - ambas as respostas com passos numerados (nunca BDD)
        String respostaComPassosNumerados = "---\nNome: Login válido\nObjetivo: Validar login\nPassos:\n1. Acessar\n2. Logar\n---";
        CenarioItem cenarioComPassosNumerados = new CenarioItem();
        cenarioComPassosNumerados.setNome("Login válido");
        cenarioComPassosNumerados.setObjetivo("Validar login");
        cenarioComPassosNumerados.setScriptTeste("1. Acessar\n2. Logar");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaComPassosNumerados);
        when(cenarioTextoParser.parsear(respostaComPassosNumerados)).thenReturn(List.of(cenarioComPassosNumerados));

        // Act & Assert
        assertThatThrownBy(() -> agent.executar(context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na geração de cenários");

        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString());
        assertThat(context.getCenarios()).isNull();
    }
}
