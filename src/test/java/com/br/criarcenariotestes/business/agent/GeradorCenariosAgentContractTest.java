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

    @Test
    @DisplayName("FASE15-BUG-005: deve instruir a não afirmar comportamento sem suporte documental como requisito obrigatório")
    void deveProibirAfirmarComportamentoSemSuporteComoObrigatorio() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo)
                .as("contrato deve mencionar explicitamente a exigência de suporte documental/fonte para afirmações categóricas")
                .containsIgnoringCase("REVIEW_REQUIRED");
        assertThat(conteudo.toLowerCase())
                .containsAnyOf("suporte documental", "sem fonte", "não documentado", "não estiver documentad");
    }

    @Test
    @DisplayName("FASE15-BUG-005: deve definir como classificar um cenário exploratório sem inventar novo campo/schema")
    void deveDefinirClassificacaoDeCenarioExploratorioReusandoCamposExistentes() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo.toLowerCase())
                .as("deve instruir o uso de Status/Rótulos existentes para marcar cenário exploratório")
                .contains("exploratório");
        assertThat(conteudo).contains("Status:");
        assertThat(conteudo).contains("Rótulos:");
    }

    @Test
    @DisplayName("FASE15-BUG-005: deve continuar permitindo inferência lógica direta como cenário normal (não reduzir criatividade)")
    void deveContinuarPermitindoInferenciaLogicaDireta() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo.toLowerCase())
                .as("contrato não deve proibir inferência lógica defensável, só afirmações sem nenhum suporte")
                .containsAnyOf("inferência lógica", "inferencia logica");
    }

    @Test
    @DisplayName("FASE15-BUG-005A: deve distinguir explicitamente O QUE validar de QUANDO/COMO a validação ocorre")
    void deveDistinguirOQueValidarDeQuandoValidar() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo.toLowerCase())
                .as("contrato deve nomear explicitamente a distinção O QUE vs QUANDO/COMO")
                .contains("o que")
                .containsAnyOf("quando/como", "quando ou como", "quando e como");
    }

    @Test
    @DisplayName("FASE15-BUG-005A: deve listar exemplos de timing/interação que não podem ser inferidos sem fonte")
    void deveListarExemplosDeTimingNaoInferivel() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);
        String lower = conteudo.toLowerCase();

        assertThat(lower).containsAnyOf("tempo real", "imediatamente");
        assertThat(lower).containsAnyOf("onblur", "ao sair do campo");
        assertThat(lower).containsAnyOf("onchange", "durante a digitação", "durante o preenchimento");
    }

    @Test
    @DisplayName("FASE15-BUG-005B: deve incluir os campos Evidência e Fontes no formato padrão de cenário")
    void deveIncluirCamposEvidenciaEFontesNoFormatoPadrao() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).contains("Evidência:");
        assertThat(conteudo).contains("Fontes:");
    }

    @Test
    @DisplayName("FASE15-BUG-005B: deve definir os três valores de evidenceType (DOCUMENTED/DIRECT_INFERENCE/EXPLORATORY)")
    void deveDefinirTresValoresDeEvidenceType() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).contains("DOCUMENTED");
        assertThat(conteudo).contains("DIRECT_INFERENCE");
        assertThat(conteudo).contains("EXPLORATORY");
    }

    @Test
    @DisplayName("FASE15-BUG-005B: deve instruir a citar a fonte real do documento (ID de regra) e usar USER para a regra digitada")
    void deveInstruirCitarFonteRealOuUser() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);

        assertThat(conteudo).contains("USER");
        assertThat(conteudo.toLowerCase())
                .as("contrato deve proibir inventar IDs de regra que não existem no documento fonte")
                .containsAnyOf("não invente", "nunca invente", "não inventar", "nunca inventar");
    }

    @Test
    @DisplayName("FASE15-BUG-005B: deve trazer um exemplo de fonte existente mas semanticamente irrelevante (cenário EXPLORATORY mesmo citando fonte real)")
    void deveTrazerExemploDeFonteExistenteMasIrrelevante() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);
        String lower = conteudo.toLowerCase();

        assertThat(lower)
                .as("contrato deve deixar claro que citar uma fonte real não basta — ela precisa sustentar semanticamente o comportamento afirmado")
                .containsAnyOf("não sustenta", "não sustentam", "não justifica", "semanticamente");
    }

    @Test
    @DisplayName("FASE15-BUG-005B: cenário misto deve assumir a menor certeza entre suas partes (nunca aprovação parcial)")
    void deveDefinirRegraDeMenorCertezaVenceParaCenarioMisto() throws IOException {
        String conteudo = Files.readString(AGENT_FILE);
        String lower = conteudo.toLowerCase();

        assertThat(lower)
                .as("contrato deve nomear explicitamente a regra de menor certeza vence para cenários mistos")
                .containsAnyOf("menor certeza", "cenário misto", "cenario misto");
    }
}
