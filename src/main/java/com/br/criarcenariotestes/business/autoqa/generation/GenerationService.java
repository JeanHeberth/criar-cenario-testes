package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationParseException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationTechnicalException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private static final Set<AutomationFramework> SUPPORTED_FRAMEWORKS = EnumSet.of(
            AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS, AutomationFramework.SELENIDE,
            AutomationFramework.SELENIUM, AutomationFramework.REST_ASSURED, AutomationFramework.ROBOT_FRAMEWORK
    );

    private final AiProviderResolver aiProviderResolver;
    private final GenerationInputSanitizer inputSanitizer;
    private final GenerationPromptFactory promptFactory;
    private final GenerationResponseParser responseParser;
    private final GenerationValidator validator;
    private final GeneratedFileWriter fileWriter;
    private final GenerationHashService hashService;
    private final GenerationManifestWriter manifestWriter;

    private Path generatedBaseDir = Path.of(".auto-qa", "generated");

    public GenerationService(AiProviderResolver aiProviderResolver,
                              GenerationInputSanitizer inputSanitizer,
                              GenerationPromptFactory promptFactory,
                              GenerationResponseParser responseParser,
                              GenerationValidator validator,
                              GeneratedFileWriter fileWriter,
                              GenerationHashService hashService,
                              GenerationManifestWriter manifestWriter) {
        this.aiProviderResolver = Objects.requireNonNull(aiProviderResolver);
        this.inputSanitizer = Objects.requireNonNull(inputSanitizer);
        this.promptFactory = Objects.requireNonNull(promptFactory);
        this.responseParser = Objects.requireNonNull(responseParser);
        this.validator = Objects.requireNonNull(validator);
        this.fileWriter = Objects.requireNonNull(fileWriter);
        this.hashService = Objects.requireNonNull(hashService);
        this.manifestWriter = Objects.requireNonNull(manifestWriter);
    }

    /** Visível apenas para testes: permite isolar a escrita em um diretório temporário. */
    void setGeneratedBaseDir(Path generatedBaseDir) {
        this.generatedBaseDir = Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
    }

    public GenerationResult generate(UUID executionId,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan) {
        if (executionId == null) throw new IllegalArgumentException("executionId must not be null");
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (scenario == null) throw new IllegalArgumentException("scenario must not be null");
        if (knowledge == null) throw new IllegalArgumentException("knowledge must not be null");
        if (plan == null) throw new IllegalArgumentException("plan must not be null");

        if (scenario.status() == ScenarioAnalysisStatus.INVALID) {
            throw new GenerationValidationException("Cenário inválido não pode ser gerado");
        }
        if (knowledge.status() == KnowledgeStatus.FAILED) {
            throw new GenerationValidationException("Knowledge FAILED não pode ser gerado");
        }
        if (plan.status() == PlanningStatus.BLOCKED) {
            throw new GenerationValidationException("Plano BLOCKED não pode ser gerado");
        }
        if (plan.status() == PlanningStatus.INVALID) {
            throw new GenerationValidationException("Plano INVALID não pode ser gerado");
        }
        if (!SUPPORTED_FRAMEWORKS.contains(discovery.getAutomationFramework())) {
            throw new GenerationValidationException("Framework não suportado para geração: " + discovery.getAutomationFramework());
        }

        log.info("Generation started. executionId={}", executionId);

        SanitizedGenerationInput sanitized = inputSanitizer.sanitize(discovery, scenario, knowledge, plan);
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitized);

        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Generation provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Generation fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        GenerationResult aiResult = generateWithFallback(
                activeProvider, fallbackProvider, sameProvider, systemPrompt, userPrompt, discovery, scenario, knowledge, plan
        );

        return materialize(executionId, discovery, plan, aiResult);
    }

    private GenerationResult generateWithFallback(AiProvider active, AiProvider fallback, boolean same,
                                                    String sys, String user,
                                                    ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                    ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        try {
            GenerationResult result = generateWithProvider(active, sys, user, discovery, scenario, knowledge, plan);
            log.debug("Generation concluída. provider={}", safeProviderName(active));
            return result;
        } catch (GenerationValidationException e) {
            throw e;
        } catch (GenerationTechnicalException | GenerationParseException primary) {
            if (same) {
                log.error("Generation failed without fallback. provider={}, failureType={}, status=FAILED",
                        safeProviderName(active), primary.getClass().getSimpleName());
                throw new GenerationTechnicalException("Falha técnica na geração", primary);
            }
            log.warn("Generation primária falhou, tentando fallback. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(active), safeProviderName(fallback));
            return generateWithFallbackProvider(active, fallback, sys, user, primary, discovery, scenario, knowledge, plan);
        }
    }

    private GenerationResult generateWithFallbackProvider(AiProvider active, AiProvider fallback,
                                                            String sys, String user, Exception primaryFailure,
                                                            ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                            ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        try {
            GenerationResult result = generateWithProvider(fallback, sys, user, discovery, scenario, knowledge, plan);
            log.debug("Generation concluída via fallback. provider={}", safeProviderName(fallback));
            return result;
        } catch (GenerationValidationException e) {
            throw e;
        } catch (GenerationTechnicalException | GenerationParseException fallbackFailure) {
            log.error("Generation failed after fallback. primaryProvider={}, fallbackProvider={}, status=FAILED",
                    safeProviderName(active), safeProviderName(fallback));
            GenerationTechnicalException ex = new GenerationTechnicalException("Falha técnica na geração", fallbackFailure);
            ex.addSuppressed(primaryFailure);
            throw ex;
        }
    }

    private GenerationResult generateWithProvider(AiProvider provider, String systemPrompt, String userPrompt,
                                                    ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                    ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        String response;
        try {
            response = provider.gerarResposta(systemPrompt, userPrompt);
        } catch (RuntimeException e) {
            throw new GenerationTechnicalException("Falha técnica no provider " + provider.getName(), e);
        }
        GenerationResult parsed = responseParser.parse(response);
        return validator.validate(parsed, discovery, scenario, knowledge, plan);
    }

    private GenerationResult materialize(UUID executionId, ProjectDiscoveryResult discovery,
                                          TechnicalPlanResult plan, GenerationResult aiResult) {
        List<GeneratedFile> finalFiles = new ArrayList<>();
        List<String> reusedFiles = new ArrayList<>();
        List<Path> written = new ArrayList<>();

        for (PlannedFileAction action : plan.fileActions()) {
            if (action == null) continue;
            if (action.operation() == FileOperation.REUSE || action.operation() == FileOperation.NONE) {
                GeneratedFileOperation operation = action.operation() == FileOperation.REUSE
                        ? GeneratedFileOperation.REUSE : GeneratedFileOperation.NONE;
                finalFiles.add(new GeneratedFile(action.relativePath(), operation, action.componentType(), null,
                        "UTF-8", null, GeneratedFileStatus.SKIPPED, action.existingFile(), List.of(),
                        action.dependencies(), List.of()));
                if (operation == GeneratedFileOperation.REUSE) {
                    reusedFiles.add(action.relativePath());
                }
            }
        }

        try {
            for (GeneratedFile file : aiResult.files()) {
                Path target = fileWriter.write(generatedBaseDir, executionId, file);
                written.add(target);
                GeneratedFileHash hash = hashService.sha256(file.content());
                finalFiles.add(new GeneratedFile(file.relativePath(), file.operation(), file.componentType(),
                        file.content(), file.encoding(), hash.hex(), GeneratedFileStatus.GENERATED,
                        file.existingFile(), file.reusedComponents(), file.dependencies(), file.warnings()));
            }
        } catch (GenerationWriteException e) {
            rollback(written);
            throw e;
        }

        GenerationStatus finalStatus = aiResult.status();
        boolean valid = switch (finalStatus) {
            case COMPLETED, COMPLETED_WITH_WARNINGS -> true;
            case PARTIAL, FAILED -> false;
        };

        GenerationManifest manifest = buildManifest(executionId, discovery, plan, finalStatus, finalFiles, aiResult.warnings());
        try {
            manifestWriter.write(generatedBaseDir, executionId, manifest);
        } catch (GenerationWriteException e) {
            rollback(written);
            throw e;
        }

        String generatedRoot = generatedBaseDir.resolve(executionId.toString()).toString().replace('\\', '/');
        String manifestRelativePath = executionId + "/" + GenerationManifestWriter.MANIFEST_FILE_NAME;

        log.info("Generation finished. executionId={}, status={}, files={}", executionId, finalStatus, finalFiles.size());

        return new GenerationResult(
                executionId,
                discovery.getAutomationFramework().name(),
                discovery.getLanguage().name(),
                finalFiles,
                reusedFiles,
                aiResult.warnings(),
                generatedRoot,
                manifestRelativePath,
                finalStatus,
                aiResult.confidence(),
                valid
        );
    }

    private GenerationManifest buildManifest(UUID executionId, ProjectDiscoveryResult discovery, TechnicalPlanResult plan,
                                              GenerationStatus status, List<GeneratedFile> files, List<GenerationWarning> warnings) {
        List<GenerationManifest.GenerationManifestFile> manifestFiles = files.stream()
                .map(f -> new GenerationManifest.GenerationManifestFile(
                        f.relativePath(),
                        f.operation().name(),
                        f.componentType() != null ? f.componentType().name() : "UNKNOWN",
                        f.status() != null ? f.status().name() : "UNKNOWN",
                        f.sha256(),
                        f.existingFile()
                ))
                .toList();
        return new GenerationManifest(
                executionId,
                discovery.getAutomationFramework().name(),
                discovery.getLanguage().name(),
                plan.status().name(),
                status.name(),
                LocalDateTime.now().toString(),
                manifestFiles,
                warnings
        );
    }

    private void rollback(List<Path> written) {
        for (Path path : written) {
            if (path == null) continue;
            try {
                java.nio.file.Files.deleteIfExists(path);
            } catch (java.io.IOException ignored) {
                // melhor esforço de limpeza; a área isolada pode ser removida integralmente depois
            }
        }
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
