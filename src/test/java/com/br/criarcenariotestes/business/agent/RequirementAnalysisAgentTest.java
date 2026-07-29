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
@DisplayName("RequirementAnalysisAgent - Testes Unitários")
class RequirementAnalysisAgentTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @InjectMocks
    private RequirementAnalysisAgent agent;

    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        CenarioRequest request = new CenarioRequest(
                "Login com OAuth",
                "Usuário deve poder fazer login usando Google, Facebook ou Microsoft",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request);
    }

    @Test
    @DisplayName("Deve extrair requisitos com sucesso")
    void deveExtrairRequisitosComSucesso() {
        // Arrange
        String respostaIA = """
                ## Requisitos Funcionais
                RF001: Sistema deve permitir login via OAuth
                RF002: Deve suportar Google, Facebook e Microsoft
                
                ## Requisitos Não-Funcionais
                RNF001: Tempo de resposta < 2s
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaIA);
        when(aiProvider.getName()).thenReturn("OpenAI");

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getRequisitos()).isNotNull();
        assertThat(context.getRequisitos()).contains("Requisitos Funcionais");
        assertThat(context.getRequisitos()).contains("OAuth");
        assertThat(context.getMetadata("requisitos_provider")).isEqualTo("OpenAI");

        verify(aiProviderResolver, times(1)).getActiveProvider();
        verify(aiProvider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Requirement Analyst");
    }

    @Test
    @DisplayName("Deve tratar erro ao chamar IA")
    void deveTratarErroAoChamarIA() {
        // Arrange
        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro de conexão"));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getRequisitos()).isNotNull();
        assertThat(context.getRequisitos()).contains("Erro ao analisar requisitos");
    }

    @Test
    @DisplayName("Deve sempre estar habilitado")
    void deveSempreEstarHabilitado() {
        // Assert
        assertThat(agent.isEnabled(context)).isTrue();
    }
}
