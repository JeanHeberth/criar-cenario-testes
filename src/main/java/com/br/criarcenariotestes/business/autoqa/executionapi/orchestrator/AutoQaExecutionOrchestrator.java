package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.CompatibilidadeFrameworkCanal;
import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.*;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaContextSnapshotMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.*;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ponto único de controle do ciclo de vida de uma execução Auto QA:
 * criação, blocos (start/generate/apply/execute), aprovações, cancelamento,
 * lock de concorrência e optimistic locking. Nunca refatora nem instancia
 * AutoQaWorkflowService — delega a execução real dos agentes a
 * AutoQaStageExecutor. Nunca decide sozinho: transições vêm de
 * AutoQaTransitionValidator, ações disponíveis de
 * AutoQaAvailableActionResolver.
 */
@Service
public class AutoQaExecutionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AutoQaExecutionOrchestrator.class);
    private static final int TOTAL_STAGES = AutoQaStage.values().length;

    private final AutoQaExecutionRepository executionRepository;
    private final AutoQaExecutionSnapshotRepository snapshotRepository;
    private final AutoQaContextSnapshotMapper snapshotMapper;
    private final AutoQaStageExecutor stageExecutor;
    private final AutoQaTransitionValidator transitionValidator;
    private final AutoQaAvailableActionResolver actionResolver;
    private final AutoQaProperties properties;
    private final ProjectPathSecurityValidator projectPathSecurityValidator;

    public AutoQaExecutionOrchestrator(AutoQaExecutionRepository executionRepository,
                                        AutoQaExecutionSnapshotRepository snapshotRepository,
                                        AutoQaContextSnapshotMapper snapshotMapper,
                                        AutoQaStageExecutor stageExecutor,
                                        AutoQaTransitionValidator transitionValidator,
                                        AutoQaAvailableActionResolver actionResolver,
                                        AutoQaProperties properties,
                                        ProjectPathSecurityValidator projectPathSecurityValidator) {
        this.executionRepository = Objects.requireNonNull(executionRepository);
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
        this.snapshotMapper = Objects.requireNonNull(snapshotMapper);
        this.stageExecutor = Objects.requireNonNull(stageExecutor);
        this.transitionValidator = Objects.requireNonNull(transitionValidator);
        this.actionResolver = Objects.requireNonNull(actionResolver);
        this.properties = Objects.requireNonNull(properties);
        this.projectPathSecurityValidator = Objects.requireNonNull(projectPathSecurityValidator);
    }

    public AutoQaExecutionDocument create(String scenario, String projectPath) {
        return create(scenario, projectPath, null);
    }

    public AutoQaExecutionDocument create(String scenario, String projectPath, String automationType) {
        return create(scenario, projectPath, automationType, null);
    }

    public AutoQaExecutionDocument create(String scenario, String projectPath, String automationType,
                                           String automationFramework) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        // Combinação impossível é recusada na criação, não no meio do pipeline:
        // descobrir "REST Assured não abre navegador" depois de duas chamadas de
        // IA custa tempo e dinheiro por um erro que se vê no formulário.
        validarCompatibilidade(automationFramework, automationType);
        if (!properties.isEnabled()) {
            throw new AutoQaExecutionDisabledException("Auto QA está desabilitado (auto-qa.enabled=false)");
        }
        // Rejeita cedo (antes de persistir) um projectPath fora de auto-qa.allowed-roots,
        // delegando à mesma política central usada de forma autoritativa em
        // ProjectDiscoveryService — evita criar uma execução fadada a falhar só no START.
        projectPathSecurityValidator.validate(Path.of(projectPath));

        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, scenario, projectPath,
                automationType, automationFramework, now);
        document.setAvailableActions(actionResolver.resolve(document, properties));
        document = saveWithLockHandling(document);

        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(executionId, now);
        saveSnapshotWithLockHandling(snapshot);
        return document;
    }

    public AutoQaExecutionDocument start(UUID executionId) {
        return runBlock(executionId, transitionValidator::validateStart,
                List.of(AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING),
                AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
    }

    public AutoQaExecutionDocument continueExecution(UUID executionId) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateContinue(document);
        List<AutoQaStage> block = blockOf(document.getLastStageStarted());
        return runBlockInternal(document, block, successStatusFor(block));
    }

    public AutoQaExecutionDocument generate(UUID executionId) {
        return runBlock(executionId, transitionValidator::validateGenerate,
                List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW), AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
    }

    public AutoQaExecutionDocument apply(UUID executionId) {
        requireSensitiveActionEnabled(properties.isAllowFileApplication(),
                "Aplicação de arquivo desabilitada por configuração (allow-file-application/sensitive-actions-enabled)");
        return runBlock(executionId, transitionValidator::validateApply,
                List.of(AutoQaStage.APPLY), AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
    }

    public AutoQaExecutionDocument execute(UUID executionId) {
        requireSensitiveActionEnabled(properties.isAllowCommandExecution(),
                "Execução de comando desabilitada por configuração (allow-command-execution/sensitive-actions-enabled)");
        return runBlock(executionId, transitionValidator::validateExecute,
                List.of(AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING), AutoQaWorkflowStatus.COMPLETED);
    }

    public AutoQaExecutionDocument registerApplyApproval(UUID executionId, ApplyApproval approval) {
        Objects.requireNonNull(approval, "approval must not be null");
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateRegisterApplyApproval(document);

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        snapshot.setApplyApproval(approval);
        snapshot.setUpdatedAt(Instant.now());
        saveSnapshotWithLockHandling(snapshot);

        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", approval.approvedBy(), toInstant(approval.approvedAt()), approval.approved()));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    public AutoQaExecutionDocument registerExecutionApproval(UUID executionId, ExecutionApproval approval) {
        Objects.requireNonNull(approval, "approval must not be null");
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateRegisterExecutionApproval(document);

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        snapshot.setExecutionApproval(approval);
        snapshot.setUpdatedAt(Instant.now());
        saveSnapshotWithLockHandling(snapshot);

        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", approval.approvedBy(), toInstant(approval.approvedAt()), approval.approved()));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    public AutoQaExecutionDocument cancel(UUID executionId, String reason) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        transitionValidator.validateCancel(document);

        document.setWorkflowStatus(AutoQaWorkflowStatus.CANCELLED);
        document.setCancelledAt(Instant.now());
        document.setCancellationReason(reason);
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    // --- internals ---

    /**
     * Valor irreconhecível é ignorado (vira "deduza pelo projeto") em vez de
     * virar erro: só a combinação IMPOSSÍVEL entre framework e canal é recusada.
     */
    private void validarCompatibilidade(String automationFramework, String automationType) {
        AutomationFramework framework = parseEnum(AutomationFramework.class, automationFramework);
        AutomationType canal = parseEnum(AutomationType.class, automationType);
        if (CompatibilidadeFrameworkCanal.compativel(framework, canal)) {
            return;
        }
        String canaisAceitos = CompatibilidadeFrameworkCanal.canaisDe(framework).stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(", "));
        throw new IllegalArgumentException(
                "Framework " + framework + " não automatiza o canal " + canal + ". Canais aceitos: " + canaisAceitos);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private AutoQaExecutionDocument runBlock(UUID executionId, Consumer<AutoQaExecutionDocument> validate,
                                              List<AutoQaStage> block, AutoQaWorkflowStatus targetStatus) {
        AutoQaExecutionDocument document = loadDocument(executionId);
        validate.accept(document);
        return runBlockInternal(document, block, targetStatus);
    }

    private AutoQaExecutionDocument runBlockInternal(AutoQaExecutionDocument document, List<AutoQaStage> block,
                                                       AutoQaWorkflowStatus targetStatus) {
        UUID executionId = document.getExecutionId();

        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);
        document.setAttempt(document.getAttempt() + 1);
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        document = saveWithLockHandling(document);
        if (document.getStartedAt() == null) {
            document.setStartedAt(Instant.now());
        }

        AutoQaExecutionSnapshot snapshot = loadSnapshot(executionId);
        AutoQaContext context = snapshotMapper.toContext(snapshot, document.getScenarioSummary(), document.getProjectPath(),
                document.getAutomationType(), document.getAutomationFramework());

        AutoQaStageExecutor.AutoQaStageExecutionResult result;
        try {
            result = stageExecutor.executeStages(context, block);
            if (ehBlocoDeGeracao(block)) {
                result = regerarAteRevisaoAprovar(context, block, result);
            }
        } catch (RuntimeException ex) {
            return finalizeFailure(document, null, null, "TECHNICAL_FAILURE", exceptionMessage(ex));
        }

        if (!result.success()) {
            return finalizeFailure(document, result.lastStageStarted(), result.lastStageCompleted(), "STAGE_FAILURE", result.message());
        }

        document.setLastStageStarted(result.lastStageStarted());
        document.setLastStageCompleted(result.lastStageCompleted());
        document.setCurrentStage(result.lastStageCompleted());

        AutoQaStage snapshotBoundary = boundaryStageOf(block);
        if (snapshotBoundary != null) {
            snapshotMapper.toSnapshot(context, snapshot, snapshotBoundary, Instant.now());
            saveSnapshotWithLockHandling(snapshot);
        }

        document.setWorkflowStatus(targetStatus);
        document.setOperationStatus(AutoQaOperationStatus.SUCCEEDED);
        document.setProgress(computeProgress(document.getLastStageCompleted()));
        if (targetStatus == AutoQaWorkflowStatus.COMPLETED) {
            document.setFinishedAt(Instant.now());
        }
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    private AutoQaExecutionDocument finalizeFailure(AutoQaExecutionDocument document, AutoQaStage lastStageStarted,
                                                      AutoQaStage lastStageCompleted, String code, String message) {
        if (lastStageStarted != null) {
            document.setLastStageStarted(lastStageStarted);
        }
        if (lastStageCompleted != null) {
            document.setLastStageCompleted(lastStageCompleted);
            document.setCurrentStage(lastStageCompleted);
        }
        document.setWorkflowStatus(AutoQaWorkflowStatus.FAILED);
        document.setOperationStatus(AutoQaOperationStatus.FAILED);
        document.getErrors().add(new AutoQaErrorRecord(code, message == null ? "erro desconhecido" : message));
        document.setUpdatedAt(Instant.now());
        document.setAvailableActions(actionResolver.resolve(document, properties));
        return saveWithLockHandling(document);
    }

    /**
     * O snapshot só avança até APPLY — o bloco final (Execution, FailureAnalysis,
     * Learning) nunca é snapshotado, pois sempre roda como uma unidade atômica:
     * se o processo cair no meio dele, a retomada (continueExecution) sempre
     * reidrata do snapshot pós-Apply e reexecuta o bloco final inteiro — nunca
     * repete ApplyAgent.
     */
    private AutoQaStage boundaryStageOf(List<AutoQaStage> block) {
        if (block.contains(AutoQaStage.EXECUTION)) {
            return null;
        }
        return block.isEmpty() ? null : block.get(block.size() - 1);
    }

    private List<AutoQaStage> blockOf(AutoQaStage stage) {
        if (stage == null) {
            throw new AutoQaInvalidTransitionException("Nenhuma etapa foi iniciada anteriormente para retomar");
        }
        return switch (stage) {
            case DISCOVERY, SCENARIO_ANALYSIS, PROJECT_KNOWLEDGE, PLANNING -> List.of(
                    AutoQaStage.DISCOVERY, AutoQaStage.SCENARIO_ANALYSIS, AutoQaStage.PROJECT_KNOWLEDGE, AutoQaStage.PLANNING);
            case GENERATION, REVIEW -> List.of(AutoQaStage.GENERATION, AutoQaStage.REVIEW);
            case APPLY -> List.of(AutoQaStage.APPLY);
            case EXECUTION, FAILURE_ANALYSIS, LEARNING -> List.of(
                    AutoQaStage.EXECUTION, AutoQaStage.FAILURE_ANALYSIS, AutoQaStage.LEARNING);
        };
    }

    private AutoQaWorkflowStatus successStatusFor(List<AutoQaStage> block) {
        if (block.contains(AutoQaStage.PLANNING)) {
            return AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL;
        }
        if (block.contains(AutoQaStage.REVIEW)) {
            return AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL;
        }
        if (block.contains(AutoQaStage.APPLY)) {
            return AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL;
        }
        return AutoQaWorkflowStatus.COMPLETED;
    }

    private void requireSensitiveActionEnabled(boolean specificFlag, String message) {
        if (!specificFlag || !properties.isSensitiveActionsEnabled()) {
            throw new AutoQaSensitiveActionDisabledException(message);
        }
    }

    private AutoQaExecutionDocument loadDocument(UUID executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        return executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new AutoQaExecutionNotFoundException("Execução não encontrada: " + executionId));
    }

    private AutoQaExecutionSnapshot loadSnapshot(UUID executionId) {
        return snapshotRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new AutoQaExecutionNotFoundException("Snapshot não encontrado: " + executionId));
    }

    private AutoQaExecutionDocument saveWithLockHandling(AutoQaExecutionDocument document) {
        try {
            return executionRepository.save(document);
        } catch (OptimisticLockingFailureException ex) {
            throw new AutoQaOptimisticLockException("Conflito de versão detectado ao salvar a execução", ex);
        }
    }

    private AutoQaExecutionSnapshot saveSnapshotWithLockHandling(AutoQaExecutionSnapshot snapshot) {
        try {
            return snapshotRepository.save(snapshot);
        } catch (OptimisticLockingFailureException ex) {
            throw new AutoQaOptimisticLockException("Conflito de versão detectado ao salvar o snapshot", ex);
        }
    }

    private int computeProgress(AutoQaStage lastStageCompleted) {
        if (lastStageCompleted == null) {
            return 0;
        }
        return (int) Math.round((lastStageCompleted.ordinal() + 1) * 100.0 / TOTAL_STAGES);
    }

    private String exceptionMessage(RuntimeException ex) {
        String detail = ex.getMessage();
        return detail == null || detail.isBlank() ? ex.getClass().getSimpleName() : detail;
    }

    private Instant toInstant(java.time.LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private boolean ehBlocoDeGeracao(List<AutoQaStage> block) {
        return block.contains(AutoQaStage.GENERATION) && block.contains(AutoQaStage.REVIEW);
    }

    /**
     * Regera enquanto a revisão reprovar por erro ACIONÁVEL, até o limite de
     * maxGenerationRetries.
     *
     * <p>Antes disto o pipeline era só detector: reprovava e parava, e o
     * maxGenerationRetries existia sem nunca ser usado. Detectar sem
     * realimentar não conserta nada — a pessoa teria de corrigir à mão o que a
     * própria ferramenta deveria produzir.
     *
     * <p>Três travas contra desperdício de token, porque cada volta é uma
     * chamada paga:
     * <ol>
     *   <li>só reprovação ACIONÁVEL entra — erro de compilação e campo fora do
     *       contrato. Achado de estilo não justifica regerar;</li>
     *   <li>se os erros da volta forem IGUAIS aos da anterior, para na hora: o
     *       modelo não está convergindo e insistir é queimar token;</li>
     *   <li>teto rígido em maxGenerationRetries.</li>
     * </ol>
     */
    private AutoQaStageExecutor.AutoQaStageExecutionResult regerarAteRevisaoAprovar(
            AutoQaContext context,
            List<AutoQaStage> block,
            AutoQaStageExecutor.AutoQaStageExecutionResult resultado) {

        List<String> anteriores = List.of();
        int tentativa = 0;

        while (resultado.success() && tentativa < properties.getMaxGenerationRetries()) {
            List<String> acionaveis = errosAcionaveis(context);
            if (acionaveis.isEmpty()) {
                return resultado;
            }
            if (acionaveis.equals(anteriores)) {
                log.warn("Regeração interrompida: mesmos erros da tentativa anterior, sem convergência. "
                        + "executionId={}, erros={}", context.getExecutionId(), acionaveis.size());
                return resultado;
            }

            tentativa++;
            log.info("Revisão reprovou por erro acionável — regerando. executionId={}, tentativa={}/{}, erros={}",
                    context.getExecutionId(), tentativa, properties.getMaxGenerationRetries(), acionaveis);

            anteriores = acionaveis;
            context.setCorrecoesSolicitadas(acionaveis);
            context.setArquivosParaRegerar(arquivosComErro(context));
            context.prepararNovaTentativaDeGeracao();
            resultado = stageExecutor.executeStages(context, block);
        }

        context.setCorrecoesSolicitadas(List.of());
        return resultado;
    }

    /**
     * Caminhos dos arquivos que têm erro acionável. Só eles voltam para a IA:
     * regerar os três arquivos inteiros a cada volta gasta token à toa e
     * aproxima a resposta do limite de saída — foi assim que o Gemini truncou.
     */
    private List<String> arquivosComErro(AutoQaContext context) {
        var revisao = context.getCodeReviewResult();
        if (revisao == null) {
            return List.of();
        }
        List<String> comErro = revisao.files().stream()
                .filter(arquivo -> arquivo.issues().stream()
                        .anyMatch(issue -> ReviewRule.COMPILATION_ERROR.name().equals(issue.code())
                                || ReviewRule.CONTRACT_FIELD_UNKNOWN.name().equals(issue.code())))
                .map(arquivo -> arquivo.relativePath())
                .distinct()
                .toList();

        return incluirOrigemDosTipos(context, revisao, comErro);
    }

    /**
     * Acrescenta os arquivos que DEFINEM os tipos citados nos erros.
     *
     * <p>Observado em produção, e caro: o cliente foi gerado devolvendo
     * {@code Promise<Sucesso | Erro>}, e todo acesso a campo no spec passou a
     * exigir estreitamento da união. Os erros apareciam no SPEC, então só ele
     * era regerado — enquanto a causa, o tipo de retorno, seguia intacta no
     * cliente. Três voltas depois os erros tinham subido de 1 para 24.
     *
     * <p>Erro de tipo aparece onde o tipo é USADO, não onde é DEFINIDO.
     * Regerar apenas o ponto de uso é insolúvel por construção.
     */
    private List<String> incluirOrigemDosTipos(AutoQaContext context,
                                                com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult revisao,
                                                List<String> comErro) {
        List<String> mensagens = revisao.files().stream()
                .flatMap(arquivo -> arquivo.issues().stream())
                .filter(issue -> ReviewRule.COMPILATION_ERROR.name().equals(issue.code()))
                .map(ReviewIssue::message)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (mensagens.isEmpty()) {
            return comErro;
        }

        // Os tipos citados vêm entre aspas simples na mensagem do tsc:
        // "Property 'x' does not exist on type 'Sucesso | Erro'".
        java.util.Set<String> tiposCitados = new java.util.LinkedHashSet<>();
        var citacao = java.util.regex.Pattern.compile("'([A-Za-z_][A-Za-z0-9_]*)'");
        for (String mensagem : mensagens) {
            var matcher = citacao.matcher(mensagem);
            while (matcher.find()) {
                tiposCitados.add(matcher.group(1));
            }
        }

        var geracao = context.getGenerationResult();
        if (geracao == null || tiposCitados.isEmpty()) {
            return comErro;
        }

        List<String> comOrigem = new java.util.ArrayList<>(comErro);
        for (var arquivo : geracao.files()) {
            if (arquivo == null || arquivo.content() == null || comOrigem.contains(arquivo.relativePath())) {
                continue;
            }
            boolean defineTipoCitado = tiposCitados.stream()
                    .anyMatch(tipo -> arquivo.content().contains("interface " + tipo)
                            || arquivo.content().contains("type " + tipo)
                            || arquivo.content().contains("class " + tipo));
            if (defineTipoCitado) {
                log.info("Arquivo incluído na regeração por DEFINIR tipo citado no erro. arquivo={}",
                        arquivo.relativePath());
                comOrigem.add(arquivo.relativePath());
            }
        }
        return List.copyOf(comOrigem);
    }

    /**
     * Achados que descrevem um defeito OBJETIVO do código e que o gerador tem
     * como corrigir relendo o próprio arquivo.
     */
    private List<String> errosAcionaveis(AutoQaContext context) {
        var revisao = context.getCodeReviewResult();
        if (revisao == null) {
            return List.of();
        }
        if (revisao.status() != ReviewStatus.CHANGES_REQUIRED && revisao.status() != ReviewStatus.BLOCKED) {
            return List.of();
        }

        return java.util.stream.Stream.concat(
                        revisao.files().stream().flatMap(arquivo -> arquivo.issues().stream()),
                        revisao.globalIssues().stream())
                .filter(issue -> ReviewRule.COMPILATION_ERROR.name().equals(issue.code())
                        || ReviewRule.CONTRACT_FIELD_UNKNOWN.name().equals(issue.code()))
                .map(ReviewIssue::message)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
}
