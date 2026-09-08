package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaPublicError;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaPublicWarning;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaPublicPlan;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshot;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;

import java.util.Objects;

/**
 * Único ponto de conversão de AutoQaExecutionDocument (Mongo, interno) para
 * AutoQaExecutionResponse (DTO público). Nunca copia projectPath — campo
 * inexistente no DTO de resposta. Sempre campo a campo, nunca reflection ou
 * serialização genérica do documento.
 */
@Component
public class AutoQaExecutionResponseMapper {

    public AutoQaExecutionResponse toResponse(AutoQaExecutionDocument document) {
        return toResponse(document, null);
    }

    /**
     * @param snapshot fonte do plano técnico. A listagem passa nulo — carregar
     *        um snapshot por item para montar uma lista seria custo sem uso.
     */
    public AutoQaExecutionResponse toResponse(AutoQaExecutionDocument document,
                                               AutoQaExecutionSnapshot snapshot) {
        Objects.requireNonNull(document, "document must not be null");

        List<AutoQaPublicWarning> warnings = document.getWarnings().stream()
                .map(w -> new AutoQaPublicWarning(w.code(), w.description(), w.blocking()))
                .toList();
        List<AutoQaPublicError> errors = document.getErrors().stream()
                .map(e -> new AutoQaPublicError(e.code(), e.message()))
                .toList();

        return new AutoQaExecutionResponse(
                document.getExecutionId(),
                document.getScenarioSummary(),
                document.getWorkflowStatus(),
                document.getCurrentStage(),
                document.getLastStageStarted(),
                document.getLastStageCompleted(),
                document.getAttempt(),
                document.getProgress(),
                document.getAvailableActions(),
                warnings,
                errors,
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getStartedAt(),
                document.getFinishedAt(),
                document.getCancelledAt(),
                document.getCancellationReason(),
                document.getAutomationFramework(),
                planoPublico(snapshot)
        );
    }

    /**
     * O plano só existe depois do estágio de planejamento; antes disso o campo
     * vem nulo, e é assim que o front distingue "ainda não planejado" de
     * "planejado sem ações".
     */
    private AutoQaPublicPlan planoPublico(AutoQaExecutionSnapshot snapshot) {
        if (snapshot == null || snapshot.getTechnicalPlan() == null) {
            return null;
        }
        TechnicalPlanResult plano = snapshot.getTechnicalPlan();

        List<AutoQaPublicPlan.AutoQaPublicFileAction> acoes = plano.fileActions().stream()
                .filter(Objects::nonNull)
                .map(a -> new AutoQaPublicPlan.AutoQaPublicFileAction(
                        a.relativePath(),
                        a.operation() == null ? null : a.operation().name(),
                        a.componentType() == null ? null : a.componentType().name(),
                        a.reason()))
                .toList();

        List<AutoQaPublicPlan.AutoQaPublicPlanWarning> advertencias = plano.warnings().stream()
                .filter(Objects::nonNull)
                .map(w -> new AutoQaPublicPlan.AutoQaPublicPlanWarning(
                        w.code(), w.description(), w.requiresHumanDecision()))
                .toList();

        return new AutoQaPublicPlan(plano.title(), plano.strategy(), acoes, advertencias);
    }

    public AutoQaExecutionListResponse toListResponse(Page<AutoQaExecutionDocument> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<AutoQaExecutionResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new AutoQaExecutionListResponse(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
