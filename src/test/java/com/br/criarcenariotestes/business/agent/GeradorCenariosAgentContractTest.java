package com.br.criarcenariotestes.business.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE15-BUG-003: garante que o contrato ativo do agente gerador de cenários
 * exige BDD/Gherkin no campo Passos, em vez do antigo formato numerado.
 * Valida apenas invariantes relevantes do contrato (não o texto completo do prompt).
 */
@DisplayName("agents/gerador_cenarios_testes.agent.md - Contrato BDD (FASE15-BUG-003)")
class GeradorCenariosAgentContractTest {

    private static final Path AGENT_FILE = Path.of("agents/gerador_cenarios_testes.agent.md");

    @Test
    @DisplayName("Deve exigir explicitamente as keywords Dado, Quando e Então no contrato de Passos")
    void deveExigirKeywordsBddNoContrato() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).containsIgnoringCase("Dado");
        assertThat(conteudo).containsIgnoringCase("Quando");
        assertThat(conteudo).containsIgnoringCase("Então");
    }

    @Test
    @DisplayName("Não deve mais apresentar o template de passos numerados (1. [passo] / 2. [passo]) como contrato de Passos")
    void naoDeveMaisApresentarTemplateDePassosNumerados() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).doesNotContain("1. [passo]");
        assertThat(conteudo).doesNotContain("2. [passo]");
    }

    @Test
    @DisplayName("Deve continuar suportando a variante 'Dado que' além de 'Dado'")
    void deveSuportarDadoQue() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).containsIgnoringCase("Dado que");
    }

    @Test
    @DisplayName("Deve exigir mínimo de 1 Dado/Dado que, 1 Quando e 1 Então por cenário")
    void deveExigirMinimoDeUmaOcorrenciaDeCadaKeyword() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo.toLowerCase())
                .as("contrato deve reforçar a exigência mínima de estrutura BDD")
                .containsAnyOf("mínimo", "obrigatório", "obrigatória");
    }
}
