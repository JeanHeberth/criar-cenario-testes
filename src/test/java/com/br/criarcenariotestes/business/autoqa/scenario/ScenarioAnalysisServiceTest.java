package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ScenarioAnalysisService - Testes Unitários")
class ScenarioAnalysisServiceTest {

    private final AiProviderResolver resolver = Mockito.mock(AiProviderResolver.class);
    private final ScenarioAnalysisPromptFactory promptFactory = new ScenarioAnalysisPromptFactory();
    private final ScenarioAnalysisResponseParser parser = new ScenarioAnalysisResponseParser(new ObjectMapper());
    private final ScenarioAnalysisValidator validator = new ScenarioAnalysisValidator();
    private final ScenarioInputSanitizer sanitizer = new ScenarioInputSanitizer();

    @Test
    @DisplayName("Deve analisar com provider ativo")
    void deveAnalisarComProviderAtivo() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn(ScenarioAnalysisTestData.validJson());

        ScenarioAnalysisResult result = service.analyze("Login válido", ScenarioAnalysisTestData.discovery());

        assertThat(result.title()).isEqualTo("Login válido");
        verify(active).gerarResposta(anyString(), anyString());
        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve enviar somente cenário sanitizado ao provider")
    void deveEnviarSomenteCenarioSanitizadoAoProvider() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn(ScenarioAnalysisTestData.validJson());

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        service.analyze(ScenarioAnalysisTestData.secretScenario(), ScenarioAnalysisTestData.discovery());

        verify(active).gerarResposta(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue()).contains("[REDACTED]");
        assertThat(userPrompt.getValue()).doesNotContain("MinhaSenha123");
        assertThat(userPrompt.getValue()).doesNotContain("abc123");
    }

    @Test
    @DisplayName("Deve não usar fallback em ValidationException")
    void deveNaoUsarFallbackEmValidationException() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn(
                ScenarioAnalysisTestData.validJson().replace("\"title\": \"Login válido\"", "\"title\": \" \"")
        );

        assertThatThrownBy(() -> service.analyze("Login válido", ScenarioAnalysisTestData.discovery()))
                .isInstanceOf(ScenarioAnalysisValidationException.class);

        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve usar fallback quando resposta for vazia")
    void deveUsarFallbackQuandoRespostaForVazia() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn("   ");
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(ScenarioAnalysisTestData.validJson());

        ScenarioAnalysisResult result = service.analyze("Login válido", ScenarioAnalysisTestData.discovery());

        assertThat(result.status()).isEqualTo(ScenarioAnalysisStatus.VALID);
        verify(fallback).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve não usar fallback em NullPointerException interna")
    void deveNaoUsarFallbackEmNullPointerExceptionInterna() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisPromptFactory failingPromptFactory = Mockito.mock(ScenarioAnalysisPromptFactory.class);
        ScenarioAnalysisService service = newService(failingPromptFactory, parser, validator);
        when(failingPromptFactory.createSystemPrompt()).thenReturn("system");
        when(failingPromptFactory.createUserPrompt(anyString(), Mockito.any(ProjectDiscoveryResult.class)))
                .thenThrow(new NullPointerException("boom"));

        assertThatThrownBy(() -> service.analyze("Login válido", ScenarioAnalysisTestData.discovery()))
                .isInstanceOf(NullPointerException.class);

        verify(resolver, never()).getActiveProvider();
        verify(active, never()).gerarResposta(anyString(), anyString());
        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve chamar cada provider uma única vez")
    void deveChamarCadaProviderUmaUnicaVez() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(ScenarioAnalysisTestData.validJson());

        ScenarioAnalysisResult result = service.analyze("Login válido", ScenarioAnalysisTestData.discovery());

        assertThat(result.status()).isEqualTo(ScenarioAnalysisStatus.VALID);
        verify(active, times(1)).gerarResposta(anyString(), anyString());
        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve falhar quando providers forem iguais")
    void deveFalharQuandoProvidersForemIguais() {
        AiProvider provider = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("openai");
        when(provider.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.analyze("Login válido", ScenarioAnalysisTestData.discovery()))
                .isInstanceOf(ScenarioAnalysisTechnicalException.class);

        verify(provider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve logicamente encerrar após falha do fallback")
    void deveLogicamenteEncerrarAposFalhaDoFallback() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));
        when(fallback.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.analyze("Login válido", ScenarioAnalysisTestData.discovery()))
                .isInstanceOf(ScenarioAnalysisTechnicalException.class);

        verify(active, times(1)).gerarResposta(anyString(), anyString());
        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve não enviar projectPath no prompt")
    void deveNaoEnviarProjectPathNoPrompt() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn(ScenarioAnalysisTestData.validJson());

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        service.analyze("Login válido", ScenarioAnalysisTestData.discovery());

        verify(active).gerarResposta(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue()).doesNotContain("/projeto");
    }

    @Test
    @DisplayName("Deve ser stateless entre execuções")
    void deveSerStatelessEntreExecucoes() {
        AiProvider active = Mockito.mock(AiProvider.class);
        AiProvider fallback = Mockito.mock(AiProvider.class);
        ScenarioAnalysisService service = newService(promptFactory, parser, validator);
        when(resolver.getActiveProvider()).thenReturn(active);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(active.getName()).thenReturn("openai");
        when(fallback.getName()).thenReturn("gemini");
        when(active.gerarResposta(anyString(), anyString())).thenReturn(
                ScenarioAnalysisTestData.validJson(),
                ScenarioAnalysisTestData.validJson().replace("Login válido", "Cadastro válido")
        );

        ScenarioAnalysisResult first = service.analyze("Login válido", ScenarioAnalysisTestData.discovery());
        ScenarioAnalysisResult second = service.analyze("Cadastro válido", ScenarioAnalysisTestData.discovery());

        assertThat(first.title()).isEqualTo("Login válido");
        assertThat(second.title()).isEqualTo("Cadastro válido");
    }

    private ScenarioAnalysisService newService(ScenarioAnalysisPromptFactory promptFactory,
                                               ScenarioAnalysisResponseParser parser,
                                               ScenarioAnalysisValidator validator) {
        return new ScenarioAnalysisService(resolver, sanitizer, promptFactory, parser, validator);
    }
}
