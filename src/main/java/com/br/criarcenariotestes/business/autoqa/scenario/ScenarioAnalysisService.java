package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ScenarioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioAnalysisService.class);

    private final AiProviderResolver aiProviderResolver;
    private final ScenarioInputSanitizer inputSanitizer;
    private final ScenarioAnalysisPromptFactory promptFactory;
    private final ScenarioAnalysisResponseParser responseParser;
    private final ScenarioAnalysisValidator validator;

    public ScenarioAnalysisService(AiProviderResolver aiProviderResolver,
                                   ScenarioInputSanitizer inputSanitizer,
                                   ScenarioAnalysisPromptFactory promptFactory,
                                   ScenarioAnalysisResponseParser responseParser,
                                   ScenarioAnalysisValidator validator) {
        this.aiProviderResolver = Objects.requireNonNull(aiProviderResolver, "aiProviderResolver must not be null");
        this.inputSanitizer = Objects.requireNonNull(inputSanitizer, "inputSanitizer must not be null");
        this.promptFactory = Objects.requireNonNull(promptFactory, "promptFactory must not be null");
        this.responseParser = Objects.requireNonNull(responseParser, "responseParser must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public ScenarioAnalysisResult analyze(String scenario, ProjectDiscoveryResult discovery) {
        validateInput(scenario, discovery);

        String sanitizedScenario = inputSanitizer.sanitize(scenario);
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitizedScenario, discovery);
        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Scenario analysis provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Scenario analysis fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        try {
            return analyzeWithFallback(activeProvider, fallbackProvider, sameProvider, systemPrompt, userPrompt);
        } catch (ScenarioAnalysisValidationException exception) {
            throw exception;
        }
    }

    private ScenarioAnalysisResult analyzeWithFallback(AiProvider activeProvider,
                                                       AiProvider fallbackProvider,
                                                       boolean sameProvider,
                                                       String systemPrompt,
                                                       String userPrompt) {
        try {
            ScenarioAnalysisResult result = analyzeWithProvider(activeProvider, systemPrompt, userPrompt);
            log.debug("Scenario analysis concluída. provider={}, passos={}, ambiguidades={}",
                    safeProviderName(activeProvider), countSteps(result), countAmbiguities(result));
            return result;
        } catch (ScenarioAnalysisTechnicalException primaryFailure) {
            if (sameProvider) {
                log.error("Scenario analysis failed without fallback. provider={}, failureType={}, status=FAILED",
                        safeProviderName(activeProvider), primaryFailure.getClass().getSimpleName());
                throw new ScenarioAnalysisTechnicalException("Falha técnica na análise de cenário", primaryFailure);
            }
            return analyzeWithFallbackProvider(activeProvider, fallbackProvider, systemPrompt, userPrompt, primaryFailure);
        }
    }

    private ScenarioAnalysisResult analyzeWithFallbackProvider(AiProvider activeProvider,
                                                               AiProvider fallbackProvider,
                                                               String systemPrompt,
                                                               String userPrompt,
                                                               ScenarioAnalysisTechnicalException primaryFailure) {
        try {
            ScenarioAnalysisResult result = analyzeWithProvider(fallbackProvider, systemPrompt, userPrompt);
            log.debug("Scenario analysis concluída. provider={}, passos={}, ambiguidades={}",
                    safeProviderName(fallbackProvider), countSteps(result), countAmbiguities(result));
            return result;
        } catch (ScenarioAnalysisValidationException exception) {
            throw exception;
        } catch (ScenarioAnalysisTechnicalException fallbackFailure) {
            log.error("Scenario analysis failed after fallback. primaryProvider={}, fallbackProvider={}, primaryFailureType={}, fallbackFailureType={}, status=FAILED",
                    safeProviderName(activeProvider),
                    safeProviderName(fallbackProvider),
                    primaryFailure.getClass().getSimpleName(),
                    fallbackFailure.getClass().getSimpleName());
            ScenarioAnalysisTechnicalException technicalException =
                    new ScenarioAnalysisTechnicalException("Falha técnica na análise de cenário", fallbackFailure);
            technicalException.addSuppressed(primaryFailure);
            throw technicalException;
        }
    }

    private ScenarioAnalysisResult analyzeWithProvider(AiProvider provider, String systemPrompt, String userPrompt) {
        String response;
        try {
            response = provider.gerarResposta(systemPrompt, userPrompt);
        } catch (RuntimeException exception) {
            throw new ScenarioAnalysisTechnicalException("Falha técnica no provider " + provider.getName(), exception);
        }

        ScenarioAnalysisResult result = responseParser.parse(response);
        return validator.validate(result);
    }

    private boolean sameProvider(AiProvider activeProvider, AiProvider fallbackProvider) {
        return activeProvider == fallbackProvider
                || Objects.equals(normalizeProviderName(activeProvider), normalizeProviderName(fallbackProvider));
    }

    private String normalizeProviderName(AiProvider provider) {
        return provider == null ? null : safeProviderName(provider).trim();
    }

    private String safeProviderName(AiProvider provider) {
        if (provider == null) {
            return "unknown";
        }
        String name = provider.getName();
        return name == null || name.isBlank() ? provider.getClass().getSimpleName() : name;
    }

    private int countSteps(ScenarioAnalysisResult result) {
        return result.steps() == null ? 0 : result.steps().size();
    }

    private int countAmbiguities(ScenarioAnalysisResult result) {
        return result.ambiguities() == null ? 0 : result.ambiguities().size();
    }

    private void validateInput(String scenario, ProjectDiscoveryResult discovery) {
        if (scenario == null || scenario.trim().isEmpty()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        if (discovery == null) {
            throw new IllegalArgumentException("discovery must not be null");
        }
    }
}
