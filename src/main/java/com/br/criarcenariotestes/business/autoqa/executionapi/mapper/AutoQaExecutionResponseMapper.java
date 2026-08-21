package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaPublicError;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaPublicWarning;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
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
                document.getAutomationFramework()
        );
    }

    public AutoQaExecutionListResponse toListResponse(Page<AutoQaExecutionDocument> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<AutoQaExecutionResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new AutoQaExecutionListResponse(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
