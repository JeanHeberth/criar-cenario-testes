package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.failure.exception.*;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.*;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FailureAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FailureAnalysisService.class);

    private final ExecutionEvidenceExtractor extractor = new ExecutionEvidenceExtractor();
    private final FailureClassifier classifier = new FailureClassifier();
    private final FailureAnalysisInputSanitizer sanitizer = new FailureAnalysisInputSanitizer();
    private final FailureSummaryBuilder summaryBuilder = new FailureSummaryBuilder();
    private final AiProviderResolver aiProviderResolver;
    private final FailureAnalysisResponseParser parser = new FailureAnalysisResponseParser();
    private final FailureAnalysisPromptFactory promptFactory = new FailureAnalysisPromptFactory();
    private final FailureAnalysisValidator validator = new FailureAnalysisValidator();

    public FailureAnalysisService(AiProviderResolver aiProviderResolver) {
        this.aiProviderResolver = aiProviderResolver;
    }

    public FailureAnalysisResult analyze(
            UUID executionId,
            ProjectDiscoveryResult discovery,
            ScenarioAnalysisResult scenario,
            ProjectKnowledgeResult knowledge,
            TechnicalPlanResult plan,
            GenerationResult generation,
            CodeReviewResult review,
            ApplyResult applyResult,
            ExecutionResult execution
    ) {
        Objects.requireNonNull(executionId);
            Objects.requireNonNull(discovery);
            Objects.requireNonNull(scenario);
            Objects.requireNonNull(knowledge);
            Objects.requireNonNull(plan);
            Objects.requireNonNull(generation);
            Objects.requireNonNull(review);
            Objects.requireNonNull(applyResult);
            Objects.requireNonNull(execution);

            // PASSED
            if (execution.status().name().equals("PASSED")) {
                return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(), FailureAnalysisStatus.NO_FAILURE, FailureConfidence.HIGH, false, false, false, true);
            }

            // Operational statuses
            switch (execution.status()) {
                case ERROR, BLOCKED, TIMED_OUT, CANCELLED:
                    FailureWarning w = new FailureWarning("OPERATIONAL", "Execution status " + execution.status());
                    return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(w), FailureAnalysisStatus.BLOCKED, FailureConfidence.UNKNOWN, true, false, false, true);
                default:
            }

            // FAILED -> extract evidence and classify
            List<FailureEvidence> evidence = extractor.extract(execution);
            if (evidence == null) evidence = List.of();
            List<FailureFinding> deterministic = classifier.classify(evidence);
            if (deterministic == null) deterministic = List.of();

            // if any deterministic finding with HIGH confidence and concrete evidence, materialize deterministically
            boolean hasHighDeterministic = deterministic.stream().anyMatch(f -> f.confidence() == FailureConfidence.HIGH && f.category() != FailureCategory.UNKNOWN && !f.evidence().isEmpty());
            if (hasHighDeterministic) {
                List<FailureEvidence> sanitized = sanitizer.sanitizeEvidence(evidence);
                FailureAnalysisResult result = summaryBuilder.summarize(executionId, execution, deterministic, List.of(), sanitized);
                validator.validate(result, execution, deterministic);
                return result;
            }

            // otherwise attempt IA with fallback
            AiProvider active = aiProviderResolver == null ? null : aiProviderResolver.getActiveProvider();
            AiProvider fallback = aiProviderResolver == null ? null : aiProviderResolver.getFallbackProvider();

            if (active == null) {
                FailureWarning w = new FailureWarning("NO_PROVIDER", "No AI provider available for analysis");
                return new FailureAnalysisResult(executionId, List.copyOf(deterministic), List.of(), List.copyOf(evidence), List.of(w), FailureAnalysisStatus.INCONCLUSIVE, FailureConfidence.LOW, true, false, false, true);
            }

            List<FailureFinding> aiFindings = List.of();
            FailureAnalysisAiResponse aiResp = null;
            String systemPrompt = promptFactory.systemPrompt();
            String userPrompt = promptFactory.userPrompt(executionId, sanitizer.sanitizeEvidence(evidence), deterministic);

            /**
             * Falha da IA aqui é AVISO, não erro fatal. Dois motivos:
             *
             * 1. A análise já foi feita: 'deterministic' sai da execução real dos
             *    testes. A IA seria enriquecimento — e hoje nem isso, porque
             *    aiFindings é forçado a List.of() logo abaixo, ou seja, a resposta
             *    é parseada e descartada.
             * 2. Quando ela derrubava o estágio, um workflow que já tinha gerado,
             *    aplicado e EXECUTADO os testes terminava como FAILED. Perdia-se o
             *    resultado de nove etapas porque o comentário sobre a falha não veio.
             *
             * O caminho NO_PROVIDER logo acima já seguia exatamente esta política.
             */
            String falhaIa = null;
            AiProvider triedFallback = null;
            AiProvider primary = active;
            AiProvider secondary = fallback;

            // attempt primary then fallback on technical/parse failures
            for (int attempt = 0; attempt < 2; attempt++) {
                AiProvider provider = (attempt == 0) ? primary : secondary;
                if (provider == null) break;
                if (provider == primary && attempt == 1) break; // no fallback same provider
                if (provider.equals(primary) && secondary != null && provider.equals(secondary)) break;
                try {
                    String response = provider.gerarResposta(systemPrompt, userPrompt);
                    aiResp = parser.parse(response);
                    break; // success
                } catch (Exception ex) {
                    // only allow fallback for technical/parse exceptions
                    if (ex instanceof com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisParseException || ex instanceof com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisTechnicalException || responseBlank(ex)) {
                        // try fallback if available and different
                        if (attempt == 0 && secondary != null && !secondary.equals(primary)) {
                            triedFallback = secondary;
                            continue;
                        }
                        falhaIa = descreverFalha(provider, ex);
                        break;
                    } else {
                        // do not fallback for validation/semantic issues
                        falhaIa = descreverFalha(provider, ex);
                        break;
                    }
                }
            }

            if (falhaIa != null) {
                log.warn("Failure analysis seguindo só com análise determinística. executionId={}, motivo='{}'",
                        executionId, falhaIa);
                return new FailureAnalysisResult(executionId, List.copyOf(deterministic), List.of(),
                        List.copyOf(sanitizer.sanitizeEvidence(evidence)),
                        List.of(new FailureWarning("AI_ANALYSIS_UNAVAILABLE", falhaIa)),
                        FailureAnalysisStatus.INCONCLUSIVE, FailureConfidence.LOW, true, false, false, true);
            }

            List<FailureEvidence> sanitized = sanitizer.sanitizeEvidence(evidence);
            if (aiResp != null) {
                // materialize aiFindings conservatively: parser currently yields Object lists; for phase10 we treat as none
                aiFindings = List.of();
            }

            FailureAnalysisResult result = summaryBuilder.summarize(executionId, execution, deterministic, aiFindings, sanitized);
            // validate final
            validator.validate(result, execution, deterministic);
            return result;
    }

    private boolean responseBlank(Exception ex) {
        String msg = ex.getMessage();
        return msg != null && (msg.contains("blank") || msg.contains("null") || msg.contains("too large"));
    }

    /**
     * Nomeia o provider e a causa real. A mensagem antiga ("AI provider failed
     * with non-retryable error") aparecia na tela sem dizer QUAL provider nem
     * POR QUÊ — e a causa concreta, "API key not valid", só existia no
     * stacktrace do servidor. Chave inválida é problema de configuração: quem
     * lê o erro precisa saber disso para agir.
     */
    private String descreverFalha(AiProvider provider, Exception ex) {
        String nome = provider == null ? "desconhecido" : provider.getName();
        String causa = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        if (causa.contains("API key not valid") || causa.contains("API_KEY_INVALID")) {
            return "Provider de IA '" + nome + "' recusou a chave de API (verifique a configuração da key)";
        }
        return "Falha no provider de IA '" + nome + "': " + resumir(causa);
    }

    private String resumir(String texto) {
        String linha = texto.lines().findFirst().orElse(texto);
        return linha.length() <= 200 ? linha : linha.substring(0, 200) + "...";
    }
}
