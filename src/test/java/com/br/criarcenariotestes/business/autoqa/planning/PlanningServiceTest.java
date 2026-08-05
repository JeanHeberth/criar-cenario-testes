package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningParseException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningTechnicalException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PlanningService - Testes Unitários")
class PlanningServiceTest {

    private AiProviderResolver aiProviderResolver;
    private PlanningInputSanitizer inputSanitizer;
    private PlanningPromptFactory promptFactory;
    private PlanningResponseParser responseParser;
    private PlanningValidator validator;
    private PlanningService service;

    private AiProvider primaryProvider;
    private AiProvider fallbackProvider;

    @BeforeEach
    void setUp() {
        aiProviderResolver = Mockito.mock(AiProviderResolver.class);
        inputSanitizer = Mockito.mock(PlanningInputSanitizer.class);
        promptFactory = Mockito.mock(PlanningPromptFactory.class);
        responseParser = Mockito.mock(PlanningResponseParser.class);
        validator = Mockito.mock(PlanningValidator.class);
        service = new PlanningService(aiProviderResolver, inputSanitizer, promptFactory, responseParser, validator);

        primaryProvider = mockProvider("primary");
        fallbackProvider = mockProvider("fallback");

        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(fallbackProvider);
        when(inputSanitizer.sanitize(any(), any(), any()))
            .thenReturn(Mockito.mock(SanitizedPlanningInput.class));
        when(promptFactory.createSystemPrompt()).thenReturn("system");
        when(promptFactory.createUserPrompt(any())).thenReturn("user");
    }

    @Test
    @DisplayName("Deve chamar provider ativo em caso de sucesso")
    void deveChamarProviderAtivo() {
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(planResult);
        when(validator.validate(any(), any(), any(), any())).thenReturn(planResult);

        TechnicalPlanResult result = service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        );

        assertThat(result).isNotNull();
        verify(primaryProvider).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve usar fallback quando provider ativo falha com TechnicalException")
    void deveUsarFallbackQuandoTechnicalException() {
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any()))
            .thenThrow(new RuntimeException("falha primário"));
        when(fallbackProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(planResult);
        when(validator.validate(any(), any(), any(), any())).thenReturn(planResult);

        TechnicalPlanResult result = service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        );

        assertThat(result).isNotNull();
        verify(primaryProvider).gerarResposta(any(), any());
        verify(fallbackProvider).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve usar fallback quando provider ativo falha com ParseException")
    void deveUsarFallbackQuandoParseException() {
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("invalid json");
        when(responseParser.parse("invalid json")).thenThrow(new PlanningParseException("JSON inválido"));
        when(fallbackProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse("{}")).thenReturn(planResult);
        when(validator.validate(any(), any(), any(), any())).thenReturn(planResult);

        TechnicalPlanResult result = service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        );

        assertThat(result).isNotNull();
        verify(fallbackProvider).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve lançar PlanningTechnicalException quando ambos falham")
    void deveLancarExcecaoQuandoAmbosFalham() {
        when(primaryProvider.gerarResposta(any(), any()))
            .thenThrow(new RuntimeException("falha primário"));
        when(fallbackProvider.gerarResposta(any(), any()))
            .thenThrow(new RuntimeException("falha fallback"));

        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        )).isInstanceOf(PlanningTechnicalException.class);
    }

    @Test
    @DisplayName("Deve lançar PlanningValidationException para cenário INVALID sem chamar provider")
    void deveLancarValidationExceptionParaCenarioInvalido() {
        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.invalidScenario(), PlanningTestData.completeKnowledge()
        )).isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("inválido");

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve lançar PlanningValidationException para knowledge FAILED sem chamar provider")
    void deveLancarValidationExceptionParaKnowledgeFailed() {
        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.failedKnowledge()
        )).isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("FAILED");

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para discovery nulo")
    void deveLancarExcecaoParaDiscoveryNulo() {
        assertThatThrownBy(() -> service.plan(null, PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para scenario nulo")
    void deveLancarExcecaoParaScenarioNulo() {
        assertThatThrownBy(() -> service.plan(PlanningTestData.discovery(), null, PlanningTestData.completeKnowledge()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para knowledge nulo")
    void deveLancarExcecaoParaKnowledgeNulo() {
        assertThatThrownBy(() -> service.plan(PlanningTestData.discovery(), PlanningTestData.validScenario(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Não deve chamar fallback quando sameProvider")
    void naoDeveChamarFallbackQuandoSameProvider() {
        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(primaryProvider);
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        )).isInstanceOf(PlanningTechnicalException.class);

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve propagar PlanningValidationException do validator sem chamar fallback")
    void devePropagateValidationExceptionSemFallback() {
        TechnicalPlanResult parsed = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(parsed);
        when(validator.validate(any(), any(), any(), any()))
            .thenThrow(new PlanningValidationException("validação falhou"));

        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        )).isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("validação");

        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve chamar provider no máximo uma vez cada")
    void deveChamarCadaProviderNoMaximoUmaVez() {
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(planResult);
        when(validator.validate(any(), any(), any(), any())).thenReturn(planResult);

        service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        );

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve passar discovery/scenario/knowledge ao validator")
    void devePassarContextoAoValidator() {
        ProjectDiscoveryResult discovery = PlanningTestData.discovery();
        ScenarioAnalysisResult scenario = PlanningTestData.validScenario();
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge();
        TechnicalPlanResult planResult = PlanningTestData.readyPlan();

        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(planResult);
        when(validator.validate(any(), any(), any(), any())).thenReturn(planResult);

        service.plan(discovery, scenario, knowledge);

        verify(validator).validate(planResult, discovery, scenario, knowledge);
    }

    @Test
    @DisplayName("Deve manter suppressed exception do primário ao lançar após fallback")
    void deveManterSuppressedExceptionDoPrimario() {
        when(primaryProvider.gerarResposta(any(), any()))
            .thenThrow(new RuntimeException("falha primário"));
        when(fallbackProvider.gerarResposta(any(), any()))
            .thenThrow(new RuntimeException("falha fallback"));

        Throwable thrown = catchThrowable(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        ));

        assertThat(thrown).isInstanceOf(PlanningTechnicalException.class);
        assertThat(thrown.getSuppressed()).hasSize(1);
    }

    @Test
    @DisplayName("Deve propagar ValidationException do fallback")
    void devePropagateValidationExceptionDoFallback() {
        TechnicalPlanResult parsed = PlanningTestData.readyPlan();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("invalid");
        when(responseParser.parse("invalid")).thenThrow(new PlanningParseException("parse error"));
        when(fallbackProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse("{}")).thenReturn(parsed);
        when(validator.validate(any(), any(), any(), any()))
            .thenThrow(new PlanningValidationException("validação do fallback"));

        assertThatThrownBy(() -> service.plan(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()
        )).isInstanceOf(PlanningValidationException.class);
    }

    // --- helper ---

    private AiProvider mockProvider(String name) {
        AiProvider provider = Mockito.mock(AiProvider.class);
        when(provider.getName()).thenReturn(name);
        return provider;
    }
}
