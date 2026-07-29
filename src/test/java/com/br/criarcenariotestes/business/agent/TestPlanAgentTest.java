package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
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
@DisplayName("TestPlanAgent - Testes Unitários")
class TestPlanAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @InjectMocks
    private TestPlanAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request);
        context.setRequisitos("RF001: Login via OAuth");
        context.setDecisoesReuniao("Decisão: Usar OAuth 2.0");
    }

    @Test
    @DisplayName("Deve criar plano de testes com sucesso")
    void deveCriarPlanoComSucesso() {
        // Arrange
        String respostaIA = """
                ## Estratégia de Cobertura
                1. Cenários positivos (login bem-sucedido)
                2. Cenários negativos (credenciais inválidas)
                3. Edge cases (timeout, rede indisponível)
                
                ## Priorização
                Alta: Login básico
                Média: Refresh token
                Baixa: Logout
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaIA);
        when(aiProvider.getName()).thenReturn("OpenAI");

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getPlanoMacro()).isNotNull();
        assertThat(context.getPlanoMacro()).contains("Estratégia de Cobertura");
        assertThat(context.getPlanoMacro()).contains("Priorização");
        assertThat(context.getMetadata("plano_provider")).isEqualTo("OpenAI");

        verify(aiProviderResolver, times(1)).getActiveProvider();
        verify(aiProvider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Test Planning Agent");
    }

    @Test
    @DisplayName("Deve incluir requisitos e decisões no prompt")
    void deveIncluirRequisitosEDecisoesNoPrompt() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn("Plano criado");

        // Act
        agent.executar(context);

        // Assert
        verify(aiProvider).gerarResposta(anyString(), anyString());
        assertThat(context.getPlanoMacro()).isNotNull();
    }

    @Test
    @DisplayName("Deve tratar erro ao gerar plano")
    void deveTratarErroAoGerarPlano() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro de rede"));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getPlanoMacro()).isNotNull();
        assertThat(context.getPlanoMacro()).contains("Erro ao gerar plano");
    }
}
