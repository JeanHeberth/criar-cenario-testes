package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaAvailableAction;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO público sanitizado. Nunca inclui projectPath absoluto, prompt,
 * resposta bruta de IA, segredo, environment, conteúdo completo de arquivo,
 * stacktrace ou exceção interna — só via AutoQaExecutionResponseMapper.
 */
public record AutoQaExecutionResponse(
        UUID executionId,
        String scenario,
        AutoQaWorkflowStatus status,
        AutoQaStage currentStage,
        AutoQaStage lastStageStarted,
        AutoQaStage lastStageCompleted,
        int attempt,
        int progress,
        Set<AutoQaAvailableAction> availableActions,
        List<AutoQaPublicWarning> warnings,
        List<AutoQaPublicError> errors,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        Instant cancelledAt,
        String cancellationReason,

        /**
         * Framework informado na criação. Público e não sensível — o painel de
         * aprovação de execução usa para marcar só os comandos que fazem sentido
         * neste projeto, em vez de oferecer os onze e deixar o usuário adivinhar.
         */
        String automationFramework
) {
}
