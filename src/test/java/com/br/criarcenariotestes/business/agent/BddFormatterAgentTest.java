package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BddFormatterAgentTest {

    private BddFormatterAgent agent;
    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        agent = new BddFormatterAgent();
        
        CenarioRequest request = new CenarioRequest(
            "Teste Login",
            "Usuário deve conseguir fazer login",
            "gemini",
            WorkflowType.COMPLETO
        );
        context = new WorkflowContext(request);
    }

    @Test
    void deveRetornarNomeCorreto() {
        assertEquals("BDD Formatter", agent.getNome());
    }

    @Test
    void deveEstarHabilitadoPorPadrao() {
        assertTrue(agent.isEnabled(context));
    }

    @Test
    void deveSepararPassosEResultadosCorretamente() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Login válido");
        item.setScriptTeste("""
            Dado que o usuário acessa a página de login
            E preenche o campo usuário com 'teste@email.com'
            Quando clica no botão Login
            """);
        item.setResultadoEsperado("""
            Então o usuário é autenticado com sucesso
            E é redirecionado para a área restrita
            """);
        
        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);
        
        // Act
        agent.executar(context);
        
        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        
        assertNotNull(resultado.getScriptTeste());
        assertNotNull(resultado.getResultadoEsperado());
        
        String passos = resultado.getScriptTeste();
        String resultados = resultado.getResultadoEsperado();
        
        // Passos devem conter apenas Dado, E, Quando
        assertTrue(passos.contains("Dado que") || passos.contains("Dado"));
        assertTrue(passos.contains("Quando"));
        assertFalse(passos.contains("Então"), "Passos não devem conter 'Então'");
        
        // Resultados devem conter apenas Então, E
        assertTrue(resultados.contains("Então"));
        assertFalse(resultados.contains("Quando"), "Resultados não devem conter 'Quando'");
        assertFalse(resultados.contains("Dado"), "Resultados não devem conter 'Dado'");
    }

    @Test
    void deveManterTextoQuandoJaFormatadoCorretamente() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Login inválido");
        item.setScriptTeste("Dado que usuário insere senha incorreta\nQuando tenta fazer login");
        item.setResultadoEsperado("Então sistema exibe mensagem de erro");
        
        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);
        
        String scriptOriginal = item.getScriptTeste();
        String resultadoOriginal = item.getResultadoEsperado();
        
        // Act
        agent.executar(context);
        
        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        
        assertNotNull(resultado.getScriptTeste());
        assertNotNull(resultado.getResultadoEsperado());
        assertTrue(resultado.getScriptTeste().contains("Dado"));
        assertTrue(resultado.getResultadoEsperado().contains("Então"));
    }

    @Test
    void deveProcessarTextoMisturadoCorretamente() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário misto");
        item.setScriptTeste("""
            Dado que usuário está na página de login
            E preenche credenciais válidas
            Quando clica em Login
            Então sistema valida as credenciais
            E redireciona para dashboard
            """);
        item.setResultadoEsperado(""); // vazio, tudo está no scriptTeste
        
        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);
        
        // Act
        agent.executar(context);
        
        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        
        String passos = resultado.getScriptTeste();
        String resultados = resultado.getResultadoEsperado();
        
        // Passos devem ter Dado e Quando
        assertTrue(passos.contains("Dado"));
        assertTrue(passos.contains("Quando"));
        
        // Resultados devem ter o Então
        assertTrue(resultados.contains("Então"));
        assertTrue(resultados.contains("valida") || resultados.contains("redireciona"));
    }

    @Test
    void deveProcessarCenariosRevisadosQuandoPresentes() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário revisado");
        item.setScriptTeste("Dado que usuário acessa sistema Quando faz login");
        item.setResultadoEsperado("Então sistema autentica E exibe dashboard");
        
        List<CenarioItem> cenariosRevisados = new ArrayList<>();
        cenariosRevisados.add(item);
        context.setCenariosRevisados(cenariosRevisados);
        
        // Act
        agent.executar(context);
        
        // Assert
        CenarioItem resultado = context.getCenariosRevisados().get(0);
        
        assertNotNull(resultado.getScriptTeste());
        assertNotNull(resultado.getResultadoEsperado());
        assertTrue(resultado.getScriptTeste().contains("Dado"));
        assertTrue(resultado.getResultadoEsperado().contains("Então"));
    }

    @Test
    void naoDeveFalharComCenariosVazios() {
        // Arrange
        context.setCenarios(new ArrayList<>());
        
        // Act & Assert
        assertDoesNotThrow(() -> agent.executar(context));
    }

    @Test
    void naoDeveFalharComCenariosNulos() {
        // Arrange
        context.setCenarios(null);
        
        // Act & Assert
        assertDoesNotThrow(() -> agent.executar(context));
    }

    @Test
    void deveSepararEntaoQuandoBddVemComoProsaCorridaSeparadaPorPonto() {
        // Arrange - FASE15-BUG-003: reproduz o padrão real observado na Fase 15,
        // em que a IA escreve os passos como prosa corrida separada por ". "
        // em vez de uma linha por palavra-chave (mesmo padrão visto nos passos
        // numerados "1. Acessar... 2. Informar..." antes desta correção).
        CenarioItem item = new CenarioItem();
        item.setNome("Login válido");
        item.setScriptTeste(
                "Dado que o usuário está na tela de login. "
                        + "Quando ele informa credenciais válidas. "
                        + "Então o login é realizado com sucesso.");
        item.setResultadoEsperado("");

        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);

        // Act
        agent.executar(context);

        // Assert
        CenarioItem resultado = context.getCenarios().get(0);

        assertTrue(resultado.getScriptTeste().contains("Dado"));
        assertTrue(resultado.getScriptTeste().contains("Quando"));
        assertFalse(resultado.getScriptTeste().contains("Então"),
                "Passos não devem conter 'Então' mesmo quando a IA escreve tudo em prosa corrida");
        assertNotNull(resultado.getResultadoEsperado());
        assertTrue(resultado.getResultadoEsperado().contains("Então"),
                "Resultado Esperado deve conter o trecho 'Então' extraído da prosa corrida");
    }

    @Test
    void deveManterCamposVaziosQuandoSemTextoBdd() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário sem BDD");
        item.setScriptTeste("Apenas texto simples sem keywords");
        item.setResultadoEsperado("Outro texto simples");
        
        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);
        
        // Act
        agent.executar(context);
        
        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        
        assertNotNull(resultado.getScriptTeste());
        assertNotNull(resultado.getResultadoEsperado());
    }
}
