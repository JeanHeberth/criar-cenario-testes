package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedundancyReviewAgent - Testes Unitários")
class RedundancyReviewAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private CenarioTextoParser cenarioTextoParser;

    @InjectMocks
    private RedundancyReviewAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request, WorkflowType.COMPLETO);

        CenarioItem cenario1 = new CenarioItem();
        cenario1.setNome("Login com email válido");
        cenario1.setScriptTeste("1. Informar email\n2. Clicar em entrar");

        CenarioItem cenario2 = new CenarioItem();
        cenario2.setNome("Login com e-mail válido");
        cenario2.setScriptTeste("1. Digitar e-mail\n2. Clicar em login");

        context.setCenarios(List.of(cenario1, cenario2));
    }

    @Test
    @DisplayName("Deve revisar e remover redundâncias com sucesso")
    void deveRevisarERemoverRedundancias() {
        // Arrange
        CenarioItem cenarioOtimizado = new CenarioItem();
        cenarioOtimizado.setNome("Login com credenciais válidas [email/senha]");

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn("---\nNome: CT001\n---");
        when(aiProvider.getName()).thenReturn("Gemini");
        when(cenarioTextoParser.parsear(anyString())).thenReturn(List.of(cenarioOtimizado));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isNotNull();
        assertThat(context.getCenariosRevisados()).hasSize(1);
        assertThat(context.getMetadata("revisao_provider")).isEqualTo("Gemini");
        assertThat(context.getMetadata("cenarios_originais")).isEqualTo(2);
        assertThat(context.getMetadata("cenarios_revisados")).isEqualTo(1);

        verify(aiProviderResolver, times(1)).getActiveProvider();
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Redundancy Reviewer");
    }

    @Test
    @DisplayName("Deve ser habilitado no workflow COMPLETO")
    void deveSerHabilitadoNoWorkflowCompleto() {
        // Arrange
        context.setWorkflowType(WorkflowType.COMPLETO);

        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve ser habilitado no workflow REVISAO")
    void deveSerHabilitadoNoWorkflowRevisao() {
        // Arrange
        context.setWorkflowType(WorkflowType.REVISAO);

        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve ser desabilitado no workflow RAPIDO")
    void deveSerDesabilitadoNoWorkflowRapido() {
        // Arrange
        context.setWorkflowType(WorkflowType.RAPIDO);

        // Assert
        assertThat(agent.isEnabled(context)).isFalse();
    }

    @Test
    @DisplayName("Deve pular revisão se não houver cenários")
    void devePularRevisaoSeNaoHouverCenarios() {
        // Arrange
        context.setCenarios(new ArrayList<>());

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isEmpty();
        verify(aiProviderResolver, never()).getActiveProvider();
    }

    @Test
    @DisplayName("Deve manter cenários originais em caso de erro")
    void deveManterCenariosOriginaisEmCasoDeErro() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro"));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getCenariosRevisados()).isEqualTo(context.getCenarios());
    }
}
