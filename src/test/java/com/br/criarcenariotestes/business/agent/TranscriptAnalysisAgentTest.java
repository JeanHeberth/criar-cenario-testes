package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptAnalysisAgent - Testes Unitários")
class TranscriptAnalysisAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @InjectMocks
    private TranscriptAnalysisAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        CenarioRequest request = new CenarioRequest(
                "Login com OAuth",
                "Usuário deve poder fazer login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request, WorkflowType.COMPLETO);
        context.setRequisitos("RF001: Login via OAuth");
    }

    @Test
    @DisplayName("Deve extrair decisões de reunião com sucesso")
    void deveExtrairDecisoesComSucesso() {
        // Arrange
        String respostaIA = """
                ## Decisões
                - Usar OAuth 2.0
                - Implementar refresh token
                
                ## Ambiguidades
                - Tempo de expiração do token não definido
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaIA);
        when(aiProvider.getName()).thenReturn("Gemini");

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getDecisoesReuniao()).isNotNull();
        assertThat(context.getDecisoesReuniao()).contains("Decisões");
        assertThat(context.getDecisoesReuniao()).contains("OAuth 2.0");
        assertThat(context.getMetadata("transcript_provider")).isEqualTo("Gemini");

        verify(aiProviderResolver, times(1)).getActiveProvider();
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Transcript Analyst");
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
    @DisplayName("Deve ser habilitado no workflow COMPLETO")
    void deveSerHabilitadoNoWorkflowCompleto() {
        // Arrange
        context.setWorkflowType(WorkflowType.COMPLETO);

        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }

    @Test
    @DisplayName("Deve tratar erro gracefully")
    void deveTratarErroGracefully() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro"));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getDecisoesReuniao()).isNotNull();
        assertThat(context.getDecisoesReuniao())
                .contains("Nenhuma transcrição de reunião disponível");
    }
}
