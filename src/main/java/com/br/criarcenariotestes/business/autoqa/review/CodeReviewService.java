package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewParseException;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewTechnicalException;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final AiProviderResolver aiProviderResolver;
    private final GeneratedArtifactReader artifactReader;
    private final StaticReviewRuleEngine ruleEngine;
    private final CodeReviewInputSanitizer inputSanitizer;
    private final CodeReviewPromptFactory promptFactory;
    private final CodeReviewResponseParser responseParser;
    private final CodeReviewValidator validator;
    private final ReviewSummaryBuilder summaryBuilder;

    public CodeReviewService(AiProviderResolver aiProviderResolver,
                              GeneratedArtifactReader artifactReader,
                              StaticReviewRuleEngine ruleEngine,
                              CodeReviewInputSanitizer inputSanitizer,
                              CodeReviewPromptFactory promptFactory,
                              CodeReviewResponseParser responseParser,
                              CodeReviewValidator validator,
                              ReviewSummaryBuilder summaryBuilder) {
        this.aiProviderResolver = Objects.requireNonNull(aiProviderResolver);
        this.artifactReader = Objects.requireNonNull(artifactReader);
        this.ruleEngine = Objects.requireNonNull(ruleEngine);
        this.inputSanitizer = Objects.requireNonNull(inputSanitizer);
        this.promptFactory = Objects.requireNonNull(promptFactory);
        this.responseParser = Objects.requireNonNull(responseParser);
        this.validator = Objects.requireNonNull(validator);
        this.summaryBuilder = Objects.requireNonNull(summaryBuilder);
    }

    public CodeReviewResult review(UUID executionId,
                                    ProjectDiscoveryResult discovery,
                                    ScenarioAnalysisResult scenario,
                                    ProjectKnowledgeResult knowledge,
                                    TechnicalPlanResult plan,
                                    GenerationResult generation) {
        if (executionId == null) throw new IllegalArgumentException("executionId must not be null");
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (scenario == null) throw new IllegalArgumentException("scenario must not be null");
        if (knowledge == null) throw new IllegalArgumentException("knowledge must not be null");
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        if (generation == null) throw new IllegalArgumentException("generation must not be null");

        if (scenario.status() == ScenarioAnalysisStatus.INVALID) {
            throw new CodeReviewValidationException("Cenário inválido não pode ser revisado");
        }
        if (knowledge.status() == KnowledgeStatus.FAILED) {
            throw new CodeReviewValidationException("Knowledge FAILED não pode ser revisado");
        }
        if (plan.status() != PlanningStatus.READY && plan.status() != PlanningStatus.READY_WITH_WARNINGS) {
            throw new CodeReviewValidationException("Plano deve estar READY ou READY_WITH_WARNINGS para revisão");
        }
        if (generation.status() == GenerationStatus.PARTIAL || generation.status() == GenerationStatus.FAILED) {
            throw new CodeReviewValidationException("Generation PARTIAL/FAILED não pode ser revisada");
        }

        log.info("Review started. executionId={}", executionId);

        List<GeneratedArtifactReader.ReadArtifact> artifacts = artifactReader.readAll(executionId, generation);

        List<ReviewIssue> integrityIssues = artifacts.stream()
                .filter(a -> !a.hashMatches())
                .map(a -> new ReviewIssue(ReviewRule.CONTENT_INTEGRITY_MISMATCH.name(), ReviewCategory.SECURITY, ReviewSeverity.CRITICAL,
                        a.relativePath(), null, "Conteúdo físico diverge do hash registrado na geração", null,
                        "Regenerar o arquivo — o conteúdo pode ter sido alterado após a geração", true))
                .toList();
        if (!integrityIssues.isEmpty()) {
            log.error("Review blocked: content integrity mismatch. executionId={}, files={}", executionId, integrityIssues.size());
            return buildBlockedResult(executionId, generation, integrityIssues);
        }

        List<ReviewIssue> staticIssues = ruleEngine.review(
                discovery.getAutomationFramework(), discovery.getLanguage(), plan, generation, artifacts);

        boolean hasCriticalStatic = staticIssues.stream().anyMatch(i -> i.severity() == ReviewSeverity.CRITICAL);
        if (hasCriticalStatic) {
            log.warn("Review blocked by static CRITICAL issue. executionId={}", executionId);
            return buildBlockedResult(executionId, generation, staticIssues);
        }

        log.debug("Review static issues found. executionId={}, count={}", executionId, staticIssues.size());

        SanitizedCodeReviewInput sanitized = inputSanitizer.sanitize(discovery, scenario, knowledge, plan, artifacts, staticIssues);
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitized);

        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Review provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Review fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        CodeReviewAiResponse aiResponse = reviewWithFallback(activeProvider, fallbackProvider, sameProvider,
                systemPrompt, userPrompt, discovery, scenario, knowledge, plan, generation);

        CodeReviewResult result = materialize(executionId, generation, staticIssues, aiResponse);
        log.info("Review finished. executionId={}, status={}, files={}, totalIssues={}",
                executionId, result.status(), result.files().size(),
                result.globalIssues().size() + result.files().stream().mapToLong(f -> f.issues().size()).sum());
        return result;
    }

    private CodeReviewAiResponse reviewWithFallback(AiProvider active, AiProvider fallback, boolean same,
                                                      String sys, String user,
                                                      ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                      ProjectKnowledgeResult knowledge, TechnicalPlanResult plan,
                                                      GenerationResult generation) {
        try {
            CodeReviewAiResponse result = reviewWithProvider(active, sys, user, discovery, scenario, knowledge, plan, generation);
            log.debug("Review concluída. provider={}", safeProviderName(active));
            return result;
        } catch (CodeReviewValidationException e) {
            throw e;
        } catch (CodeReviewTechnicalException | CodeReviewParseException primary) {
            if (same) {
                log.error("Review failed without fallback. provider={}, failureType={}", safeProviderName(active), primary.getClass().getSimpleName());
                throw new CodeReviewTechnicalException("Falha técnica na revisão", primary);
            }
            log.warn("Review primária falhou, tentando fallback. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(active), safeProviderName(fallback));
            return reviewWithFallbackProvider(active, fallback, sys, user, primary, discovery, scenario, knowledge, plan, generation);
        }
    }

    private CodeReviewAiResponse reviewWithFallbackProvider(AiProvider active, AiProvider fallback,
                                                              String sys, String user, Exception primaryFailure,
                                                              ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                              ProjectKnowledgeResult knowledge, TechnicalPlanResult plan,
                                                              GenerationResult generation) {
        try {
            CodeReviewAiResponse result = reviewWithProvider(fallback, sys, user, discovery, scenario, knowledge, plan, generation);
            log.debug("Review concluída via fallback. provider={}", safeProviderName(fallback));
            return result;
        } catch (CodeReviewValidationException e) {
            throw e;
        } catch (CodeReviewTechnicalException | CodeReviewParseException fallbackFailure) {
            log.error("Review failed after fallback. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(active), safeProviderName(fallback));
            CodeReviewTechnicalException ex = new CodeReviewTechnicalException("Falha técnica na revisão", fallbackFailure);
            ex.addSuppressed(primaryFailure);
            throw ex;
        }
    }

    private CodeReviewAiResponse reviewWithProvider(AiProvider provider, String systemPrompt, String userPrompt,
                                                      ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                      ProjectKnowledgeResult knowledge, TechnicalPlanResult plan,
                                                      GenerationResult generation) {
        String response;
        try {
            response = provider.gerarResposta(systemPrompt, userPrompt);
        } catch (RuntimeException e) {
            throw new CodeReviewTechnicalException("Falha técnica no provider " + provider.getName(), e);
        }
        CodeReviewAiResponse parsed = responseParser.parse(response);
        return validator.validate(parsed, discovery, scenario, knowledge, plan, generation);
    }

    private CodeReviewResult materialize(UUID executionId, GenerationResult generation,
                                          List<ReviewIssue> staticIssues, CodeReviewAiResponse aiResponse) {
        if (aiResponse.status() == ReviewStatus.INVALID) {
            return new CodeReviewResult(executionId, List.of(), aiResponse.globalIssues(), aiResponse.suggestions(),
                    aiResponse.passedRules(), aiResponse.skippedRules(), aiResponse.warnings(),
                    ReviewStatus.INVALID, aiResponse.confidence(), true, false);
        }

        Map<String, List<ReviewIssue>> staticByPath = staticIssues.stream()
                .filter(i -> i.relativePath() != null)
                .collect(Collectors.groupingBy(ReviewIssue::relativePath, LinkedHashMap::new, Collectors.toList()));

        Map<String, CodeReviewAiResponse.AiFileReview> aiByPath = new LinkedHashMap<>();
        for (CodeReviewAiResponse.AiFileReview fileReview : aiResponse.files()) {
            if (fileReview != null && fileReview.relativePath() != null) {
                aiByPath.putIfAbsent(fileReview.relativePath(), fileReview);
            }
        }

        List<FileReviewResult> finalFiles = new ArrayList<>();
        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() == GeneratedFileOperation.REUSE || file.operation() == GeneratedFileOperation.NONE) {
                finalFiles.add(new FileReviewResult(file.relativePath(), file.operation(), file.componentType(),
                        FileReviewStatus.SKIPPED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.UNKNOWN, true));
                continue;
            }

            List<ReviewIssue> statics = staticByPath.getOrDefault(file.relativePath(), List.of());
            CodeReviewAiResponse.AiFileReview aiFile = aiByPath.get(file.relativePath());
            List<ReviewIssue> aiIssues = aiFile != null ? aiFile.issues() : List.of();
            List<ReviewIssue> merged = mergeIssues(statics, aiIssues);

            FileReviewStatus status = summaryBuilder.deriveFileStatus(merged);
            finalFiles.add(new FileReviewResult(
                    file.relativePath(), file.operation(), file.componentType(), status, merged,
                    aiFile != null ? aiFile.suggestions() : List.of(),
                    aiFile != null ? aiFile.passedRules() : List.of(),
                    aiFile != null ? aiFile.skippedRules() : List.of(),
                    aiFile != null ? aiFile.confidence() : ReviewConfidence.UNKNOWN,
                    status != FileReviewStatus.BLOCKED
            ));
        }

        ReviewStatus globalStatus = summaryBuilder.deriveGlobalStatus(finalFiles, aiResponse.globalIssues());
        boolean humanReviewRequired = summaryBuilder.deriveHumanReviewRequired(finalFiles, aiResponse.globalIssues());
        boolean valid = summaryBuilder.deriveValid(globalStatus);

        return new CodeReviewResult(executionId, finalFiles, aiResponse.globalIssues(), aiResponse.suggestions(),
                aiResponse.passedRules(), aiResponse.skippedRules(), aiResponse.warnings(),
                globalStatus, aiResponse.confidence(), humanReviewRequired, valid);
    }

    private CodeReviewResult buildBlockedResult(UUID executionId, GenerationResult generation, List<ReviewIssue> issues) {
        Map<String, List<ReviewIssue>> byPath = issues.stream()
                .filter(i -> i.relativePath() != null)
                .collect(Collectors.groupingBy(ReviewIssue::relativePath, LinkedHashMap::new, Collectors.toList()));

        List<FileReviewResult> files = new ArrayList<>();
        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() == GeneratedFileOperation.REUSE || file.operation() == GeneratedFileOperation.NONE) {
                files.add(new FileReviewResult(file.relativePath(), file.operation(), file.componentType(),
                        FileReviewStatus.SKIPPED, List.of(), List.of(), List.of(), List.of(), ReviewConfidence.UNKNOWN, true));
                continue;
            }
            List<ReviewIssue> fileIssues = byPath.getOrDefault(file.relativePath(), List.of());
            FileReviewStatus status = summaryBuilder.deriveFileStatus(fileIssues);
            files.add(new FileReviewResult(file.relativePath(), file.operation(), file.componentType(), status,
                    fileIssues, List.of(), List.of(), List.of(), ReviewConfidence.LOW, status != FileReviewStatus.BLOCKED));
        }

        ReviewStatus globalStatus = summaryBuilder.deriveGlobalStatus(files, List.of());
        boolean humanReviewRequired = summaryBuilder.deriveHumanReviewRequired(files, List.of());

        return new CodeReviewResult(executionId, files, List.of(), List.of(), List.of(), List.of(), List.of(),
                globalStatus, ReviewConfidence.LOW, humanReviewRequired, true);
    }

    private List<ReviewIssue> mergeIssues(List<ReviewIssue> statics, List<ReviewIssue> aiIssues) {
        List<ReviewIssue> merged = new ArrayList<>(statics);
        Set<String> staticCodes = new LinkedHashSet<>();
        for (ReviewIssue issue : statics) staticCodes.add(issue.code());
        for (ReviewIssue aiIssue : aiIssues) {
            if (aiIssue != null && !staticCodes.contains(aiIssue.code())) {
                merged.add(aiIssue);
            }
        }
        return List.copyOf(merged);
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
