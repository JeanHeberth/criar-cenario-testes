package com.br.criarcenariotestes.business.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptFactory - Testes Unitários")
class PromptFactoryTest {

    private static final String TITULO = "Saque em conta corrente";
    private static final String REGRA = "O saque só pode ocorrer com saldo disponível";
    private static final String CONTEXTO = "Reunião de refinamento definiu limite diário de 5000";

    @Test
    @DisplayName("Deve retornar system prompt não vazio")
    void deveRetornarSystemPromptNaoVazio() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt).isNotBlank();
    }

    @Test
    @DisplayName("Deve incluir as seções obrigatórias no system prompt")
    void deveIncluirSecoesObrigatoriasNoSystemPrompt() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt)
            .contains("# CONTEXTO")
            .contains("# OBJETIVO")
            .contains("# REGRAS OBRIGATÓRIAS")
            .contains("# SAÍDA ESPERADA")
            .contains("# ESTILO")
            .contains("# FORMATO DE SAÍDA");
    }

    @Test
    @DisplayName("Deve exigir cobertura mínima de cenários no system prompt")
    void deveExigirCoberturaMinimaDeCenarios() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt)
            .contains("MÍNIMO OBRIGATÓRIO: 6-10 cenários")
            .containsIgnoringCase("Cobertura Mínima Esperada");
    }

    @Test
    @DisplayName("Deve exigir os campos do formato Zephyr no system prompt")
    void deveExigirCamposDoFormatoZephyr() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt)
            .contains("Nome:")
            .contains("Objetivo:")
            .contains("Precondição:")
            .contains("Script de Teste (Passo-a-Passo):")
            .contains("Script de Teste (Passo-a-Passo) - Resultado:")
            .contains("Variáveis:")
            .contains("Componente:")
            .contains("Rótulos:")
            .contains("Propósito:")
            .contains("Pasta:")
            .contains("Cobertura:")
            .contains("Status: Aguardando execução");
    }

    @Test
    @DisplayName("Deve definir o proprietário padrão dos cenários")
    void deveDefinirProprietarioPadrao() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt).contains("Proprietário: JIRAUSER23105");
    }

    @Test
    @DisplayName("Deve instruir BDD com Dado, Quando e Então")
    void deveInstruirBddComDadoQuandoEntao() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt)
            .contains("## BDD (OBRIGATÓRIO)")
            .contains("Usar: Dado, Quando, Então.");
    }

    @Test
    @DisplayName("Deve instruir o uso de variáveis no formato do Zephyr")
    void deveInstruirUsoDeVariaveisDoZephyr() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt)
            .contains("## VARIÁVEIS DO ZEPHYR")
            .contains("<nomeVariavel>")
            .contains("Não se aplica");
    }

    @Test
    @DisplayName("Deve proibir resposta em JSON no system prompt")
    void deveProibirRespostaEmJson() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt).contains("Não use JSON.");
    }

    @Test
    @DisplayName("Deve exigir separador de blocos entre cenários")
    void deveExigirSeparadorDeBlocosEntreCenarios() {
        String systemPrompt = PromptFactory.getSystemPrompt();
        assertThat(systemPrompt).contains("Cada bloco deve ser separado por três hífens: ---");
    }

    @Test
    @DisplayName("Deve retornar o mesmo system prompt em chamadas consecutivas")
    void deveRetornarMesmoSystemPromptEmChamadasConsecutivas() {
        assertThat(PromptFactory.getSystemPrompt()).isEqualTo(PromptFactory.getSystemPrompt());
    }

    @Test
    @DisplayName("Deve montar user prompt com título e regra de negócio")
    void deveMontarUserPromptComTituloERegra() {
        String userPrompt = PromptFactory.getUserPrompt(TITULO, REGRA);
        assertThat(userPrompt)
            .contains("Título da Feature (Tema): " + TITULO)
            .contains("Regra de Negócio (Critérios de Aceite): " + REGRA);
    }

    @Test
    @DisplayName("Deve montar user prompt sem bloco de contexto adicional")
    void deveMontarUserPromptSemBlocoDeContexto() {
        String userPrompt = PromptFactory.getUserPrompt(TITULO, REGRA);
        assertThat(userPrompt).doesNotContain("CONTEXTO ADICIONAL");
    }

    @Test
    @DisplayName("Deve montar user prompt com valores nulos sem lançar exceção")
    void deveMontarUserPromptComValoresNulos() {
        String userPrompt = PromptFactory.getUserPrompt(null, null);
        assertThat(userPrompt)
            .contains("Título da Feature (Tema): null")
            .contains("Regra de Negócio (Critérios de Aceite): null");
    }

    @Test
    @DisplayName("Deve montar user prompt com contexto incluindo título, regra e contexto")
    void deveMontarUserPromptComContexto() {
        String userPrompt = PromptFactory.getUserPromptComContexto(TITULO, REGRA, CONTEXTO);
        assertThat(userPrompt)
            .contains("Título da Feature (Tema): " + TITULO)
            .contains("Regra de Negócio (Critérios de Aceite): " + REGRA)
            .contains(CONTEXTO);
    }

    @Test
    @DisplayName("Deve incluir bloco e instruções de contexto adicional")
    void deveIncluirBlocoEInstrucoesDeContextoAdicional() {
        String userPrompt = PromptFactory.getUserPromptComContexto(TITULO, REGRA, CONTEXTO);
        assertThat(userPrompt)
            .contains("CONTEXTO ADICIONAL")
            .contains("INSTRUÇÕES PARA USAR O CONTEXTO:")
            .contains("Não copie a transcrição inteira na resposta.");
    }

    @Test
    @DisplayName("Deve posicionar o contexto adicional após a regra de negócio")
    void devePosicionarContextoAposRegraDeNegocio() {
        String userPrompt = PromptFactory.getUserPromptComContexto(TITULO, REGRA, CONTEXTO);
        assertThat(userPrompt.indexOf(CONTEXTO)).isGreaterThan(userPrompt.indexOf(REGRA));
    }

    @Test
    @DisplayName("Deve preservar caracteres especiais do contexto sem interpretá-los como formato")
    void devePreservarCaracteresEspeciaisDoContexto() {
        String contextoComPercentual = "Cobrança de 100% do valor em caso de %s inadimplência";
        String userPrompt = PromptFactory.getUserPromptComContexto(TITULO, REGRA, contextoComPercentual);
        assertThat(userPrompt).contains(contextoComPercentual);
    }

    @Test
    @DisplayName("Deve montar user prompt com contexto vazio sem lançar exceção")
    void deveMontarUserPromptComContextoVazio() {
        String userPrompt = PromptFactory.getUserPromptComContexto(TITULO, REGRA, "");
        assertThat(userPrompt)
            .contains("CONTEXTO ADICIONAL")
            .contains("Título da Feature (Tema): " + TITULO);
    }

    @Test
    @DisplayName("Deve gerar user prompt com contexto maior que o user prompt simples")
    void deveGerarUserPromptComContextoMaiorQueOSimples() {
        String simples = PromptFactory.getUserPrompt(TITULO, REGRA);
        String comContexto = PromptFactory.getUserPromptComContexto(TITULO, REGRA, CONTEXTO);
        assertThat(comContexto.length()).isGreaterThan(simples.length());
    }
}