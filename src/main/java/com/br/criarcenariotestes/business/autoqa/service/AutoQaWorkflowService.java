package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.agent.AutomationPlannerAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
import com.br.criarcenariotestes.business.autoqa.agent.CodeGenerationAgent;
import com.br.criarcenariotestes.business.autoqa.agent.CodeReviewAgent;
import com.br.criarcenariotestes.business.autoqa.agent.TestExecutionAgent;
import com.br.criarcenariotestes.business.autoqa.agent.TestResultAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.agent.FailureAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.agent.FixSuggestionAgent;
import com.br.criarcenariotestes.business.autoqa.exception.InvalidProjectPathException;
import com.br.criarcenariotestes.business.autoqa.exception.UnsupportedFrameworkException;
import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import com.br.criarcenariotestes.business.autoqa.model.response.AutoQaResponse;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquestrador principal do workflow Auto QA.
 * Coordena as etapas: validação → descoberta → escaneamento → análise → planejamento.
 * Cada etapa usa o AutoQaContext como estado compartilhado em memória.
 */
@Service
@RequiredArgsConstructor
public class AutoQaWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AutoQaWorkflowService.class);

    private final AutoQaProperties properties;
    private final ProjectPathValidationService pathValidationService;
    private final ProjectDiscoveryAgent discoveryAgent;
    private final ProjectScannerService scannerService;
    private final ProjectAnalysisAgent analysisAgent;
    private final AutomationPlannerAgent plannerAgent;
    private final AutomationFrameworkResolver frameworkResolver;
    private final AutoQaExecutionRepository executionRepository;
    private final CenarioRepository cenarioRepository;
    private final com.br.criarcenariotestes.business.autoqa.agent.CodeGenerationAgent codeGenerationAgent;
    private final com.br.criarcenariotestes.business.autoqa.agent.CodeReviewAgent codeReviewAgent;
    private final com.br.criarcenariotestes.business.autoqa.agent.TestExecutionAgent testExecutionAgent;
    private final com.br.criarcenariotestes.business.autoqa.agent.TestResultAnalysisAgent testResultAnalysisAgent;
    private final com.br.criarcenariotestes.business.autoqa.agent.FailureAnalysisAgent failureAnalysisAgent;
    private final com.br.criarcenariotestes.business.autoqa.agent.FixSuggestionAgent fixSuggestionAgent;
    private final PlaywrightProjectLayoutService playwrightProjectLayoutService;
    @Autowired(required = false)
    private GeneratedFileStorageService generatedFileStorageService;
    @Autowired(required = false)
    private TestExecutionService testExecutionService;

    // ─── Endpoint principal: Analisar ─────────────────────────────────────────

    public AutoQaResponse analyze(AutoQaRequest request) {
        AutoQaContext context = new AutoQaContext(request);
        log.info("Iniciando análise Auto QA. executionId='{}', title='{}'",
                context.executionIdAsString(), request.title());

        try {
            // 1. Validar e resolver caminho
            context.updateStatus(AutoQaStatus.DISCOVERING_PROJECT, "VALIDATE_PATH");
            Path projectPath = pathValidationService.resolveSafely(request.projectPath());
            validatePathExists(projectPath, context);
            context.setNormalizedProjectPath(projectPath);

            // 2. Descobrir projeto
            context.updateStatus(AutoQaStatus.DISCOVERING_PROJECT, "DISCOVER_PROJECT");
            ProjectDiscoveryResult discovery = discoveryAgent.discover(
                    projectPath, request.framework(), request.language()
            );
            context.setDiscoveryResult(discovery);

            // 3. Verificar divergência de framework
            if (discovery.hasFrameworkDivergence()) {
                String msg = discovery.getDivergences().isEmpty()
                        ? "Framework informado diverge do detectado"
                        : discovery.getDivergences().get(0);
                context.addIssue(WorkflowIssue.blocker("CHECK_FRAMEWORK", "DIVERGENCE", msg));
                context.updateStatus(AutoQaStatus.ERROR, "CHECK_FRAMEWORK");
                return saveAndBuildResponse(context);
            }
            context.updateStatus(AutoQaStatus.PROJECT_DISCOVERED, "DISCOVER_PROJECT");

            // 4. Escanear projeto
            context.updateStatus(AutoQaStatus.ANALYZING_PROJECT, "SCAN_PROJECT");
            AutomationFrameworkAdapter adapter;
            try {
                adapter = frameworkResolver.resolve(discovery.effectiveFramework());
            } catch (UnsupportedFrameworkException ex) {
                context.addIssue(WorkflowIssue.error("SCAN_PROJECT", "UNSUPPORTED_FRAMEWORK", ex.getReason()));
                context.updateStatus(AutoQaStatus.ERROR, "SCAN_PROJECT");
                return saveAndBuildResponse(context);
            }
            ProjectCatalog catalog = scannerService.scan(projectPath, adapter.ignoredDirectories());
            context.setProjectCatalog(catalog);

            // 5. Analisar projeto
            context.setProjectAnalysis(analysisAgent.analyze(catalog, discovery.effectiveFramework()));
            context.updateStatus(AutoQaStatus.PROJECT_ANALYZED, "ANALYZE_PROJECT");

            // 6. Criar plano
            context.updateStatus(AutoQaStatus.PLANNING, "CREATE_PLAN");
            String scenarioText = resolveScenario(request);
            AutomationPlan plan = plannerAgent.plan(context, scenarioText);
            context.setAutomationPlan(plan);

            if (plan != null && plan.isBlocked()) {
                context.addIssue(WorkflowIssue.blocker("CREATE_PLAN", "PLAN_BLOCKED",
                        plan.getBlockedReason() != null ? plan.getBlockedReason() : "Plano bloqueado pela IA"));
                context.updateStatus(AutoQaStatus.ERROR, "CREATE_PLAN");
            } else {
                context.updateStatus(AutoQaStatus.PLAN_READY, "CREATE_PLAN");
            }

            // 7-10. Executar fases avançadas (código, revisão, testes, análise) — apenas se GENERATE_AND_EXECUTE
            if (request.modeOrDefault() == AutoQaMode.GENERATE_AND_EXECUTE) {
                executeAdvancedPhases(context, projectPath);
            }

        } catch (InvalidProjectPathException ex) {
            context.addIssue(WorkflowIssue.error("VALIDATE_PATH", "INVALID_PATH", ex.getReason()));
            context.updateStatus(AutoQaStatus.ERROR, "VALIDATE_PATH");
        } catch (ResponseStatusException ex) {
            context.addIssue(WorkflowIssue.error("WORKFLOW", "HTTP_ERROR", ex.getReason()));
            context.updateStatus(AutoQaStatus.ERROR, "WORKFLOW");
        } catch (Exception ex) {
            log.error("Erro inesperado no workflow Auto QA: {}", ex.getMessage(), ex);
            context.addIssue(WorkflowIssue.error("WORKFLOW", "UNEXPECTED_ERROR",
                    ex.getMessage() != null ? ex.getMessage() : "Erro interno"));
            context.updateStatus(AutoQaStatus.ERROR, "WORKFLOW");
        }

        context.finish();
        return saveAndBuildResponse(context);
    }

    // ─── Fases avançadas (7-10) ─────────────────────────────────────────────

    private void executeAdvancedPhases(AutoQaContext context, Path projectPath) {
        if (context.getAutomationPlan() == null || context.getAutomationPlan().isBlocked()) {
            log.warn("Plano não disponível ou bloqueado, pulando fases avançadas");
            return;
        }

        try {
            // 7. Gerar código
            if (codeGenerationAgent != null && context.getDiscoveryResult() != null) {
                context.updateStatus(AutoQaStatus.GENERATING, "CODE_GENERATION");
                // CodeGenerationAgent.generate() retorna GeneratedCodeResponse
                // Chamada será feita por outro serviço (CenarioService)
                log.debug("Fase 7 (Geração de Código) — delegada a CenarioService");
                context.updateStatus(AutoQaStatus.CODE_GENERATED, "CODE_GENERATION");
            }

            // 8. Revisar código
            if (codeReviewAgent != null && context.getGeneratedCodeResponse() != null) {
                context.updateStatus(AutoQaStatus.REVIEWING, "CODE_REVIEW");
                codeReviewAgent.review(context);
                context.updateStatus(AutoQaStatus.REVIEW_APPROVED, "CODE_REVIEW");
            }

            // 9. Executar testes
            if (testExecutionAgent != null) {
                context.updateStatus(AutoQaStatus.EXECUTING, "TEST_EXECUTION");
                context.setProjectPath(projectPath.toString());
                testExecutionAgent.execute(context);
                
                // 9.1 Analisar resultados de teste
                if (testResultAnalysisAgent != null && context.getTestExecutionResult() != null) {
                    testResultAnalysisAgent.execute(context);
                }
                
                if (context.getTestExecutionResult() != null && context.getTestExecutionResult().exitCode() == 0) {
                    context.updateStatus(AutoQaStatus.EXECUTION_SUCCESS, "TEST_EXECUTION");
                } else {
                    context.updateStatus(AutoQaStatus.EXECUTION_FAILED, "TEST_EXECUTION");
                }
            }

            // 10. Analisar falhas e sugerir correções
            if (!context.getFailureAnalyses().isEmpty()) {
                context.updateStatus(AutoQaStatus.ANALYZING_FAILURE, "FAILURE_ANALYSIS");
                failureAnalysisAgent.execute(context);
                fixSuggestionAgent.execute(context);
                log.info("Análise de {} falhas concluída, {} sugestões geradas",
                        context.getFailureAnalyses().size(), context.getFixSuggestions().size());
            }

        } catch (Exception ex) {
            log.error("Erro nas fases avançadas: {}", ex.getMessage(), ex);
            context.addIssue(WorkflowIssue.error("ADVANCED_PHASES", "EXECUTION_ERROR",
                    ex.getMessage() != null ? ex.getMessage() : "Erro nas fases avançadas"));
            context.updateStatus(AutoQaStatus.ERROR, "ADVANCED_PHASES");
        }
    }

    // ─── getExecution ─────────────────────────────────────────────────────────

    public AutoQaResponse getExecution(String executionId) {
        return executionRepository.findByExecutionId(executionId)
                .map(this::toResponse)
                .orElse(null);
    }

    public AutoQaResponse generate(String executionId) {
        AutoQaExecutionDocument doc = getExecutionOrThrow(executionId);
        if (doc.getProjectPath() == null || doc.getAutomationPlan() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Execução sem plano ou caminho de projeto");
        }
        if (generatedFileStorageService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "GeneratedFileStorageService não disponível");
        }
        if (doc.getAutomationPlan().isBlocked()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Plano bloqueado. Corrija pendências antes da geração.");
        }

        Path projectPath = Path.of(doc.getProjectPath());
        AutomationFramework framework = doc.getFramework() != null ? doc.getFramework() : AutomationFramework.UNKNOWN;
        AutomationLanguage language = doc.getLanguage() != null ? doc.getLanguage() : AutomationLanguage.UNKNOWN;

        doc.setStatus(AutoQaStatus.GENERATING);
        doc.setUpdatedAt(LocalDateTime.now());
        saveExecutionOrThrow(doc);

        AutomationFrameworkAdapter adapter = frameworkResolver.resolve(framework);
        String scenarioText = doc.getScenarioText() != null ? doc.getScenarioText() : doc.getTitle();
        String preferredTestDir = "tests";
        String preferredPageObjectDir = "tests/pages";
        if (framework == AutomationFramework.PLAYWRIGHT) {
            preferredTestDir = playwrightProjectLayoutService.resolvePreferredTestDirectory(
                    projectPath, doc.getDiscoveryResult(), doc.getProjectAnalysis()
            );
            preferredPageObjectDir = playwrightProjectLayoutService.resolvePreferredPageObjectDirectory(
                    projectPath, doc.getProjectAnalysis(), preferredTestDir
            );
        }

        GeneratedCodeResponse generated = codeGenerationAgent.generate(
                doc.getAutomationPlan(),
                framework,
                language,
                adapter,
                scenarioText,
                preferredTestDir,
                preferredPageObjectDir
        );

        if (framework == AutomationFramework.PLAYWRIGHT) {
            generated = playwrightProjectLayoutService.normalizeGeneratedPaths(
                    generated,
                    preferredTestDir,
                    preferredPageObjectDir
            );
            if (doc.getAutomationPlan().isRequiresNewPageObject()
                    && !playwrightProjectLayoutService.hasPageObjectFile(generated)) {
                List<WorkflowIssue> issues = doc.getIssues() != null ? new ArrayList<>(doc.getIssues()) : new ArrayList<>();
                issues.add(WorkflowIssue.warning(
                        "GENERATE_CODE",
                        "PAGE_OBJECT_MISSING",
                        "Plano indicou necessidade de Page Object, mas nenhum arquivo de Page Object foi gerado.",
                        "Revise o plano ou ajuste o prompt para incluir criação de Page Object."
                ));
                doc.setIssues(issues);
            }
        }

        if (generated.generationFailed()) {
            doc.setStatus(AutoQaStatus.ERROR);
            List<WorkflowIssue> issues = doc.getIssues() != null ? new ArrayList<>(doc.getIssues()) : new ArrayList<>();
            issues.add(WorkflowIssue.error("GENERATE_CODE", "GENERATION_FAILED",
                    generated.failureReason() != null ? generated.failureReason() : "Falha na geração"));
            doc.setIssues(issues);
            doc.setUpdatedAt(LocalDateTime.now());
            saveExecutionOrThrow(doc);
            return toResponse(doc);
        }

        List<GeneratedFileMetadata> metadata = generatedFileStorageService.store(
                executionId,
                projectPath,
                generated,
                framework,
                language,
                scenarioText,
                AutoQaStatus.CODE_GENERATED,
                1
        );
        doc.setGeneratedFileMetadata(metadata);
        doc.setStatus(AutoQaStatus.CODE_GENERATED);
        doc.setUpdatedAt(LocalDateTime.now());
        saveExecutionOrThrow(doc);
        return toResponse(doc);
    }

    public AutoQaResponse execute(String executionId) {
        AutoQaExecutionDocument doc = getExecutionOrThrow(executionId);
        if (doc.getProjectPath() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Execução sem projectPath");
        }
        if (testExecutionService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TestExecutionService não disponível");
        }
        doc.setStatus(AutoQaStatus.EXECUTING);
        doc.setUpdatedAt(LocalDateTime.now());
        saveExecutionOrThrow(doc);

        TestExecutionResult result = testExecutionService.execute(
                executionId,
                Path.of(doc.getProjectPath()),
                doc.getFramework() != null ? doc.getFramework() : AutomationFramework.UNKNOWN,
                doc.getDiscoveryResult() != null ? doc.getDiscoveryResult().getPackageManager() : PackageManager.UNKNOWN,
                null
        );

        if (result.success()) {
            doc.setStatus(AutoQaStatus.EXECUTION_SUCCESS);
        } else {
            doc.setStatus(AutoQaStatus.EXECUTION_FAILED);
        }
        doc.setExecutionResult(result);
        doc.setUpdatedAt(LocalDateTime.now());
        saveExecutionOrThrow(doc);
        return toResponse(doc);
    }

    public AutoQaResponse discard(String executionId) {
        AutoQaExecutionDocument doc = getExecutionOrThrow(executionId);
        doc.setStatus(AutoQaStatus.REVIEW_REJECTED);
        doc.setFinishedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        saveExecutionOrThrow(doc);
        return toResponse(doc);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void validatePathExists(Path path, AutoQaContext context) {
        if (!java.nio.file.Files.exists(path)) {
            throw InvalidProjectPathException.notFound(path.toString());
        }
        if (!java.nio.file.Files.isDirectory(path)) {
            throw InvalidProjectPathException.notDirectory(path.toString());
        }
        if (!java.nio.file.Files.isReadable(path)) {
            throw InvalidProjectPathException.noReadPermission(path.toString());
        }
    }

    private String resolveScenario(AutoQaRequest request) {
        if (request.scenarioText() != null && !request.scenarioText().isBlank()) {
            return request.scenarioText();
        }
        if (request.scenarioId() != null && !request.scenarioId().isBlank()) {
            return cenarioRepository.findById(request.scenarioId())
                    .map(c -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Título: ").append(c.getTitulo()).append("\n");
                        sb.append("Regra de negócio: ").append(c.getRegraDeNegocio()).append("\n");
                        if (c.getCenarios() != null) {
                            c.getCenarios().forEach(item ->
                                    sb.append("Cenário: ").append(item.getNome())
                                      .append(" - ").append(item.getObjetivo()).append("\n")
                            );
                        }
                        return sb.toString();
                    })
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Cenário não encontrado: " + request.scenarioId()));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "scenarioId ou scenarioText é obrigatório");
    }

    private AutoQaResponse saveAndBuildResponse(AutoQaContext context) {
        AutoQaExecutionDocument doc = toDocument(context);
        try {
            executionRepository.save(doc);
        } catch (Exception ex) {
            log.error("Falha ao salvar execução no MongoDB: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falha ao salvar execução no MongoDB");
        }
        return toResponse(doc);
    }

    private AutoQaExecutionDocument getExecutionOrThrow(String executionId) {
        return executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Execução não encontrada: " + executionId));
    }

    private void saveExecutionOrThrow(AutoQaExecutionDocument doc) {
        try {
            executionRepository.save(doc);
        } catch (Exception ex) {
            log.error("Falha ao salvar execução no MongoDB: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falha ao salvar execução no MongoDB");
        }
    }

    private AutoQaExecutionDocument toDocument(AutoQaContext context) {
        AutoQaExecutionDocument doc = new AutoQaExecutionDocument();
        doc.setExecutionId(context.executionIdAsString());
        doc.setTitle(context.getRequest().title());
        doc.setScenarioId(context.getRequest().scenarioId());
        doc.setScenarioText(context.getRequest().scenarioText());
        doc.setProjectPath(context.projectPathAsString());
        doc.setFramework(context.getDiscoveryResult() != null
                ? context.getDiscoveryResult().effectiveFramework() : context.getRequest().framework());
        doc.setLanguage(context.getDiscoveryResult() != null
                ? context.getDiscoveryResult().effectiveLanguage() : context.getRequest().language());
        doc.setMode(context.getRequest().modeOrDefault());
        doc.setAllowFileUpdate(context.getRequest().allowFileUpdate());
        doc.setExecuteAfterGeneration(context.getRequest().executeAfterGeneration());
        doc.setStatus(context.getStatus());
        doc.setDiscoveryResult(context.getDiscoveryResult());
        doc.setProjectAnalysis(context.getProjectAnalysis());
        doc.setAutomationPlan(context.getAutomationPlan());
        doc.setCodeReviewResult(buildCodeReviewResult(context));
        doc.setFailureAnalysisResult(buildFailureAnalysisResult(context));
        doc.setIssues(context.getIssues());
        doc.setCreatedAt(context.getStartedAt());
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setFinishedAt(context.getFinishedAt());
        return doc;
    }

    private AutoQaResponse toResponse(AutoQaExecutionDocument doc) {
        return new AutoQaResponse(
                doc.getExecutionId(),
                doc.getTitle(),
                doc.getStatus(),
                doc.getStatus() != null ? doc.getStatus().getDescricao() : null,
                doc.getFramework(),
                doc.getLanguage(),
                doc.getMode(),
                doc.getScenarioId(),
                doc.getScenarioText(),
                doc.isAllowFileUpdate(),
                doc.isExecuteAfterGeneration(),
                doc.getProjectPath(),
                doc.getDiscoveryResult(),
                doc.getProjectAnalysis(),
                doc.getAutomationPlan(),
                doc.getGeneratedFileMetadata(),
                doc.getCodeReviewResult(),
                doc.getExecutionResult(),
                doc.getFailureAnalysisResult(),
                doc.getIssues(),
                doc.getCreatedAt(),
                doc.getFinishedAt(),
                doc.getStatus() != null ? doc.getStatus().getDescricao() : null
        );
    }

    private CodeReviewResult buildCodeReviewResult(AutoQaContext context) {
        if (context.getIssues() == null || context.getIssues().isEmpty()) {
            return null;
        }
        boolean hasError = context.getIssues().stream().anyMatch(WorkflowIssue::isError);
        return new CodeReviewResult(!hasError, List.of(), List.of(), 1);
    }

    private FailureAnalysisResult buildFailureAnalysisResult(AutoQaContext context) {
        if (context.getFailureAnalyses() == null || context.getFailureAnalyses().isEmpty()) {
            return null;
        }
        var first = context.getFailureAnalyses().get(0);
        return new FailureAnalysisResult(
                first.failureType(),
                first.errorMessage(),
                first.stackTrace(),
                first.sourceFile() != null ? List.of(first.sourceFile()) : List.of(),
                first.suggestions() != null ? first.suggestions() : List.of(),
                false,
                true
        );
    }
}
