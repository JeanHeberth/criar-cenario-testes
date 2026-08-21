package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAmbiguity;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ScenarioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioAnalysisService.class);

    private final AiProviderResolver aiProviderResolver;
    private final AutomationTypeResolver automationTypeResolver = new AutomationTypeResolver();
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
        return analyze(scenario, discovery, null);
    }

    public ScenarioAnalysisResult analyze(String scenario, ProjectDiscoveryResult discovery,
                                           AutomationType informedAutomationType) {
        validateInput(scenario, discovery);

        String sanitizedScenario = inputSanitizer.sanitize(scenario);
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitizedScenario, discovery, informedAutomationType);
        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Scenario analysis provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Scenario analysis fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        try {
            return analyzeWithFallback(activeProvider, fallbackProvider, sameProvider, systemPrompt, userPrompt,
                    discovery, informedAutomationType);
        } catch (ScenarioAnalysisValidationException exception) {
            throw exception;
        }
    }

    private ScenarioAnalysisResult analyzeWithFallback(AiProvider activeProvider,
                                                       AiProvider fallbackProvider,
                                                       boolean sameProvider,
                                                       String systemPrompt,
                                                       String userPrompt,
                                                       ProjectDiscoveryResult discovery,
                                                       AutomationType informedAutomationType) {
        try {
            ScenarioAnalysisResult result = analyzeWithProvider(activeProvider, systemPrompt, userPrompt, discovery,
                    informedAutomationType);
            log.debug("Scenario analysis concluída. provider={}, passos={}, ambiguidades={}",
                    safeProviderName(activeProvider), countSteps(result), countAmbiguities(result));
            return result;
        } catch (ScenarioAnalysisTechnicalException primaryFailure) {
            if (sameProvider) {
                log.error("Scenario analysis failed without fallback. provider={}, failureType={}, status=FAILED",
                        safeProviderName(activeProvider), primaryFailure.getClass().getSimpleName());
                throw new ScenarioAnalysisTechnicalException("Falha técnica na análise de cenário", primaryFailure);
            }
            return analyzeWithFallbackProvider(activeProvider, fallbackProvider, systemPrompt, userPrompt, discovery,
                    informedAutomationType, primaryFailure);
        }
    }

    private ScenarioAnalysisResult analyzeWithFallbackProvider(AiProvider activeProvider,
                                                               AiProvider fallbackProvider,
                                                               String systemPrompt,
                                                               String userPrompt,
                                                               ProjectDiscoveryResult discovery,
                                                               AutomationType informedAutomationType,
                                                               ScenarioAnalysisTechnicalException primaryFailure) {
        try {
            ScenarioAnalysisResult result = analyzeWithProvider(fallbackProvider, systemPrompt, userPrompt, discovery,
                    informedAutomationType);
            log.debug("Scenario analysis concluída. provider={}, passos={}, ambiguidades={}",
                    safeProviderName(fallbackProvider), countSteps(result), countAmbiguities(result));
            return result;
        } catch (ScenarioAnalysisValidationException exception) {
            throw exception;
        } catch (ScenarioAnalysisTechnicalException fallbackFailure) {
            // Só o NOME da exceção não diz por que falhou, e a resposta da IA
            // muda a cada geração: sem a mensagem da causa, reproduzir para
            // diagnosticar custa uma rodada inteira de chamadas.
            log.error("Scenario analysis failed after fallback. primaryProvider={}, fallbackProvider={}, primaryFailureType={}, primaryFailureMessage='{}', primaryFailureCause='{}', fallbackFailureType={}, fallbackFailureMessage='{}', status=FAILED",
                    safeProviderName(activeProvider),
                    safeProviderName(fallbackProvider),
                    primaryFailure.getClass().getSimpleName(),
                    primaryFailure.getMessage(),
                    primaryFailure.getCause() == null ? null : primaryFailure.getCause().getMessage(),
                    fallbackFailure.getClass().getSimpleName(),
                    fallbackFailure.getMessage());
            ScenarioAnalysisTechnicalException technicalException =
                    new ScenarioAnalysisTechnicalException("Falha técnica na análise de cenário", fallbackFailure);
            technicalException.addSuppressed(primaryFailure);
            throw technicalException;
        }
    }

    private ScenarioAnalysisResult analyzeWithProvider(AiProvider provider, String systemPrompt, String userPrompt,
                                                       ProjectDiscoveryResult discovery,
                                                       AutomationType informedAutomationType) {
        String response;
        try {
            response = provider.gerarResposta(systemPrompt, userPrompt);
        } catch (RuntimeException exception) {
            throw new ScenarioAnalysisTechnicalException("Falha técnica no provider " + provider.getName(), exception);
        }

        ScenarioAnalysisResult result = responseParser.parse(response);
        return validator.validate(completarComContextoDoProjeto(result, discovery, informedAutomationType));
    }

    /**
     * A IA responde UNKNOWN em automationType com frequência, e com razão: o
     * texto do cenário não diz se o teste será por UI ou API. O discovery já
     * sabe, porque inspecionou o projeto — usar esse fato evita reprovar uma
     * análise boa por um campo que temos de forma determinística.
     */
    private ScenarioAnalysisResult completarComContextoDoProjeto(ScenarioAnalysisResult result,
                                                                 ProjectDiscoveryResult discovery,
                                                                 AutomationType informedAutomationType) {
        if (result == null || discovery == null) {
            return result;
        }

        AutomationType resolvido = automationTypeResolver.resolver(
                result.automationType(), discovery, informedAutomationType);

        // status e valid são consequência determinística das ambiguidades e
        // warnings que a própria análise trouxe - não há informação nova em
        // pedir que o modelo também acerte o rótulo, e errá-lo descartava a
        // análise inteira. Continuamos desconfiando do CONTEÚDO (que segue
        // pela validação); só o rótulo passa a ser calculado.
        boolean bloqueante = result.ambiguities().stream().anyMatch(ScenarioAmbiguity::blocking);
        boolean temAvisos = !result.warnings().isEmpty() || !result.ambiguities().isEmpty();

        ScenarioAnalysisStatus status;
        boolean valido;
        if (bloqueante) {
            status = ScenarioAnalysisStatus.INVALID;
            valido = false;
        } else if (temAvisos) {
            status = ScenarioAnalysisStatus.VALID_WITH_WARNINGS;
            valido = true;
        } else {
            status = ScenarioAnalysisStatus.VALID;
            valido = true;
        }

        if (resolvido == result.automationType()
                && status == result.status()
                && valido == result.valid()) {
            return result;
        }

        return new ScenarioAnalysisResult(
                result.title(), result.objective(), result.preconditions(), result.steps(),
                result.testData(), result.businessRules(), result.risks(), result.ambiguities(),
                result.entities(), result.dependencies(), resolvido, status,
                result.warnings(), valido);
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
