package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningParseException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningTechnicalException;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class PlanningService {

    private static final Logger log = LoggerFactory.getLogger(PlanningService.class);

    private final AiProviderResolver aiProviderResolver;
    private final PlanningInputSanitizer inputSanitizer;
    private final PlanningPromptFactory promptFactory;
    private final PlanningResponseParser responseParser;
    private final PlanningValidator validator;

    public PlanningService(AiProviderResolver aiProviderResolver,
                           PlanningInputSanitizer inputSanitizer,
                           PlanningPromptFactory promptFactory,
                           PlanningResponseParser responseParser,
                           PlanningValidator validator) {
        this.aiProviderResolver = Objects.requireNonNull(aiProviderResolver);
        this.inputSanitizer = Objects.requireNonNull(inputSanitizer);
        this.promptFactory = Objects.requireNonNull(promptFactory);
        this.responseParser = Objects.requireNonNull(responseParser);
        this.validator = Objects.requireNonNull(validator);
    }

    public TechnicalPlanResult plan(ProjectDiscoveryResult discovery,
                                     ScenarioAnalysisResult scenario,
                                     ProjectKnowledgeResult knowledge) {
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (scenario == null) throw new IllegalArgumentException("scenario must not be null");
        if (knowledge == null) throw new IllegalArgumentException("knowledge must not be null");
        if (scenario.status() == ScenarioAnalysisStatus.INVALID) {
            throw new PlanningValidationException("Cenário inválido não pode ser planejado");
        }
        if (knowledge.status() == KnowledgeStatus.FAILED) {
            throw new PlanningValidationException("Knowledge FAILED não pode ser planejado");
        }

        log.info("Planning started");

        SanitizedPlanningInput sanitized = inputSanitizer.sanitize(discovery, scenario, knowledge);
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitized);

        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Planning provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Planning fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        return planWithFallback(activeProvider, fallbackProvider, sameProvider, systemPrompt, userPrompt, discovery, scenario, knowledge);
    }

    private TechnicalPlanResult planWithFallback(AiProvider active, AiProvider fallback, boolean same,
                                                  String sys, String user,
                                                  ProjectDiscoveryResult discovery,
                                                  ScenarioAnalysisResult scenario,
                                                  ProjectKnowledgeResult knowledge) {
        try {
            TechnicalPlanResult result = planWithProvider(active, sys, user, discovery, scenario, knowledge);
            log.debug("Planning concluído. provider={}", safeProviderName(active));
            return result;
        } catch (PlanningValidationException e) {
            throw e;
        } catch (PlanningTechnicalException | PlanningParseException primary) {
            if (same) {
                log.error("Planning failed without fallback. provider={}, failureType={}, status=FAILED",
                    safeProviderName(active), primary.getClass().getSimpleName());
                throw new PlanningTechnicalException("Falha técnica no planejamento", primary);
            }
            log.warn("Planning primário falhou, tentando fallback. primaryProvider={}, fallbackProvider={}, failureType={}, failureMessage='{}'",
                safeProviderName(active), safeProviderName(fallback),
                primary.getClass().getSimpleName(), primary.getMessage());
            return planWithFallbackProvider(active, fallback, sys, user, primary, discovery, scenario, knowledge);
        }
    }

    private TechnicalPlanResult planWithFallbackProvider(AiProvider active, AiProvider fallback,
                                                          String sys, String user,
                                                          Exception primaryFailure,
                                                          ProjectDiscoveryResult discovery,
                                                          ScenarioAnalysisResult scenario,
                                                          ProjectKnowledgeResult knowledge) {
        try {
            TechnicalPlanResult result = planWithProvider(fallback, sys, user, discovery, scenario, knowledge);
            log.debug("Planning concluído via fallback. provider={}", safeProviderName(fallback));
            return result;
        } catch (PlanningValidationException e) {
            throw e;
        } catch (PlanningTechnicalException | PlanningParseException fallbackFailure) {
            log.error("Planning failed after fallback. primaryProvider={}, fallbackProvider={}, status=FAILED",
                safeProviderName(active), safeProviderName(fallback));
            PlanningTechnicalException ex = new PlanningTechnicalException("Falha técnica no planejamento", fallbackFailure);
            ex.addSuppressed(primaryFailure);
            throw ex;
        }
    }

    private TechnicalPlanResult planWithProvider(AiProvider provider, String systemPrompt, String userPrompt,
                                                  ProjectDiscoveryResult discovery,
                                                  ScenarioAnalysisResult scenario,
                                                  ProjectKnowledgeResult knowledge) {
        String response;
        try {
            response = provider.gerarResposta(systemPrompt, userPrompt);
        } catch (RuntimeException e) {
            throw new PlanningTechnicalException("Falha técnica no provider " + provider.getName(), e);
        }
        TechnicalPlanResult result = responseParser.parse(response);
        return validator.validate(result, discovery, scenario, knowledge);
    }

    private boolean sameProvider(AiProvider a, AiProvider b) {
        return a == b || Objects.equals(normalizeProviderName(a), normalizeProviderName(b));
    }

    private String normalizeProviderName(AiProvider p) {
        return p == null ? null : safeProviderName(p).trim();
    }

    private String safeProviderName(AiProvider p) {
        if (p == null) return "unknown";
        String n = p.getName();
        return n == null || n.isBlank() ? p.getClass().getSimpleName() : n;
    }
}
