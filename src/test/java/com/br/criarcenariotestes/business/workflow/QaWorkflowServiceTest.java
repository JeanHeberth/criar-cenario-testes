package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.agent.*;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QaWorkflowService - Testes Unitários")
class QaWorkflowServiceTest {

    @Mock
    private RequirementAnalysisAgent requirementAnalysisAgent;

    @Mock
    private TranscriptAnalysisAgent transcriptAnalysisAgent;

    @Mock
    private TestPlanAgent testPlanAgent;

    @Mock
    private TestScenarioAgent testScenarioAgent;

    @Mock
    private RedundancyReviewAgent redundancyReviewAgent;

    @Mock
    private BddFormatterAgent bddFormatterAgent;

    @Mock
    private ZephyrFormatterAgent zephyrFormatterAgent;

    @Mock
    private ZephyrPublisherAgent zephyrPublisherAgent;

    @Mock
    private AgentLoaderService agentLoaderService;

    @Mock
    private CenarioRepository cenarioRepository;

    private QaWorkflowService service;

    private CenarioRequest request;

    @BeforeEach
    void setUp() {
        service = new QaWorkflowService(
                requirementAnalysisAgent,
                transcriptAnalysisAgent,
                testPlanAgent,
                testScenarioAgent,
                redundancyReviewAgent,
                bddFormatterAgent,
                zephyrFormatterAgent,
                zephyrPublisherAgent,
                agentLoaderService,
                cenarioRepository,
                new GeneratedScenariosValidator()
        );

        request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );

        when(agentLoaderService.loadAgentInstructions(anyString()))
                .thenReturn("Instruções do agente");

        when(requirementAnalysisAgent.getNome()).thenReturn("Requirement Analyst");
        when(transcriptAnalysisAgent.getNome()).thenReturn("Transcript Analyst");
        when(testPlanAgent.getNome()).thenReturn("Test Planning Agent");
        when(testScenarioAgent.getNome()).thenReturn("Test Scenario Generator");
        when(redundancyReviewAgent.getNome()).thenReturn("Redundancy Reviewer");
        when(bddFormatterAgent.getNome()).thenReturn("BDD Formatter");
        when(zephyrFormatterAgent.getNome()).thenReturn("Zephyr Formatter");
        when(zephyrPublisherAgent.getNome()).thenReturn("Zephyr Publisher");
    }

    @Test
    @DisplayName("Deve executar workflow COMPLETO com todos os 7 agentes")
    void deveExecutarWorkflowCompleto() {
        // Arrange
        mockAgentesHabilitados();
        mockCenariosValidos();
        mockCenarioSalvo();

        // Act
        CenarioResponse response = service.executarWorkflow(request, WorkflowType.COMPLETO);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.titulo()).isEqualTo("Login OAuth");

        verify(requirementAnalysisAgent, times(1)).executar(any(WorkflowContext.class));
        verify(transcriptAnalysisAgent, times(1)).executar(any(WorkflowContext.class));
        verify(testPlanAgent, times(1)).executar(any(WorkflowContext.class));
        verify(testScenarioAgent, times(1)).executar(any(WorkflowContext.class));
        verify(redundancyReviewAgent, times(1)).executar(any(WorkflowContext.class));
        verify(bddFormatterAgent, times(1)).executar(any(WorkflowContext.class));
        verify(zephyrFormatterAgent, times(1)).executar(any(WorkflowContext.class));
        verify(zephyrPublisherAgent, times(1)).executar(any(WorkflowContext.class));
        verify(cenarioRepository, times(1)).save(any(Cenario.class));
    }

    @Test
    @DisplayName("Deve executar workflow RAPIDO sem Transcript e Redundancy")
    void deveExecutarWorkflowRapido() {
        // Arrange
        mockAgentesHabilitados();
        when(transcriptAnalysisAgent.isEnabled(any())).thenReturn(false);
        when(redundancyReviewAgent.isEnabled(any())).thenReturn(false);
        mockCenariosValidos();
        mockCenarioSalvo();

        // Act
        CenarioResponse response = service.executarWorkflow(request, WorkflowType.RAPIDO);

        // Assert
        assertThat(response).isNotNull();

        verify(requirementAnalysisAgent, times(1)).executar(any());
        verify(transcriptAnalysisAgent, never()).executar(any());
        verify(testPlanAgent, times(1)).executar(any());
        verify(testScenarioAgent, times(1)).executar(any());
        verify(redundancyReviewAgent, never()).executar(any());
        verify(bddFormatterAgent, times(1)).executar(any());
        verify(zephyrFormatterAgent, times(1)).executar(any());
        verify(zephyrPublisherAgent, times(1)).executar(any());
    }

    @Test
    @DisplayName("Deve executar workflow REVISAO apenas com 3 agentes")
    void deveExecutarWorkflowRevisao() {
        // Arrange
        mockAgentesHabilitados();
        mockCenariosValidos();
        mockCenarioSalvo();

        // Act
        CenarioResponse response = service.executarWorkflow(request, WorkflowType.REVISAO);

        // Assert
        assertThat(response).isNotNull();

        verify(requirementAnalysisAgent, never()).executar(any());
        verify(transcriptAnalysisAgent, never()).executar(any());
        verify(testPlanAgent, never()).executar(any());
        verify(testScenarioAgent, never()).executar(any());
        verify(redundancyReviewAgent, times(1)).executar(any());
        verify(bddFormatterAgent, times(1)).executar(any());
        verify(zephyrFormatterAgent, times(1)).executar(any());
        verify(zephyrPublisherAgent, times(1)).executar(any());
    }

    @Test
    @DisplayName("Deve usar workflowType do request quando não especificado")
    void deveUsarWorkflowTypeDoRequest() {
        // Arrange
        mockAgentesHabilitados();
        mockCenariosValidos();
        mockCenarioSalvo();

        // Act
        CenarioResponse response = service.executarWorkflow(request);

        // Assert
        assertThat(response).isNotNull();
        verify(requirementAnalysisAgent, times(1)).executar(any());
    }

    @Test
    @DisplayName("Deve propagar exceção quando agente falhar")
    void devePropagaExcecaoQuandoAgenteFalhar() {
        // Arrange
        when(requirementAnalysisAgent.isEnabled(any())).thenReturn(true);
        doThrow(new RuntimeException("Erro no agente"))
                .when(requirementAnalysisAgent).executar(any());

        // Act & Assert
        assertThatThrownBy(() -> service.executarWorkflow(request, WorkflowType.COMPLETO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha no workflow no agente: Requirement Analyst");
    }

    @Test
    @DisplayName("Deve carregar instruções do agente no início")
    void deveCarregarInstrucoesDoAgente() {
        // Arrange
        mockAgentesHabilitados();
        mockCenariosValidos();
        mockCenarioSalvo();

        // Act
        service.executarWorkflow(request, WorkflowType.COMPLETO);

        // Assert
        verify(agentLoaderService, times(1))
                .loadAgentInstructions("gerador_cenarios_testes");
    }

    @Test
    @DisplayName("Deve salvar cenário com dados corretos do context")
    void deveSalvarCenarioComDadosCorretosDoContext() {
        // Arrange
        mockAgentesHabilitados();
        
        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            CenarioItem item = new CenarioItem();
            item.setNome("CT001");
            item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
            item.setResultadoEsperado("Então o login é realizado com sucesso");
            ctx.setCenarios(List.of(item));
            ctx.setCriteriosAceitacao("Critérios definidos");
            return null;
        }).when(testScenarioAgent).executar(any());

        Cenario cenarioSalvo = new Cenario();
        cenarioSalvo.setId("123");
        cenarioSalvo.setTitulo("Login OAuth");
        when(cenarioRepository.save(any(Cenario.class))).thenReturn(cenarioSalvo);

        // Act
        CenarioResponse response = service.executarWorkflow(request, WorkflowType.COMPLETO);

        // Assert
        ArgumentCaptor<Cenario> cenarioCaptor = ArgumentCaptor.forClass(Cenario.class);
        verify(cenarioRepository).save(cenarioCaptor.capture());

        Cenario cenarioSalvado = cenarioCaptor.getValue();
        assertThat(cenarioSalvado.getTitulo()).isEqualTo("Login OAuth");
        assertThat(cenarioSalvado.getRegraDeNegocio()).isEqualTo("Sistema de login");
        assertThat(cenarioSalvado.getCriteriosAceitacao()).isEqualTo("Critérios definidos");
        assertThat(cenarioSalvado.getCenarios()).hasSize(1);
        
        assertThat(response.id()).isEqualTo("123");
        assertThat(response.titulo()).isEqualTo("Login OAuth");
    }

    // ===== FASE15-BUG-003A: validação final antes da persistência =====

    @Test
    @DisplayName("FASE15-BUG-003A: não deve persistir quando o cenário final tiver Passos vazio")
    void naoDevePersistirQuandoCenarioFinalTiverPassosVazio() {
        // Arrange
        mockAgentesHabilitados();
        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            CenarioItem item = new CenarioItem();
            item.setNome("Login com credenciais válidas");
            item.setScriptTeste("");
            item.setResultadoEsperado("Então o login é realizado com sucesso");
            ctx.setCenarios(List.of(item));
            return null;
        }).when(testScenarioAgent).executar(any());

        // Act & Assert
        assertThatThrownBy(() -> service.executarWorkflow(request, WorkflowType.COMPLETO))
                .isInstanceOf(RuntimeException.class);

        verify(cenarioRepository, never()).save(any(Cenario.class));

        // Regressão: publicar no Zephyr ANTES da validação estrutural criava
        // casos de teste órfãos e permanentes no Zephyr para cenários que
        // acabavam rejeitados e nunca persistidos aqui. Nunca deve publicar
        // quando a validação falha.
        verify(zephyrPublisherAgent, never()).executar(any());
    }

    @Test
    @DisplayName("FASE15-BUG-003A: não deve persistir quando o cenário final tiver Resultado Esperado vazio")
    void naoDevePersistirQuandoCenarioFinalTiverResultadoEsperadoVazio() {
        // Arrange
        mockAgentesHabilitados();
        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            CenarioItem item = new CenarioItem();
            item.setNome("Login com credenciais válidas");
            item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
            item.setResultadoEsperado("");
            ctx.setCenarios(List.of(item));
            return null;
        }).when(testScenarioAgent).executar(any());

        // Act & Assert
        assertThatThrownBy(() -> service.executarWorkflow(request, WorkflowType.COMPLETO))
                .isInstanceOf(RuntimeException.class);

        verify(cenarioRepository, never()).save(any(Cenario.class));

        // Regressão: publicar no Zephyr ANTES da validação estrutural criava
        // casos de teste órfãos e permanentes no Zephyr para cenários que
        // acabavam rejeitados e nunca persistidos aqui. Nunca deve publicar
        // quando a validação falha.
        verify(zephyrPublisherAgent, never()).executar(any());
    }

    @Test
    @DisplayName("FASE15-BUG-003A: não deve persistir quando o cenário final não tiver estrutura BDD mínima (Dado/Quando)")
    void naoDevePersistirQuandoCenarioFinalNaoTiverEstruturaBdd() {
        // Arrange
        mockAgentesHabilitados();
        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            CenarioItem item = new CenarioItem();
            item.setNome("Login com credenciais válidas");
            item.setScriptTeste("O usuário acessa a tela e informa os dados"); // sem Dado/Quando
            item.setResultadoEsperado("Então o login é realizado com sucesso");
            ctx.setCenarios(List.of(item));
            return null;
        }).when(testScenarioAgent).executar(any());

        // Act & Assert
        assertThatThrownBy(() -> service.executarWorkflow(request, WorkflowType.COMPLETO))
                .isInstanceOf(RuntimeException.class);

        verify(cenarioRepository, never()).save(any(Cenario.class));

        // Regressão: publicar no Zephyr ANTES da validação estrutural criava
        // casos de teste órfãos e permanentes no Zephyr para cenários que
        // acabavam rejeitados e nunca persistidos aqui. Nunca deve publicar
        // quando a validação falha.
        verify(zephyrPublisherAgent, never()).executar(any());
    }

    private void mockAgentesHabilitados() {
        when(requirementAnalysisAgent.isEnabled(any())).thenReturn(true);
        when(transcriptAnalysisAgent.isEnabled(any())).thenReturn(true);
        when(testPlanAgent.isEnabled(any())).thenReturn(true);
        when(testScenarioAgent.isEnabled(any())).thenReturn(true);
        when(redundancyReviewAgent.isEnabled(any())).thenReturn(true);
        when(bddFormatterAgent.isEnabled(any())).thenReturn(true);
        when(zephyrFormatterAgent.isEnabled(any())).thenReturn(true);
        when(zephyrPublisherAgent.isEnabled(any())).thenReturn(true);
    }

    private CenarioItem cenarioBddValido() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
        item.setResultadoEsperado("Então o login é realizado com sucesso");
        return item;
    }

    private void mockCenariosValidos() {
        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            ctx.setCenarios(List.of(cenarioBddValido()));
            return null;
        }).when(testScenarioAgent).executar(any());

        doAnswer(invocation -> {
            WorkflowContext ctx = invocation.getArgument(0);
            ctx.setCenariosRevisados(List.of(cenarioBddValido()));
            return null;
        }).when(redundancyReviewAgent).executar(any());
    }

    private void mockCenarioSalvo() {
        Cenario cenarioSalvo = new Cenario();
        cenarioSalvo.setId("123");
        cenarioSalvo.setTitulo("Login OAuth");
        cenarioSalvo.setRegraDeNegocio("Sistema de login");
        cenarioSalvo.setCenarios(List.of(new CenarioItem()));
        when(cenarioRepository.save(any(Cenario.class))).thenReturn(cenarioSalvo);
    }
}
