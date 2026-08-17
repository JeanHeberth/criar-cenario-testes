package com.br.criarcenariotestes.business.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReferenciaTarefaParser - Testes Unitários")
class ReferenciaTarefaParserTest {

    private final ReferenciaTarefaParser parser = new ReferenciaTarefaParser();

    @Test
    @DisplayName("Deve resolver URL do Jira colada do navegador")
    void deveResolverUrlJira() {
        ReferenciaTarefa ref = parser.parsear("https://jeanheberth.atlassian.net/browse/SCRUM-28").orElseThrow();

        assertEquals(ProvedorTarefa.JIRA, ref.provedor());
        assertEquals("SCRUM", ref.projeto());
        assertEquals("SCRUM-28", ref.identificador());
        assertNull(ref.organizacao(), "Jira não tem organização");
    }

    @Test
    @DisplayName("Deve resolver URL do Jira com issue selecionada no board")
    void deveResolverUrlJiraComIssueSelecionada() {
        ReferenciaTarefa ref = parser.parsear(
                "https://jeanheberth.atlassian.net/jira/software/projects/SCRUM/boards/1?selectedIssue=SCRUM-28")
                .orElseThrow();

        assertEquals(ProvedorTarefa.JIRA, ref.provedor());
        assertEquals("SCRUM-28", ref.identificador());
    }

    @Test
    @DisplayName("Deve aceitar a chave pura do Jira, que era o formato aceito antes")
    void deveAceitarChavePura() {
        ReferenciaTarefa ref = parser.parsear("SCRUM-28").orElseThrow();

        assertEquals(ProvedorTarefa.JIRA, ref.provedor());
        assertEquals("SCRUM", ref.projeto());
        assertEquals("SCRUM-28", ref.identificador());
    }

    @Test
    @DisplayName("Deve normalizar a chave do Jira para maiúscula")
    void deveNormalizarChaveParaMaiuscula() {
        assertEquals("SCRUM-28", parser.parsear("scrum-28").orElseThrow().identificador());
        assertEquals("SCRUM", parser.parsear("scrum-28").orElseThrow().projeto());
    }

    @Test
    @DisplayName("Deve resolver URL do Azure DevOps, extraindo organização e projeto que o id sozinho não carrega")
    void deveResolverUrlAzure() {
        ReferenciaTarefa ref = parser.parsear(
                "https://dev.azure.com/minhaOrg/MeuProjeto/_workitems/edit/1234").orElseThrow();

        assertEquals(ProvedorTarefa.AZURE_DEVOPS, ref.provedor());
        assertEquals("minhaOrg", ref.organizacao());
        assertEquals("MeuProjeto", ref.projeto());
        assertEquals("1234", ref.identificador());
    }

    @Test
    @DisplayName("Deve resolver URL do Azure no formato legado visualstudio.com")
    void deveResolverUrlAzureLegado() {
        ReferenciaTarefa ref = parser.parsear(
                "https://minhaOrg.visualstudio.com/MeuProjeto/_workitems/edit/1234").orElseThrow();

        assertEquals(ProvedorTarefa.AZURE_DEVOPS, ref.provedor());
        assertEquals("minhaOrg", ref.organizacao());
        assertEquals("MeuProjeto", ref.projeto());
    }

    @Test
    @DisplayName("Deve decodificar projeto do Azure com espaço no nome")
    void deveDecodificarProjetoComEspaco() {
        ReferenciaTarefa ref = parser.parsear(
                "https://dev.azure.com/org/Meu%20Projeto/_workitems/edit/99").orElseThrow();

        assertEquals("Meu Projeto", ref.projeto());
    }

    @Test
    @DisplayName("Campo não informado é opcional e não deve virar erro")
    void campoVazioDeveRetornarOptionalVazio() {
        assertEquals(Optional.empty(), parser.parsear(null));
        assertEquals(Optional.empty(), parser.parsear(""));
        assertEquals(Optional.empty(), parser.parsear("   "));
    }

    @Test
    @DisplayName("Id numérico solto deve falhar explicando que não identifica a tarefa por completo")
    void idNumericoSoltoDeveFalharComMensagemUtil() {
        ReferenciaTarefaInvalidaException erro = assertThrows(ReferenciaTarefaInvalidaException.class,
                () -> parser.parsear("1234"));

        assertTrue(erro.getMessage().contains("dev.azure.com"),
                "Mensagem deve mostrar o formato esperado: " + erro.getMessage());
    }

    @Test
    @DisplayName("Texto irreconhecível deve falhar em vez de publicar sem o vínculo pedido em silêncio")
    void textoInvalidoDeveFalhar() {
        assertThrows(ReferenciaTarefaInvalidaException.class, () -> parser.parsear("qualquer coisa"));
        assertThrows(ReferenciaTarefaInvalidaException.class, () -> parser.parsear("https://exemplo.com/algo"));
    }

    @Test
    @DisplayName("Deve preservar a entrada original para rastreabilidade")
    void devePreservarEntradaOriginal() {
        String url = "https://jeanheberth.atlassian.net/browse/SCRUM-28";
        assertEquals(url, parser.parsear(url).orElseThrow().entradaOriginal());
    }
}
