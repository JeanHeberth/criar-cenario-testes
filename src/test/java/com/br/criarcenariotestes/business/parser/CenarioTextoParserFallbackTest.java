package com.br.criarcenariotestes.business.parser;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CenarioTextoParserFallbackTest {

    private final CenarioTextoParser parser = new CenarioTextoParser();

    @Test
    void deveExtrairCamposDeTextoCorrido() {
        // Arrange - Formato similar ao retornado pela IA
        String textoCorrido = """
            ---
            Nome: Geração de login com dados válidos, únicos e diferentes combinações
            Objetivo: Verificar se o sistema permite a geração de login com dados válidos, únicos e diferentes combinações de campos obrigatórios, conforme a política de autenticação.
            Pré-condições: Usuário não possui login previamente cadastrado; sistema disponível.
            Passos:
            1. Informar os campos obrigatórios usando parâmetros:
                - e-mail: {email_válido} (ex: institucional, comercial)
                - senha: {senha_forte} (com diferentes variações válidas, incluindo caracteres especiais)
                - nome de usuário: {usuario_unico} (com letras, números, no mínimo 3 e no máximo 30 caracteres)
            2. Submeter a solicitação de criação de login.
            Resultado esperado: Login gerado com sucesso; mensagem de confirmação exibida; usuário pode acessar a aplicação.
            Tipo: Positivo
            Prioridade: P0
            Tags: [fluxo principal, funcional, unicidade, variação]
            ---
            """;

        // Act
        List<CenarioItem> cenarios = parser.parsear(textoCorrido);

        // Assert
        assertEquals(1, cenarios.size(), "Deve extrair 1 cenário");
        
        CenarioItem item = cenarios.get(0);
        
        // Verificar nome
        assertNotNull(item.getNome(), "Nome não deve ser nulo");
        assertTrue(item.getNome().contains("Geração de login"), "Nome deve conter 'Geração de login'");
        
        // Verificar objetivo
        assertNotNull(item.getObjetivo(), "Objetivo não deve ser nulo");
        assertTrue(item.getObjetivo().contains("Verificar se o sistema"), "Objetivo deve estar extraído");
        
        // Verificar pré-condições
        assertNotNull(item.getPrecondicao(), "Pré-condições não devem ser nulas");
        assertTrue(item.getPrecondicao().contains("Usuário não possui login"), "Pré-condições devem estar extraídas");
        
        // Verificar passos
        assertNotNull(item.getScriptTeste(), "Script de teste não deve ser nulo");
        assertTrue(item.getScriptTeste().contains("Informar os campos"), "Passos devem estar extraídos");
        
        // Verificar resultado esperado
        assertNotNull(item.getResultadoEsperado(), "Resultado esperado não deve ser nulo");
        assertTrue(item.getResultadoEsperado().contains("Login gerado com sucesso"), "Resultado esperado deve estar extraído");
    }

    @Test
    void deveExtrairCamposQuandoTudoEstaNoCampoNome() {
        // Arrange - Cenário onde tudo está no campo "Nome"
        String textoComTudoNoNome = """
            ---
            Nome: Validação de campos obrigatórios
            Objetivo: Garantir validação correta
            Pré-condições: Sistema disponível
            Passos:
            1. Acessar página de login
            2. Submeter formulário vazio
            Resultado esperado: Sistema exibe mensagem de erro
            ---
            """;

        // Act
        List<CenarioItem> cenarios = parser.parsear(textoComTudoNoNome);

        // Assert
        assertEquals(1, cenarios.size());
        
        CenarioItem item = cenarios.get(0);
        
        assertNotNull(item.getNome());
        assertNotNull(item.getObjetivo());
        assertNotNull(item.getPrecondicao());
        assertNotNull(item.getScriptTeste());
        assertNotNull(item.getResultadoEsperado());
    }

    @Test
    void deveSuportarFormatoOriginalComCamposSeparados() {
        // Arrange - Formato original esperado
        String formatoOriginal = """
            ---
            Nome: Login válido
            Objetivo: Validar login
            Pré-condições: Usuário cadastrado
            Passos:
            1. Acessar sistema
            2. Fazer login
            Resultado esperado: Login realizado com sucesso
            ---
            """;

        // Act
        List<CenarioItem> cenarios = parser.parsear(formatoOriginal);

        // Assert
        assertEquals(1, cenarios.size());
        
        CenarioItem item = cenarios.get(0);
        
        assertEquals("Login válido", item.getNome());
        assertEquals("Validar login", item.getObjetivo());
        assertTrue(item.getPrecondicao().contains("Usuário cadastrado"));
        assertTrue(item.getScriptTeste().contains("Acessar sistema"));
        assertTrue(item.getResultadoEsperado().contains("Login realizado"));
    }

    @Test
    void deveRetornarListaVaziaParaTextoNulo() {
        // Act
        List<CenarioItem> cenarios = parser.parsear(null);

        // Assert
        assertTrue(cenarios.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaParaTextoVazio() {
        // Act
        List<CenarioItem> cenarios = parser.parsear("");

        // Assert
        assertTrue(cenarios.isEmpty());
    }

    @Test
    void deveExtrairCamposDoFormatoRealDoAgenteGeradorDeCenarios() {
        // Arrange - formato REAL produzido pelo agente ativo em produção
        // (agents/gerador_cenarios_testes.agent.md): cada campo em bullet "- "
        // e vocabulário "Título"/"Massa de dados" em vez de "Nome"/"Variáveis".
        // Reproduz literalmente o bug FASE15-BUG-001 (Fase 15, teste manual
        // end-to-end): campos vazando um no outro e conteúdo duplicado.
        String formatoReal = """
            ---
            Cenário 1: Login bem-sucedido de usuário ativo sem autenticação em dois fatores
            - Objetivo: Validar que um usuário ativo, sem 2FA habilitado, consegue acessar o sistema normalmente.
            - Pré-condições: Usuário está cadastrado, ativo, sem 2FA habilitado.
            - Massa de dados: E-mail válido; senha correta.
            - Passos:
              1. Acessar a tela de login.
              2. Informar e-mail e senha corretos.
              3. Clicar em "Entrar".
            - Resultado esperado: Usuário é autenticado e redirecionado para a página inicial da aplicação.
            - Tipo: Positivo
            - Prioridade: P0
            - Tags: fluxo-principal, login, sem-2FA
            ---
            Cenário 2: Tentativa de login com e-mail inexistente
            - Objetivo: Verificar que o sistema não revela se o e-mail existe e exibe mensagem genérica.
            - Pré-condições: E-mail informado não está cadastrado no sistema.
            - Massa de dados: E-mail inexistente; senha qualquer.
            - Passos:
              1. Acessar a tela de login.
              2. Informar e-mail não cadastrado e qualquer senha.
              3. Clicar em "Entrar".
            - Resultado esperado: Mensagem genérica de credenciais inválidas sem revelar existência do e-mail.
            - Tipo: Negativo
            - Prioridade: P0
            - Tags: negativo, seguranca, mensagem-generica
            ---
            """;

        List<CenarioItem> cenarios = parser.parsear(formatoReal);

        assertEquals(2, cenarios.size(), "Deve extrair os 2 cenários, sem misturar um no outro");

        CenarioItem c1 = cenarios.get(0);

        assertEquals(
                "Login bem-sucedido de usuário ativo sem autenticação em dois fatores",
                c1.getNome(),
                "Nome (via alias 'Título') não deve vazar o resto do bloco"
        );
        assertEquals(
                "Validar que um usuário ativo, sem 2FA habilitado, consegue acessar o sistema normalmente.",
                c1.getObjetivo(),
                "Objetivo não deve vazar Pré-condições/Massa de dados/Passos/etc."
        );
        assertEquals(
                "Usuário está cadastrado, ativo, sem 2FA habilitado.",
                c1.getPrecondicao(),
                "Pré-condições não deve vazar Massa de dados/Passos/etc."
        );
        assertEquals(
                "E-mail válido; senha correta.",
                c1.getVariaveis(),
                "Variáveis deve ser populado a partir do alias 'Massa de dados', não cair no fallback 'Não se aplica'"
        );
        assertFalse(
                c1.getScriptTeste().contains("Resultado esperado"),
                "Script de Teste (Passos) não deve vazar o Resultado Esperado"
        );
        assertFalse(
                c1.getScriptTeste().toLowerCase().contains("tipo:") || c1.getScriptTeste().toLowerCase().contains("prioridade:"),
                "Script de Teste (Passos) não deve vazar metadados (Tipo/Prioridade/Tags)"
        );
        assertEquals(
                "Usuário é autenticado e redirecionado para a página inicial da aplicação.",
                c1.getResultadoEsperado(),
                "Resultado Esperado não deve incluir Tipo/Prioridade/Tags nem duplicar o próprio texto"
        );

        CenarioItem c2 = cenarios.get(1);
        assertEquals("Tentativa de login com e-mail inexistente", c2.getNome());
        assertEquals("E-mail inexistente; senha qualquer.", c2.getVariaveis());
        assertEquals(
                "Mensagem genérica de credenciais inválidas sem revelar existência do e-mail.",
                c2.getResultadoEsperado()
        );
    }

    @Test
    void deveProcessarMultiplosCenariosNoMesmoTexto() {
        // Arrange
        String multiplos = """
            ---
            Nome: Cenário 1
            Objetivo: Objetivo 1
            Passos:
            1. Passo 1
            Resultado esperado: Resultado 1
            ---
            ---
            Nome: Cenário 2
            Objetivo: Objetivo 2
            Passos:
            1. Passo 2
            Resultado esperado: Resultado 2
            ---
            """;

        // Act
        List<CenarioItem> cenarios = parser.parsear(multiplos);

        // Assert
        assertEquals(2, cenarios.size(), "Deve extrair 2 cenários");
        assertEquals("Cenário 1", cenarios.get(0).getNome());
        assertEquals("Cenário 2", cenarios.get(1).getNome());
    }

    @Test
    void naoDeveTransformarBlocoDeObservacoesFinaisDoReviewerEmCenario() {
        // FASE15-BUG-003A (Fase 15, Sessão 5): reproduz o padrão real observado
        // em produção — o Reviewer às vezes anexa um bloco de observações/notas
        // finais depois dos cenários reais. Esse bloco não tem nenhum campo
        // real preenchido, mas o parser aceitava-o como cenário porque o
        // fallback de "primeira linha" preenchia `nome` ANTES do guard de
        // bloco vazio ser avaliado (nome deixava de estar em branco e o guard
        // `if (nome.isBlank() && objetivo.isBlank() && ...)` nunca disparava).
        String respostaComObservacoesFinais = """
                ---
                Nome: Login válido
                Objetivo: Validar login
                Passos:
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                Então o login é realizado com sucesso
                Resultado esperado: Login realizado
                ---
                Observações de otimização:
                Consolidamos os passos e resultado esperado dos cenários acima removendo redundâncias, mantendo a cobertura original.
                """;

        List<CenarioItem> cenarios = parser.parsear(respostaComObservacoesFinais);

        assertEquals(1, cenarios.size(), "O bloco de observações finais não deve virar um cenário");
        assertEquals("Login válido", cenarios.get(0).getNome());
    }
}
