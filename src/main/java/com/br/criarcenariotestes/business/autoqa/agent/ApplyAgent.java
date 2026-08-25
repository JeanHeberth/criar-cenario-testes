package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.apply.FileApplicationService;
import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyValidationException;
import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Aplica no projeto real somente arquivos já planejados, gerados e
 * revisados. Não executa IA, comandos externos ou testes do projeto
 * analisado. Não cria ExecuteAgent nem avança para a Fase 9.
 */
@Component
@Order(60)
public class ApplyAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ApplyAgent.class);

    private final FileApplicationService fileApplicationService;

    public ApplyAgent(FileApplicationService fileApplicationService) {
        this.fileApplicationService = Objects.requireNonNull(fileApplicationService, "fileApplicationService must not be null");
    }

    @Override
    public String getName() {
        return "apply";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Apply agent started. executionId={}", context.getExecutionId());

        if (context.getProjectDiscoveryResult() == null
                || context.getScenarioAnalysisResult() == null
                || context.getProjectKnowledgeResult() == null
                || context.getTechnicalPlanResult() == null
                || context.getGenerationResult() == null
                || context.getCodeReviewResult() == null
                || context.getApplyApproval() == null) {
            return failureSkip(context, "missing-preconditions");
        }

        if (context.getScenarioAnalysisResult().status() == ScenarioAnalysisStatus.INVALID) {
            return failureSkip(context, "invalid-scenario");
        }
        if (context.getProjectKnowledgeResult().status() == KnowledgeStatus.FAILED) {
            return failureSkip(context, "failed-knowledge");
        }
        PlanningStatus planStatus = context.getTechnicalPlanResult().status();
        if (planStatus == PlanningStatus.BLOCKED || planStatus == PlanningStatus.INVALID) {
            return failureSkip(context, "blocked-or-invalid-plan");
        }
        GenerationStatus generationStatus = context.getGenerationResult().status();
        if (generationStatus == GenerationStatus.PARTIAL || generationStatus == GenerationStatus.FAILED) {
            return failureSkip(context, "generation-partial-or-failed");
        }
        ReviewStatus reviewStatus = context.getCodeReviewResult().status();
        if (reviewStatus == ReviewStatus.BLOCKED || reviewStatus == ReviewStatus.INVALID) {
            return failureSkip(context, "review-not-approved");
        }
        // CHANGES_REQUIRED por achados leves (ex.: "URL hardcoded", severidade
        // LOW) travava a aplicação sem saída: o humano já aprovou marcando
        // allowWarnings, e esse campo não era consultado em lugar nenhum.
        // Achado de severidade alta continua bloqueando, aprovado ou não.
        if (reviewStatus == ReviewStatus.CHANGES_REQUIRED
                && !podeSeguirComAchadosLeves(context)) {
            return failureSkip(context, "review-not-approved");
        }

        try {
            ApplyResult result = fileApplicationService.apply(
                    context.getExecutionId(),
                    context.getProjectDiscoveryResult(),
                    context.getTechnicalPlanResult(),
                    context.getGenerationResult(),
                    context.getCodeReviewResult(),
                    context.getApplyApproval()
            );
            context.registerApplyResult(result);
            log.info("Apply agent finished. executionId={}, status={}", context.getExecutionId(), result.status());
            // BLOCKED/FAILED/ROLLED_BACK não gravaram (ou desfizeram) nada. Tratá-los
            // como sucesso fazia o workflow avançar para WAITING_EXECUTION_APPROVAL
            // com errors=[], anunciando progresso sem nenhum arquivo no disco - a
            // falha só aparecia relendo o log.
            if (result.status() == ApplyStatus.BLOCKED
                    || result.status() == ApplyStatus.FAILED
                    || result.status() == ApplyStatus.ROLLED_BACK) {
                log.warn("Apply não aplicou arquivos. executionId={}, status={}, conflitos={}",
                        context.getExecutionId(), result.status(), result.conflicts().size());
                return AgentExecutionResult.failure("Aplicação de arquivos não concluída: " + result.status());
            }
            return AgentExecutionResult.success(buildSummary(result));
        } catch (ApplyValidationException | IllegalArgumentException exception) {
            log.warn("Apply agent failed. executionId={}, failureType={}, failureMessage='{}'",
                    context.getExecutionId(), exception.getClass().getSimpleName(), exception.getMessage());
            log.info("Apply agent finished. executionId={}, status=FAILED", context.getExecutionId());
            return AgentExecutionResult.failure("Falha na aplicação de arquivos no projeto: "
                    + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        }
    }

    private AgentExecutionResult failureSkip(AutoQaContext context, String reason) {
        log.warn("Apply agent skipped. executionId={}, reason={}", context.getExecutionId(), reason);
        log.info("Apply agent finished. executionId={}, status=FAILED", context.getExecutionId());
        return AgentExecutionResult.failure(mensagemDe(context, reason));
    }

    /**
     * A mensagem genérica ("Falha na aplicação de arquivos") aparecia na tela sem
     * dizer nada: o estágio anterior mostrava "Concluída" e o motivo real só
     * existia no log do servidor. Cada recusa passa a se explicar, porque é o
     * texto que o usuário lê para saber o que fazer a seguir.
     */
    private String mensagemDe(AutoQaContext context, String reason) {
        return switch (reason) {
            case "review-not-approved" -> "Revisão não aprovou o código gerado ("
                    + context.getCodeReviewResult().status() + "): "
                    + contarAchadosBloqueantes(context) + " problema(s) de severidade alta ou crítica. "
                    + "Veja a etapa de Revisão de Código.";
            case "invalid-scenario" -> "Cenário marcado como inválido pela análise — verifique ambiguidades bloqueantes";
            case "blocked-or-invalid-plan" -> "Planejamento técnico não ficou pronto para aplicação";
            case "generation-partial-or-failed" -> "Geração não produziu todos os arquivos planejados";
            case "failed-knowledge" -> "Conhecimento do projeto não pôde ser coletado";
            case "missing-preconditions" -> "Etapas anteriores não foram concluídas";
            case "approval-missing" -> "Aplicação não foi aprovada";
            default -> "Falha na aplicação de arquivos no projeto";
        };
    }

    private long contarAchadosBloqueantes(AutoQaContext context) {
        var review = context.getCodeReviewResult();
        if (review == null) {
            return 0;
        }
        return review.files().stream()
                .flatMap(f -> f.issues().stream())
                .filter(i -> i.severity() == ReviewSeverity.CRITICAL || i.severity() == ReviewSeverity.HIGH)
                .count()
                + review.globalIssues().stream()
                .filter(i -> i.severity() == ReviewSeverity.CRITICAL || i.severity() == ReviewSeverity.HIGH)
                .count();
    }

    private String buildSummary(ApplyResult result) {
        long applied = result.files().stream()
                .filter(f -> f.status() == ApplyFileStatus.APPLIED)
                .count();
        return "Aplicação concluída: " + result.status() + " / " + applied + " arquivos aplicados / "
                + result.conflicts().size() + " conflitos / " + result.backups().size() + " backups";
    }

    /**
     * Só segue quando o humano aprovou explicitamente com allowWarnings E
     * nenhum achado é de severidade alta. É o que dá sentido ao campo
     * allowWarnings da aprovação, sem transformá-lo em "ignore tudo".
     */
    private boolean podeSeguirComAchadosLeves(AutoQaContext context) {
        var aprovacao = context.getApplyApproval();
        if (aprovacao == null || !aprovacao.allowWarnings()) {
            return false;
        }

        return context.getCodeReviewResult().files().stream()
                .flatMap(arquivo -> arquivo.issues().stream())
                .noneMatch(issue -> issue.severity() == ReviewSeverity.HIGH
                        || issue.severity() == ReviewSeverity.CRITICAL)
                && context.getCodeReviewResult().globalIssues().stream()
                .noneMatch(issue -> issue.severity() == ReviewSeverity.HIGH
                        || issue.severity() == ReviewSeverity.CRITICAL);
    }
}
