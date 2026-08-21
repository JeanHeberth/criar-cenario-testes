package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.DestinoPublicacaoResponse;
import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.business.tracker.FolderStrategyResolver;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefaParser;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DestinoPublicacaoService - Testes Unitários")
class DestinoPublicacaoServiceTest {

    @Mock
    private JiraClient jiraClient;

    private ZephyrProperties zephyrProperties;
    private DestinoPublicacaoService service;

    @BeforeEach
    void setUp() {
        zephyrProperties = new ZephyrProperties();
        zephyrProperties.setProjectKey("PADRAO");
        zephyrProperties.setRootFolder("RaizPadrao");
        service = new DestinoPublicacaoService(
                new ReferenciaTarefaParser(),
                new FolderStrategyResolver(zephyrProperties, jiraClient),
                zephyrProperties);
    }

    @Test
    @DisplayName("Deve resolver o destino a partir da URL do Jira, sem gerar nada")
    void deveResolverDestinoDaUrlJira() {
        DestinoPublicacaoResponse destino =
                service.resolver("https://empresa.atlassian.net/browse/PAY-77", null, null);

        assertThat(destino.valido()).isTrue();
        assertThat(destino.provedor()).isEqualTo("JIRA");
        assertThat(destino.identificador()).isEqualTo("PAY-77");
        assertThat(destino.projectKey()).isEqualTo("PAY");
    }

    @Test
    @DisplayName("Deve devolver o identificador normalizado - é o que evita o front reimplementar o parsing")
    void deveDevolverIdentificadorNormalizado() {
        DestinoPublicacaoResponse destino =
                service.resolver("https://empresa.atlassian.net/browse/scrum-28", null, null);

        assertThat(destino.identificador()).isEqualTo("SCRUM-28");
    }

    @Test
    @DisplayName("Referência inválida deve devolver 'valido=false' com o motivo, em vez de estourar")
    void referenciaInvalidaDeveDevolverMotivo() {
        DestinoPublicacaoResponse destino = service.resolver("não é tarefa", null, null);

        assertThat(destino.valido()).isFalse();
        assertThat(destino.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("Sem tarefa informada, deve mostrar os defaults do ambiente")
    void semTarefaDeveMostrarDefaults() {
        DestinoPublicacaoResponse destino = service.resolver(null, null, null);

        assertThat(destino.valido()).isTrue();
        assertThat(destino.identificador()).isNull();
        assertThat(destino.projectKey()).isEqualTo("PADRAO");
        assertThat(destino.pastaRaiz()).isEqualTo("RaizPadrao");
    }

    @Test
    @DisplayName("Deve refletir a pasta derivada pela folder-strategy")
    void deveRefletirPastaDerivada() {
        zephyrProperties.getFolderStrategy().setEnabled(true);
        zephyrProperties.getFolderStrategy().setMapping(Map.of("postman", "Postman"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(new DadosDaIssue("SCRUM-28", "10027", "x", List.of("Postman"), List.of()));

        DestinoPublicacaoResponse destino =
                service.resolver("https://empresa.atlassian.net/browse/SCRUM-28", null, null);

        assertThat(destino.pastaRaiz()).isEqualTo("Postman");
    }

    @Test
    @DisplayName("Preview deve honrar a mesma precedência da publicação - explícito ganha do derivado")
    void previewDeveHonrarPrecedenciaDaPublicacao() {
        // Um preview que divergisse do comportamento real seria pior que
        // não ter preview.
        zephyrProperties.getFolderStrategy().setEnabled(true);
        zephyrProperties.getFolderStrategy().setMapping(Map.of("postman", "Postman"));
        when(jiraClient.buscarDadosDaIssue("SCRUM-28"))
                .thenReturn(new DadosDaIssue("SCRUM-28", "10027", "x", List.of("Postman"), List.of()));

        DestinoPublicacaoResponse destino =
                service.resolver("https://empresa.atlassian.net/browse/SCRUM-28", "Java", "QA");

        assertThat(destino.pastaRaiz()).isEqualTo("Java");
        assertThat(destino.projectKey()).isEqualTo("QA");
    }

    @Test
    @DisplayName("Referência do Azure não deriva projeto - cai no default do ambiente")
    void referenciaAzureCaiNoDefault() {
        DestinoPublicacaoResponse destino = service.resolver(
                "https://dev.azure.com/org/Projeto/_workitems/edit/1234", null, null);

        assertThat(destino.valido()).isTrue();
        assertThat(destino.provedor()).isEqualTo("AZURE_DEVOPS");
        assertThat(destino.identificador()).isEqualTo("1234");
        assertThat(destino.projectKey()).isEqualTo("PADRAO");
    }
}
