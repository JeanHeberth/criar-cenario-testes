package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.agent.BddFormatterAgent;
import com.br.criarcenariotestes.business.agent.RedundancyReviewAgent;
import com.br.criarcenariotestes.business.agent.RequirementAnalysisAgent;
import com.br.criarcenariotestes.business.agent.TestPlanAgent;
import com.br.criarcenariotestes.business.agent.TestScenarioAgent;
import com.br.criarcenariotestes.business.agent.TranscriptAnalysisAgent;
import com.br.criarcenariotestes.business.agent.ZephyrFormatterAgent;
import com.br.criarcenariotestes.business.agent.ZephyrPublisherAgent;
import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FASE15-BUG-002: reproduz o defeito real encontrado em Fase 15 — a IA
 * responde com um "Plano de Geração" (formato de arquivos) em vez de
 * cenários de teste, o que fazia o RedundancyReviewAgent alucinar cenários
 * completamente desconectados da regra de negócio original e o workflow
 * inteiro ser reportado como "sucesso" mesmo sem gerar nada válido.
 *
 * Este teste usa instâncias REAIS de TestScenarioAgent e RedundancyReviewAgent
 * (com CenarioTextoParser e GeneratedScenariosValidator reais), mockando
 * apenas o provider de IA — para provar, de ponta a ponta na orquestração,
 * que o Reviewer nunca é chamado e nada é persistido quando a geração é
 * estruturalmente inválida mesmo após o retry único.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QaWorkflowService - Regressão FASE15-BUG-002 (cascata de alucinação)")
class QaWorkflowServiceBug002RegressionTest {

    @Mock
    private AiProviderResolver aiProviderResolver;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private AgentLoaderService agentLoaderService;

    @Mock
    private CenarioRepository cenarioRepository;

    @Mock
    private RequirementAnalysisAgent requirementAnalysisAgent;

    @Mock
    private TranscriptAnalysisAgent transcriptAnalysisAgent;

    @Mock
    private TestPlanAgent testPlanAgent;

    @Mock
    private BddFormatterAgent bddFormatterAgent;

    @Mock
    private ZephyrFormatterAgent zephyrFormatterAgent;

    @Mock
    private ZephyrPublisherAgent zephyrPublisherAgent;

    private RedundancyReviewAgent redundancyReviewAgentSpy;
    private QaWorkflowService service;

    @BeforeEach
    void setUp() {
        CenarioTextoParser parser = new CenarioTextoParser();
        GeneratedScenariosValidator validator = new GeneratedScenariosValidator();

        TestScenarioAgent testScenarioAgentReal =
                new TestScenarioAgent(aiProviderResolver, parser, validator);
        redundancyReviewAgentSpy =
                spy(new RedundancyReviewAgent(aiProviderResolver, parser, validator));

        service = new QaWorkflowService(
                requirementAnalysisAgent,
                transcriptAnalysisAgent,
                testPlanAgent,
                testScenarioAgentReal,
                redundancyReviewAgentSpy,
                bddFormatterAgent,
                zephyrFormatterAgent,
                zephyrPublisherAgent,
                agentLoaderService,
                cenarioRepository,
                validator
        );

        when(agentLoaderService.loadAgentInstructions(anyString())).thenReturn("");
        when(requirementAnalysisAgent.isEnabled(any())).thenReturn(true);
        when(transcriptAnalysisAgent.isEnabled(any())).thenReturn(true);
        when(testPlanAgent.isEnabled(any())).thenReturn(true);
        when(bddFormatterAgent.isEnabled(any())).thenReturn(true);
        when(zephyrFormatterAgent.isEnabled(any())).thenReturn(true);
        when(zephyrPublisherAgent.isEnabled(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Login+2FA: se o gerador só retorna plano de arquivos (antes e após o retry), "
            + "o Reviewer nunca deve rodar e nada de alucinado deve ser persistido")
    void naoDevePersistirCenariosAlucinadosQuandoGeradorSempreRetornaPlanoDeArquivos() {
        // Arrange - reproduz literalmente o log de produção do FASE15-BUG-002
        String planoDeGeracao = """
                📋 Plano de Geração
                - Pasta base: `login_bloqueio_2fa_tests/`
                - Arquivos a criar:
                  - `CENARIOS_DE_TESTE.md` – Todos os cenários detalhados
                """;

        when(aiProviderResolver.getActiveProvider()).thenReturn(aiProvider);
        when(aiProvider.gerarResposta(anyString(), anyString(), eq(TestScenarioAgent.GENERATOR_MAX_TOKENS)))
                .thenReturn(planoDeGeracao);

        CenarioRequest request = new CenarioRequest(
                "Login com bloqueio por tentativas inválidas e autenticação em dois fatores",
                "Regra complexa de login + 2FA com bloqueio de conta e código de verificação",
                "gerador_cenarios_testes"
        );

        // Act & Assert - o workflow deve falhar de forma explícita, não reportar sucesso.
        // FASE15-BUG-003A: a exceção agora é uma ValidacaoEstruturalException propagada
        // sem reenvelopamento (para nunca ser mascarada pelo fallback do CenarioService).
        assertThatThrownBy(() -> service.executarWorkflow(request, WorkflowType.COMPLETO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na geração de cenários");

        // Exatamente 1 retry (2 chamadas ao provider), nunca um loop sem fim.
        verify(aiProvider, times(2)).gerarResposta(anyString(), anyString(), eq(TestScenarioAgent.GENERATOR_MAX_TOKENS));

        // O Reviewer NUNCA deve receber uma geração inválida.
        verify(redundancyReviewAgentSpy, never()).executar(any());

        // Nem o Zephyr Publisher - nunca deve criar casos de teste reais a
        // partir de uma geração estruturalmente inválida.
        verify(zephyrPublisherAgent, never()).executar(any());

        // Nada deve ser persistido a partir de uma geração inválida.
        verify(cenarioRepository, never()).save(any(Cenario.class));
    }
}
