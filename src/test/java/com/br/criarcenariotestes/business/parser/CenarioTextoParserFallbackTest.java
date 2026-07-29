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
}
