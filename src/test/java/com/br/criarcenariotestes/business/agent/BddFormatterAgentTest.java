package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    void deveQuebrarKeywordAposTabelaMarkdown() {
        // Observado em produção: o modelo escreveu os dados do passo como tabela
        // markdown e a keyword ficou logo após um "|". Sem o pipe como fronteira,
        // "Quando" não ganhava quebra de linha, o validador via Quando=false e
        // reprovava um cenário íntegro — derrubando o workflow inteiro.
        CenarioItem item = new CenarioItem();
        item.setNome("Login rejeitado por credencial inválida");
        item.setScriptTeste("Dado que é enviada uma requisição POST para /auth/login "
                + "E o corpo contém: | \"email\" | \"senha\" | | a@b.com | errada | Quando a requisição é enviada");
        item.setResultadoEsperado("Então a resposta deve ter status 401");

        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);

        agent.executar(context);

        String passos = context.getCenarios().get(0).getScriptTeste();
        assertTrue(passos.matches("(?s).*(^|\\n)[ \\t]*Quando\\b.*"),
                "keyword 'Quando' deve iniciar um passo após a tabela. Recebido:\n" + passos);
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
    void deveRemoverDuplicacaoExataDoResultadoEsperadoNoFinalDosPassos() {
        // Arrange - FASE15-BUG-003A: reproduz o padrão real observado na Sessão 5
        // (cenários 1, 5 e 7 do reteste): a conclusão do "Então" fica corretamente
        // no passo, mas o mesmo texto do Resultado Esperado é colado de novo,
        // sem separador, logo em seguida.
        CenarioItem item = new CenarioItem();
        item.setNome("Tentativa de login com conta inativa");
        item.setScriptTeste(
                "Dado que o usuário está na tela de login\n"
                        + "E possui conta marcada como inativa\n"
                        + "Quando preenche o campo de e-mail e senha corretamente\n"
                        + "E clica em \"Entrar\" Então o sistema exibe uma mensagem específica de conta inativa "
                        + "Login rejeitado e mensagem específica de conta inativa exibida ao usuário.");
        item.setResultadoEsperado("Login rejeitado e mensagem específica de conta inativa exibida ao usuário.");

        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);

        // Act
        agent.executar(context);

        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        String duplicado = "Login rejeitado e mensagem específica de conta inativa exibida ao usuário.";

        // Os Passos não podem conter "Então" — é o contrato do módulo, afirmado
        // também por deveSepararPassosEResultadosCorretamente. A asserção antiga
        // exigia o oposto: ela fixava o comportamento de quando a fronteira de
        // quebra não reconhecia aspas e o "Então" ficava preso nos Passos.
        assertFalse(resultado.getScriptTeste().contains("Então"),
                "Passos não devem conter 'Então'. Recebido:\n" + resultado.getScriptTeste());
        assertTrue(resultado.getScriptTeste().contains("E clica em \"Entrar\""),
                "O passo anterior ao 'Então' deve ser preservado");

        // A frase duplicada pertence ao Resultado, mas só uma vez.
        assertEquals(1, countOcorrencias(resultado.getResultadoEsperado(), duplicado),
                "A frase deve sobrar exatamente uma vez. Recebido:\n" + resultado.getResultadoEsperado());
        assertTrue(resultado.getResultadoEsperado().startsWith("Então o sistema exibe"),
                "O Resultado deve começar pelo 'Então' original");
        assertEquals(0, countOcorrencias(resultado.getScriptTeste(), duplicado),
                "O texto do Resultado não deve aparecer dentro dos Passos");
    }

    @Test
    void naoDeveRemoverTextoQuandoScriptTesteEIgualAoResultadoEsperadoInteiro() {
        // Arrange - caso de borda: não deduplicar se isso apagaria o passo inteiro
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário de borda");
        item.setScriptTeste("Login rejeitado.");
        item.setResultadoEsperado("Login rejeitado.");

        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);

        // Act
        agent.executar(context);

        // Assert
        CenarioItem resultado = context.getCenarios().get(0);
        assertFalse(resultado.getScriptTeste().isBlank());
    }

    private long countOcorrencias(String texto, String trecho) {
        if (texto == null || trecho == null || trecho.isEmpty()) {
            return 0;
        }
        long count = 0;
        int idx = 0;
        while ((idx = texto.indexOf(trecho, idx)) != -1) {
            count++;
            idx += trecho.length();
        }
        return count;
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

    @ParameterizedTest(name = "keyword apos ''{0}''")
    @ValueSource(strings = {
            "E o corpo contém {\"email\": \"\", \"senha\": \"\"}",   // aspas/chave de JSON
            "Dado que os campos estão vazios,",                     // vírgula
            "Dado email e/ou senha ausentes/",                       // a "/" de "e/ou"
            "E os campos estão em branco -",                         // hífen
            "E o corpo contém: | \"email\" | | a@b.com |"            // tabela markdown
    })
    void deveQuebrarKeywordAposQualquerCaractere(String prefixo) {
        // Regressão: a fronteira era uma classe enumerada de caracteres e foi
        // ampliada duas vezes na marra. Cada um destes prefixos derrubava o
        // workflow INTEIRO — um cenário com Quando=false reprova o lote todo.
        CenarioItem item = new CenarioItem();
        item.setNome("Validação de campos obrigatórios vazios");
        item.setScriptTeste(prefixo + " Quando a requisição é enviada");
        item.setResultadoEsperado("Então a resposta deve ter status 400");

        List<CenarioItem> cenarios = new ArrayList<>();
        cenarios.add(item);
        context.setCenarios(cenarios);

        agent.executar(context);

        String passos = context.getCenarios().get(0).getScriptTeste();
        assertTrue(passos.matches("(?s).*(^|\\n)[ \\t]*Quando\\b.*"),
                "keyword 'Quando' deve iniciar um passo. Recebido:\n" + passos);
    }
}
