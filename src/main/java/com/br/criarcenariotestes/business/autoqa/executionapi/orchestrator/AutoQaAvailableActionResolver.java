package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaAvailableAction;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Função pura estado -> ações disponíveis. O frontend nunca decide sozinho
 * se uma ação é permitida — sempre consome o resultado deste resolver. Sem
 * I/O, sem dependência de Mongo/serviço externo.
 */
@Component
public class AutoQaAvailableActionResolver {

    public Set<AutoQaAvailableAction> resolve(AutoQaExecutionDocument document, AutoQaProperties properties) {
        Objects.requireNonNull(document, "document must not be null");
        Objects.requireNonNull(properties, "properties must not be null");

        if (!properties.isEnabled()) {
            return Set.of(AutoQaAvailableAction.NONE);
        }
        if (document.getOperationStatus() == AutoQaOperationStatus.IN_PROGRESS) {
            return Set.of();
        }

        Set<AutoQaAvailableAction> actions = new LinkedHashSet<>();
        boolean applyApproved = hasApproval(document, "APPLY");
        boolean executionApproved = hasApproval(document, "EXECUTION");

        switch (document.getWorkflowStatus()) {
            case CREATED -> actions.add(AutoQaAvailableAction.START);
            case WAITING_GENERATION_APPROVAL -> {
                actions.add(AutoQaAvailableAction.GENERATE);
                actions.add(AutoQaAvailableAction.CANCEL);
            }
            case WAITING_APPLY_APPROVAL -> {
                if (applyApproved) {
                    if (properties.isAllowFileApplication() && properties.isSensitiveActionsEnabled()) {
                        actions.add(AutoQaAvailableAction.APPLY);
                    }
                } else {
                    actions.add(AutoQaAvailableAction.APPROVE_FILE_UPDATE);
                }
                actions.add(AutoQaAvailableAction.VIEW_GENERATED_FILES);
                actions.add(AutoQaAvailableAction.VIEW_DIFF);
                actions.add(AutoQaAvailableAction.CANCEL);
            }
            case WAITING_EXECUTION_APPROVAL -> {
                if (executionApproved) {
                    if (properties.isAllowCommandExecution() && properties.isSensitiveActionsEnabled()) {
                        actions.add(AutoQaAvailableAction.EXECUTE);
                    }
                } else {
                    actions.add(AutoQaAvailableAction.APPROVE_EXECUTION);
                }
                actions.add(AutoQaAvailableAction.CANCEL);
            }
            case COMPLETED -> {
                actions.add(AutoQaAvailableAction.VIEW_GENERATED_FILES);
                actions.add(AutoQaAvailableAction.VIEW_DIFF);
                actions.add(AutoQaAvailableAction.VIEW_LOGS);
                actions.add(AutoQaAvailableAction.VIEW_LEARNING);
            }
            case FAILED -> {
                actions.add(AutoQaAvailableAction.CONTINUE);
                actions.add(AutoQaAvailableAction.CANCEL);
                actions.add(AutoQaAvailableAction.VIEW_LOGS);
            }
            case RUNNING, CANCELLED -> {
                // RUNNING só existe transitoriamente (coberto pelo lock IN_PROGRESS acima);
                // CANCELLED é terminal — nenhuma ação de estado.
            }
        }

        if (actions.isEmpty()) {
            return Set.of(AutoQaAvailableAction.NONE);
        }
        return Set.copyOf(actions);
    }

    private boolean hasApproval(AutoQaExecutionDocument document, String type) {
        return document.getApprovals().stream().anyMatch(a -> a.type().equals(type) && a.approved());
    }
}
