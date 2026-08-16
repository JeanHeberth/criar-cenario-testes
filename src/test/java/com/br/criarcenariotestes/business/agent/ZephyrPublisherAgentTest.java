package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import com.br.criarcenariotestes.infrastructure.zephyr.ZephyrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ZephyrPublisherAgent - Testes Unitários")
class ZephyrPublisherAgentTest {

    @Mock
    private ZephyrClient zephyrClient;

    @Mock
    private JiraClient jiraClient;

    private ZephyrProperties zephyrProperties;
    private ZephyrPublisherAgent agent;
    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        zephyrProperties = new ZephyrProperties();
        agent = new ZephyrPublisherAgent(zephyrClient, zephyrProperties, jiraClient);

        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request);
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        assertThat(agent.getNome()).isEqualTo("Zephyr Publisher");
    }

    @Test
    @DisplayName("Deve estar desabilitado quando zephyr.enabled=false")
    void deveEstarDesabilitadoQuandoZephyrDesabilitado() {
        zephyrProperties.setEnabled(false);
        assertThat(agent.isEnabled(context)).isFalse();
    }

    @Test
    @DisplayName("Deve estar habilitado quando zephyr.enabled=true")
    void deveEstarHabilitadoQuandoZephyrHabilitado() {
        zephyrProperties.setEnabled(true);
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve publicar cada cenário e gravar a key retornada pelo Zephyr")
    void devePublicarCadaCenarioEGravarKey() {
        // Arrange
        CenarioItem item1 = new CenarioItem();
        item1.setNome("Login com credenciais válidas");
        CenarioItem item2 = new CenarioItem();
        item2.setNome("Login com credenciais inválidas");
        context.setCenarios(List.of(item1, item2));

        when(zephyrClient.criarCasoDeTeste(eq(item1), any())).thenReturn("SCRUM-T1");
        when(zephyrClient.criarCasoDeTeste(eq(item2), any())).thenReturn("SCRUM-T2");

        // Act
        agent.executar(context);

        // Assert
        assertThat(item1.getZephyrTestCaseKey()).isEqualTo("SCRUM-T1");
        assertThat(item2.getZephyrTestCaseKey()).isEqualTo("SCRUM-T2");
        assertThat(context.getMetadata("zephyr_publicados")).isEqualTo(2);
        assertThat(context.getMetadata("zephyr_falhas")).isEqualTo(0);

        verify(zephyrClient, times(1)).criarCasoDeTeste(eq(item1), any());
        verify(zephyrClient, times(1)).criarCasoDeTeste(eq(item2), any());
    }

    @Test
    @DisplayName("Falha ao publicar um cenário não deve impedir a publicação dos demais nem lançar exceção")
    void falhaEmUmCenarioNaoDeveImpedirOsDemais() {
        // Arrange
        CenarioItem item1 = new CenarioItem();
        item1.setNome("Cenário que falha no Zephyr");
        CenarioItem item2 = new CenarioItem();
        item2.setNome("Cenário que publica com sucesso");
        context.setCenarios(List.of(item1, item2));

        when(zephyrClient.criarCasoDeTeste(eq(item1), any())).thenThrow(new IllegalStateException("Zephyr indisponível"));
        when(zephyrClient.criarCasoDeTeste(eq(item2), any())).thenReturn("SCRUM-T2");

        // Act
        agent.executar(context);

        // Assert - não lança, item1 fica sem key, item2 publica normalmente
        assertThat(item1.getZephyrTestCaseKey()).isNull();
        assertThat(item2.getZephyrTestCaseKey()).isEqualTo("SCRUM-T2");
        assertThat(context.getMetadata("zephyr_publicados")).isEqualTo(1);
        assertThat(context.getMetadata("zephyr_falhas")).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve preferir cenariosRevisados quando disponível, igual aos demais formatadores")
    void devePreferirCenariosRevisados() {
        // Arrange
        CenarioItem original = new CenarioItem();
        original.setNome("Versão original");
        CenarioItem revisado = new CenarioItem();
        revisado.setNome("Versão revisada");

        context.setCenarios(List.of(original));
        context.setCenariosRevisados(List.of(revisado));

        when(zephyrClient.criarCasoDeTeste(eq(revisado), any())).thenReturn("SCRUM-T9");

        // Act
        agent.executar(context);

        // Assert
        verify(zephyrClient, times(1)).criarCasoDeTeste(eq(revisado), any());
        verify(zephyrClient, never()).criarCasoDeTeste(eq(original), any());
        assertThat(revisado.getZephyrTestCaseKey()).isEqualTo("SCRUM-T9");
    }

    @Test
    @DisplayName("Não deve lançar exceção quando não houver cenários para publicar")
    void naoDeveLancarExcecaoQuandoNaoHouverCenarios() {
        // Act & Assert (não deve lançar)
        agent.executar(context);

        verify(zephyrClient, never()).criarCasoDeTeste(any(), any());
        verify(zephyrClient, never()).resolverOuCriarFolder(any());
    }

    @Test
    @DisplayName("Deve usar CenarioItem#pasta como nome de pasta quando a IA especificou uma")
    void deveUsarPastaDoItemQuandoEspecificada() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setPasta("Autenticação/Login");
        context.setCenarios(List.of(item));

        when(zephyrClient.resolverOuCriarFolder("Autenticação/Login")).thenReturn(42L);
        when(zephyrClient.criarCasoDeTeste(eq(item), eq(42L))).thenReturn("SCRUM-T1");

        // Act
        agent.executar(context);

        // Assert
        verify(zephyrClient, times(1)).resolverOuCriarFolder("Autenticação/Login");
        verify(zephyrClient, times(1)).criarCasoDeTeste(item, 42L);
        assertThat(item.getZephyrTestCaseKey()).isEqualTo("SCRUM-T1");
    }

    @Test
    @DisplayName("Deve cair no título do pedido como nome de pasta quando o item não especifica pasta")
    void deveUsarTituloDoPedidoQuandoItemSemPasta() {
        // Arrange - request.titulo() = "Login OAuth" (definido no setUp)
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário sem pasta");
        context.setCenarios(List.of(item));

        when(zephyrClient.resolverOuCriarFolder("Login OAuth")).thenReturn(7L);
        when(zephyrClient.criarCasoDeTeste(eq(item), eq(7L))).thenReturn("SCRUM-T5");

        // Act
        agent.executar(context);

        // Assert
        verify(zephyrClient, times(1)).resolverOuCriarFolder("Login OAuth");
        assertThat(item.getZephyrTestCaseKey()).isEqualTo("SCRUM-T5");
    }

    @Test
    @DisplayName("Deve resolver a pasta uma única vez e reaproveitar para todos os itens com o mesmo nome")
    void deveResolverPastaUmaUnicaVezParaMesmoNome() {
        // Arrange - nenhum item define pasta -> ambos caem no título "Login OAuth"
        CenarioItem item1 = new CenarioItem();
        item1.setNome("Cenário 1");
        CenarioItem item2 = new CenarioItem();
        item2.setNome("Cenário 2");
        context.setCenarios(List.of(item1, item2));

        when(zephyrClient.resolverOuCriarFolder("Login OAuth")).thenReturn(99L);
        when(zephyrClient.criarCasoDeTeste(any(), eq(99L))).thenReturn("SCRUM-T1", "SCRUM-T2");

        // Act
        agent.executar(context);

        // Assert - resolverOuCriarFolder chamado só 1 vez, não 2 (cache local)
        verify(zephyrClient, times(1)).resolverOuCriarFolder("Login OAuth");
        verify(zephyrClient, times(2)).criarCasoDeTeste(any(), eq(99L));
    }

    @Test
    @DisplayName("Falha ao resolver/criar pasta não deve impedir a publicação do item - cai para sem pasta")
    void falhaAoResolverPastaNaoDeveImpedirPublicacao() {
        // Arrange
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário qualquer");
        context.setCenarios(List.of(item));

        when(zephyrClient.resolverOuCriarFolder("Login OAuth"))
                .thenThrow(new IllegalStateException("Zephyr indisponível para pastas"));
        when(zephyrClient.criarCasoDeTeste(eq(item), eq(null))).thenReturn("SCRUM-T1");

        // Act
        agent.executar(context);

        // Assert - segue publicando sem pasta (folderId null)
        verify(zephyrClient, times(1)).criarCasoDeTeste(item, null);
        assertThat(item.getZephyrTestCaseKey()).isEqualTo("SCRUM-T1");
        assertThat(context.getMetadata("zephyr_falhas")).isEqualTo(0);
    }

    // ===== Vínculo opcional com issue do Jira =====

    @Test
    @DisplayName("Não deve consultar o Jira quando o pedido não informa jiraIssueKey")
    void naoDeveConsultarJiraQuandoSemIssueKey() {
        // Arrange - context do setUp já usa o construtor de 3 args (jiraIssueKey=null)
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário sem issue");
        context.setCenarios(List.of(item));

        when(zephyrClient.criarCasoDeTeste(eq(item), any())).thenReturn("SCRUM-T1");

        // Act
        agent.executar(context);

        // Assert
        verify(jiraClient, never()).buscarIssueId(any());
        verify(zephyrClient, never()).linkarIssueJira(any(), any());
    }

    @Test
    @DisplayName("Deve resolver o id da issue uma única vez e vincular cada caso de teste criado")
    void deveVincularCadaCasoDeTesteAIssueJiraInformada() {
        // Arrange
        CenarioRequest requestComIssue = new CenarioRequest(
                "Login OAuth", "Sistema de login", "gerador_cenarios_testes",
                com.br.criarcenariotestes.business.workflow.WorkflowType.RAPIDO, "SCRUM-29"
        );
        WorkflowContext contextComIssue = new WorkflowContext(requestComIssue);

        CenarioItem item1 = new CenarioItem();
        item1.setNome("Cenário 1");
        CenarioItem item2 = new CenarioItem();
        item2.setNome("Cenário 2");
        contextComIssue.setCenarios(List.of(item1, item2));

        when(jiraClient.buscarIssueId("SCRUM-29")).thenReturn("10001");
        when(zephyrClient.criarCasoDeTeste(any(), any())).thenReturn("SCRUM-T1", "SCRUM-T2");

        // Act
        agent.executar(contextComIssue);

        // Assert - resolve o id 1 única vez, vincula os 2 casos de teste criados
        verify(jiraClient, times(1)).buscarIssueId("SCRUM-29");
        verify(zephyrClient, times(1)).linkarIssueJira("SCRUM-T1", "10001");
        verify(zephyrClient, times(1)).linkarIssueJira("SCRUM-T2", "10001");
    }

    @Test
    @DisplayName("Falha ao resolver o id da issue não deve impedir a publicação nem o vínculo")
    void falhaAoResolverIdDaIssueNaoDeveImpedirPublicacao() {
        // Arrange
        CenarioRequest requestComIssue = new CenarioRequest(
                "Login OAuth", "Sistema de login", "gerador_cenarios_testes",
                com.br.criarcenariotestes.business.workflow.WorkflowType.RAPIDO, "SCRUM-INEXISTENTE"
        );
        WorkflowContext contextComIssue = new WorkflowContext(requestComIssue);

        CenarioItem item = new CenarioItem();
        item.setNome("Cenário qualquer");
        contextComIssue.setCenarios(List.of(item));

        when(jiraClient.buscarIssueId("SCRUM-INEXISTENTE"))
                .thenThrow(new RuntimeException("Task Jira nao encontrada"));
        when(zephyrClient.criarCasoDeTeste(eq(item), any())).thenReturn("SCRUM-T1");

        // Act
        agent.executar(contextComIssue);

        // Assert - publica normalmente, só não vincula
        assertThat(item.getZephyrTestCaseKey()).isEqualTo("SCRUM-T1");
        assertThat(contextComIssue.getMetadata("zephyr_falhas")).isEqualTo(0);
        verify(zephyrClient, never()).linkarIssueJira(any(), any());
    }

    @Test
    @DisplayName("Falha ao vincular no Zephyr não deve derrubar a publicação do caso de teste já criado")
    void falhaAoVincularNaoDeveDerrubarPublicacao() {
        // Arrange
        CenarioRequest requestComIssue = new CenarioRequest(
                "Login OAuth", "Sistema de login", "gerador_cenarios_testes",
                com.br.criarcenariotestes.business.workflow.WorkflowType.RAPIDO, "SCRUM-29"
        );
        WorkflowContext contextComIssue = new WorkflowContext(requestComIssue);

        CenarioItem item = new CenarioItem();
        item.setNome("Cenário qualquer");
        contextComIssue.setCenarios(List.of(item));

        when(jiraClient.buscarIssueId("SCRUM-29")).thenReturn("10001");
        when(zephyrClient.criarCasoDeTeste(eq(item), any())).thenReturn("SCRUM-T1");
        org.mockito.Mockito.doThrow(new IllegalStateException("Zephyr indisponível pra vincular"))
                .when(zephyrClient).linkarIssueJira("SCRUM-T1", "10001");

        // Act
        agent.executar(contextComIssue);

        // Assert - caso de teste continua marcado como publicado com sucesso
        assertThat(item.getZephyrTestCaseKey()).isEqualTo("SCRUM-T1");
        assertThat(contextComIssue.getMetadata("zephyr_publicados")).isEqualTo(1);
        assertThat(contextComIssue.getMetadata("zephyr_falhas")).isEqualTo(0);
    }
}
