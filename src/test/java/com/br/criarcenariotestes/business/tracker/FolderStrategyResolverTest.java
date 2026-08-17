package com.br.criarcenariotestes.business.tracker;

import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.infrastructure.jira.DadosDaIssue;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FolderStrategyResolver - Testes Unitários")
class FolderStrategyResolverTest {

    @Mock
    private JiraClient jiraClient;

    private ZephyrProperties zephyrProperties;
    private FolderStrategyResolver resolver;

    private final ReferenciaTarefa refJira = new ReferenciaTarefa(
            ProvedorTarefa.JIRA, null, "SCRUM", "SCRUM-28",
            "https://jeanheberth.atlassian.net/browse/SCRUM-28");

    @BeforeEach
    void setUp() {
        zephyrProperties = new ZephyrProperties();
        zephyrProperties.getFolderStrategy().setEnabled(true);
        zephyrProperties.getFolderStrategy().setMapping(Map.of(
                "postman", "Postman",
                "java", "Java",
                "robot", "Robot"
        ));
        resolver = new FolderStrategyResolver(zephyrProperties, jiraClient);
    }

    private DadosDaIssue issue(String summary, List<String> componentes, List<String> labels) {
        return new DadosDaIssue("SCRUM-28", "10027", summary, componentes, labels);
    }

    @Test
    @DisplayName("Desligada por padrão - não deve nem consultar o Jira")
    void desligadaNaoDeveConsultarJira() {
        zephyrProperties.getFolderStrategy().setEnabled(false);

        assertThat(resolver.resolverPastaRaiz(refJira)).isNull();
        verify(jiraClient, never()).buscarDadosDaIssue(any());
    }

    @Test
    @DisplayName("Deve derivar a pasta do componente da issue - a fonte natural num Jira corporativo")
    void deveDerivarDoComponente() {
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("Automacao do POST Usuario", List.of("Postman"), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isEqualTo("Postman");
    }

    @Test
    @DisplayName("Deve respeitar a ordem das fontes configuradas")
    void deveRespeitarOrdemDasFontes() {
        zephyrProperties.getFolderStrategy().setSources(List.of("labels", "components"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("qualquer", List.of("Java"), List.of("robot")));

        assertThat(resolver.resolverPastaRaiz(refJira)).isEqualTo("Robot");
    }

    @Test
    @DisplayName("Deve casar ignorando maiúsculas e acentos")
    void deveCasarIgnorandoCaixaEAcento() {
        zephyrProperties.getFolderStrategy().setMapping(Map.of("automação", "Automacao"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("x", List.of("AUTOMACAO"), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isEqualTo("Automacao");
    }

    @Test
    @DisplayName("Valor fora do mapa não deve virar pasta nova - cai no fallback")
    void valorForaDoMapaNaoDeveVirarPasta() {
        // O mapa é fechado de propósito: é o que separa isto de deixar a IA
        // adivinhar. Pasta no Zephyr não tem DELETE via API.
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("x", List.of("Cypress"), List.of("k6")));

        assertThat(resolver.resolverPastaRaiz(refJira)).isNull();
    }

    @Test
    @DisplayName("Com 'summary' nas fontes, deve achar o termo no título - degrau para Jira sem campo estruturado")
    void deveAcharTermoNoSummary() {
        zephyrProperties.getFolderStrategy().setSources(List.of("components", "labels", "summary"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("Automacao POSTMAN do POST Usuario", List.of(), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isEqualTo("Postman");
    }

    @Test
    @DisplayName("No summary deve casar palavra inteira - 'java' não pode casar com 'javascript'")
    void summaryDeveCasarPalavraInteira() {
        // Sem isso o caso iria para a pasta errada, e no Zephyr isso é permanente.
        zephyrProperties.getFolderStrategy().setSources(List.of("summary"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("Automacao javascript do front", List.of(), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isNull();
    }

    @Test
    @DisplayName("Sem campo estruturado e sem 'summary' configurado, não deve derivar nada")
    void semFonteUtilNaoDeveDerivar() {
        // É o caso do Jira de teste hoje: components e labels vazios.
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("Automacao POSTMAN do POST Usuario", List.of(), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isNull();
    }

    @Test
    @DisplayName("Referência do Azure não deve consultar o Jira")
    void referenciaAzureNaoDeveConsultarJira() {
        ReferenciaTarefa refAzure = new ReferenciaTarefa(
                ProvedorTarefa.AZURE_DEVOPS, "org", "Projeto", "1234", "url");

        assertThat(resolver.resolverPastaRaiz(refAzure)).isNull();
        verify(jiraClient, never()).buscarDadosDaIssue(any());
    }

    @Test
    @DisplayName("Jira indisponível não deve derrubar a publicação - só perde a pasta derivada")
    void jiraIndisponivelNaoDeveDerrubar() {
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenThrow(new IllegalStateException("Jira fora do ar"));

        assertThat(resolver.resolverPastaRaiz(refJira)).isNull();
    }

    @Test
    @DisplayName("Fonte desconhecida na configuração deve ser ignorada, não quebrar")
    void fonteDesconhecidaDeveSerIgnorada() {
        zephyrProperties.getFolderStrategy().setSources(List.of("inexistente", "components"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(issue("x", List.of("Java"), List.of()));

        assertThat(resolver.resolverPastaRaiz(refJira)).isEqualTo("Java");
    }

    @Test
    @DisplayName("Referência ausente não deve consultar o Jira")
    void referenciaNulaNaoDeveConsultarJira() {
        assertThat(resolver.resolverPastaRaiz(null)).isNull();
        verify(jiraClient, never()).buscarDadosDaIssue(any());
    }
}
