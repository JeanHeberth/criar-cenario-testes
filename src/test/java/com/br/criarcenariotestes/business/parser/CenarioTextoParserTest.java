package com.br.criarcenariotestes.business.parser;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CenarioTextoParserTest {

    private final CenarioTextoParser parser = new CenarioTextoParser();

    @Test
    void deveParsearMultiplosBlocosComSeparadorFlexivel() {
        String resposta = """
                Nome: Cenario 1
                Objetivo: Validar fluxo 1
                Precondição: Usuario autenticado
                Script de Teste (Passo-a-Passo): Dado x\nQuando y
                Script de Teste (Passo-a-Passo) - Resultado: Entao ok
                Variáveis: Nao se aplica
                Componente: API
                Rótulos: regressao
                Propósito: Cobertura principal
                Pasta: Funcionalidade
                Cobertura: REG-1
                Status: APPROVED
                  ---
                Nome: Cenario 2
                Objetivo: Validar fluxo 2
                Precondição: Usuario autenticado
                Script de Teste (Passo-a-Passo): Dado x\nQuando y
                Script de Teste (Passo-a-Passo) - Resultado: Entao ok
                Variáveis: Nao se aplica
                Componente: API
                Rótulos: regressao
                Propósito: Cobertura secundaria
                Pasta: Funcionalidade
                Cobertura: REG-2
                Status: APPROVED
                ----
                Nome: Cenario 3
                Objetivo: Validar fluxo 3
                Precondição: Usuario autenticado
                Script de Teste (Passo-a-Passo): Dado x\nQuando y
                Script de Teste (Passo-a-Passo) - Resultado: Entao ok
                Variáveis: Nao se aplica
                Componente: API
                Rótulos: regressao
                Propósito: Cobertura adicional
                Pasta: Funcionalidade
                Cobertura: REG-3
                Status: APPROVED
                """;

        List<CenarioItem> itens = parser.parsear(resposta);

        assertEquals(3, itens.size());
        assertEquals("Cenario 1", itens.get(0).getNome());
        assertEquals("Cenario 3", itens.get(2).getNome());
    }

    @Test
    void deveAceitarCampoNomeCaseInsensitive() {
        String resposta = """
                nome: Cenario minusculo
                Objetivo: Validar parse
                Precondição: Ambiente pronto
                Script de Teste (Passo-a-Passo): Dado a\nQuando b
                Script de Teste (Passo-a-Passo) - Resultado: Entao sucesso
                Variáveis: Nao se aplica
                Componente: API
                Rótulos: regressao
                Propósito: validar parser
                Pasta: Funcionalidade
                Cobertura: REG-10
                Status: APPROVED
                """;

        List<CenarioItem> itens = parser.parsear(resposta);

        assertEquals(1, itens.size());
        assertTrue(itens.get(0).getNome().contains("minusculo"));
    }
}

