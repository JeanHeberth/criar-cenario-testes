package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningTechnicalException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestrador stateless da Fase 11. Sem persistência, sem I/O, sem alteração
 * de agentes/regras anteriores. Determinístico primeiro: a IA só é chamada
 * quando há evidência conflitante, tipo indeterminado ou confidence LOW —
 * nunca para reformular texto ou quando já existe determinístico HIGH/MEDIUM
 * coerente.
 */
public class LearningService {

    private static final Set<ExecutionStatus> OPERATIONAL_EXECUTION_STATUSES =
            EnumSet.of(ExecutionStatus.ERROR, ExecutionStatus.BLOCKED, ExecutionStatus.TIMED_OUT, ExecutionStatus.CANCELLED);
    private static final Set<ApplyStatus> BLOCKING_APPLY_STATUSES =
            EnumSet.of(ApplyStatus.FAILED, ApplyStatus.ROLLED_BACK);

    private final LearningEvidenceExtractor extractor = new LearningEvidenceExtractor();
    private final PositiveLearningCollector positiveCollector = new PositiveLearningCollector();
    private final NegativeLearningCollector negativeCollector = new NegativeLearningCollector();
    private final LearningConfidenceResolver confidenceResolver = new LearningConfidenceResolver();
    private final LearningDeduplicator deduplicator = new LearningDeduplicator();
    private final LearningInputSanitizer sanitizer = new LearningInputSanitizer();
    private final LearningPromptFactory promptFactory = new LearningPromptFactory();
    private final LearningResponseParser parser = new LearningResponseParser();
    private final LearningSummaryBuilder summaryBuilder = new LearningSummaryBuilder();
    private final LearningValidator validator = new LearningValidator();
    private final AiProviderResolver aiProviderResolver;

    public LearningService(AiProviderResolver aiProviderResolver) {
        this.aiProviderResolver = aiProviderResolver;
    }

    public LearningResult learn(
            UUID executionId,
            ProjectDiscoveryResult discovery,
            ScenarioAnalysisResult scenario,
            ProjectKnowledgeResult knowledge,
            TechnicalPlanResult plan,
            GenerationResult generation,
            CodeReviewResult review,
            ApplyResult applyResult,
            ExecutionResult execution,
            FailureAnalysisResult failureAnalysis
    ) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(generation, "generation must not be null");
        Objects.requireNonNull(review, "review must not be null");
        Objects.requireNonNull(applyResult, "applyResult must not be null");
        Objects.requireNonNull(execution, "execution must not be null");
        Objects.requireNonNull(failureAnalysis, "failureAnalysis must not be null");

        if (isOperationalBlocked(execution, applyResult, review, failureAnalysis)) {
            LearningResult blocked = summaryBuilder.summarize(executionId, execution.status(), true,
                    List.of(), List.of(), List.of());
            return validator.validate(blocked, List.of());
        }

        List<LearningEvidence> evidence = extractor.extract(discovery, scenario, knowledge, plan, generation,
                review, applyResult, execution, failureAnalysis);

        List<LearningItem> rawDeterministic = new ArrayList<>();
        rawDeterministic.addAll(positiveCollector.collect(execution, review, applyResult, evidence));
        rawDeterministic.addAll(negativeCollector.collect(execution, failureAnalysis, evidence));

        LearningDeduplicator.Result selfDedup = deduplicator.deduplicate(rawDeterministic, List.of());
        List<LearningItem> resolvedDeterministic = selfDedup.items().stream()
                .map(confidenceResolver::resolve)
                .toList();

        if (resolvedDeterministic.isEmpty()) {
            LearningResult skipped = summaryBuilder.summarize(executionId, execution.status(), false,
                    List.of(), List.copyOf(evidence), List.of());
            return validator.validate(skipped, List.of());
        }

        boolean hadConflict = !selfDedup.warnings().isEmpty();
        List<LearningItem> aiItems = List.of();
        List<LearningWarning> aiWarnings = List.of();
        if (needsAi(resolvedDeterministic, hadConflict)) {
            List<LearningEvidence> sanitizedEvidenceForPrompt = sanitizer.sanitizeEvidence(evidence);
            List<LearningItem> sanitizedItemsForPrompt = sanitizer.sanitizeItems(resolvedDeterministic);
            AiOutcome outcome = callAi(executionId, sanitizedItemsForPrompt, sanitizedEvidenceForPrompt, selfDedup.warnings());
            aiItems = outcome.items();
            aiWarnings = outcome.warnings();
        }

        LearningDeduplicator.Result finalDedup = deduplicator.deduplicate(resolvedDeterministic, aiItems);
        List<LearningWarning> warnings = new ArrayList<>(selfDedup.warnings());
        warnings.addAll(finalDedup.warnings());
        warnings.addAll(aiWarnings);

        List<LearningEvidence> sanitizedFinalEvidence = sanitizer.sanitizeEvidence(evidence);
        List<LearningItem> sanitizedFinalItems = sanitizer.sanitizeItems(finalDedup.items());

        LearningResult summarized = summaryBuilder.summarize(executionId, execution.status(), false,
                sanitizedFinalItems, sanitizedFinalEvidence, warnings);
        return validator.validate(summarized, resolvedDeterministic);
    }

    private boolean isOperationalBlocked(ExecutionResult execution, ApplyResult applyResult, CodeReviewResult review,
                                          FailureAnalysisResult failureAnalysis) {
        if (OPERATIONAL_EXECUTION_STATUSES.contains(execution.status())) {
            return true;
        }
        if (BLOCKING_APPLY_STATUSES.contains(applyResult.status())) {
            return true;
        }
        if (review.status() == ReviewStatus.INVALID) {
            return true;
        }
        return failureAnalysis.status() == FailureAnalysisStatus.INVALID || !failureAnalysis.valid();
    }

    private boolean needsAi(List<LearningItem> items, boolean hadConflict) {
        if (hadConflict) {
            return true;
        }
        return items.stream().anyMatch(i -> i.confidence() == LearningConfidence.LOW
                || i.confidence() == LearningConfidence.UNKNOWN
                || i.type() == LearningType.UNKNOWN);
    }

    /** Resultado interno da chamada à IA: itens materializados e warnings a propagar ao LearningResult final. */
    private record AiOutcome(List<LearningItem> items, List<LearningWarning> warnings) {
        private static final AiOutcome EMPTY = new AiOutcome(List.of(), List.of());
    }

    private AiOutcome callAi(UUID executionId, List<LearningItem> sanitizedItems,
                              List<LearningEvidence> sanitizedEvidence, List<LearningWarning> priorWarnings) {
        if (aiProviderResolver == null) {
            return AiOutcome.EMPTY;
        }
        AiProvider primary;
        try {
            primary = aiProviderResolver.getActiveProvider();
        } catch (RuntimeException ex) {
            return AiOutcome.EMPTY;
        }
        if (primary == null) {
            return AiOutcome.EMPTY;
        }
        AiProvider secondary;
        try {
            secondary = aiProviderResolver.getFallbackProvider();
        } catch (RuntimeException ex) {
            secondary = null;
        }
        boolean sameProvider = secondary != null
                && (secondary == primary || Objects.equals(secondary.getName(), primary.getName()));

        List<AiProvider> attempts = new ArrayList<>();
        attempts.add(primary);
        if (secondary != null && !sameProvider) {
            attempts.add(secondary);
        }

        String systemPrompt = promptFactory.systemPrompt();
        String userPrompt = promptFactory.userPrompt(executionId, sanitizedItems, sanitizedEvidence, priorWarnings);

        LearningAiResponse response = null;
        Exception lastError = null;
        for (AiProvider provider : attempts) {
            try {
                String raw = provider.gerarResposta(systemPrompt, userPrompt);
                LearningAiResponse parsed = parser.parse(raw);
                rejectForbiddenScopes(parsed);
                response = parsed;
                lastError = null;
                break;
            } catch (com.br.criarcenariotestes.business.autoqa.learning.exception.LearningValidationException semanticFailure) {
                // Falha semântica (ex.: IA sugeriu escopo GLOBAL/TEAM): nunca usa fallback,
                // nunca combina parcialmente a resposta — descarta a resposta inteira, preserva
                // apenas os itens determinísticos já coletados no fluxo anterior, e registra um
                // warning controlado (sem JSON bruto, prompt, resposta da IA, paths ou dado sensível).
                LearningWarning warning = new LearningWarning("UNSUPPORTED_SCOPE",
                        "Resposta da IA rejeitada: sugeriu escopo não suportado nesta fase", false);
                return new AiOutcome(List.of(), List.of(warning));
            } catch (Exception ex) {
                lastError = ex;
            }
        }
        if (response == null) {
            throw new LearningTechnicalException("AI provider failed after fallback", lastError);
        }
        return new AiOutcome(materialize(response), List.of());
    }

    /**
     * A IA não pode promover nenhum item para escopo GLOBAL ou TEAM nesta
     * fase. Se a resposta contiver qualquer item nesses escopos, a resposta
     * inteira é rejeitada (não apenas o item ofensor) — nunca materializada,
     * nunca combinada parcialmente com os demais itens sugeridos.
     */
    private void rejectForbiddenScopes(LearningAiResponse response) {
        if (response.items() == null) {
            return;
        }
        for (LearningAiResponse.Item item : response.items()) {
            if (item == null) {
                continue;
            }
            if ("GLOBAL".equals(item.scope()) || "TEAM".equals(item.scope())) {
                throw new com.br.criarcenariotestes.business.autoqa.learning.exception.LearningValidationException(
                        "AI suggested forbidden scope: " + item.scope());
            }
        }
    }

    private List<LearningItem> materialize(LearningAiResponse response) {
        if (response.items() == null) {
            return List.of();
        }
        List<LearningItem> result = new ArrayList<>();
        for (LearningAiResponse.Item aiItem : response.items()) {
            if (aiItem == null) {
                continue;
            }
            LearningItem materialized = materializeItem(aiItem);
            if (materialized != null) {
                result.add(materialized);
            }
        }
        return List.copyOf(result);
    }

    private LearningItem materializeItem(LearningAiResponse.Item aiItem) {
        if (isBlank(aiItem.title()) || isBlank(aiItem.description()) || isBlank(aiItem.recommendation())) {
            return null;
        }
        LearningType type = parseEnum(aiItem.type(), LearningType.class, LearningType.UNKNOWN);
        LearningScope scope = parseEnum(aiItem.scope(), LearningScope.class, null);
        if (scope == null || scope == LearningScope.GLOBAL || scope == LearningScope.TEAM) {
            return null;
        }
        List<String> relatedComponents = safeList(aiItem.relatedComponents());
        List<String> relatedFiles = safeList(aiItem.relatedFiles());
        List<String> tags = safeList(aiItem.tags());
        String title = aiItem.title().trim();
        String id = LearningIdGenerator.generate(type, scope, title, relatedComponents, relatedFiles);
        boolean positive = !isNegativeType(type);

        return new LearningItem(id, type, scope, LearningSource.AI_SUGGESTION, title, aiItem.description().trim(),
                aiItem.recommendation().trim(), LearningConfidence.LOW, LearningApprovalStatus.PENDING, List.of(),
                relatedComponents, relatedFiles, List.of(), tags, positive, false, true);
    }

    private boolean isNegativeType(LearningType type) {
        return type == LearningType.FAILURE_PATTERN || type == LearningType.ANTI_PATTERN
                || type == LearningType.RISK_PATTERN || type == LearningType.RETRY_STRATEGY
                || type == LearningType.REGENERATION_STRATEGY;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
