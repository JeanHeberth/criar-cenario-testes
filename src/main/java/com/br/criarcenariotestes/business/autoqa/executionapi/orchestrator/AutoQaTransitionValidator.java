package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaExecutionConflictException;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaInvalidTransitionException;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.springframework.stereotype.Component;

/**
 * Ponto único de validação de transição de AutoQaWorkflowStatus. Nunca
 * representa espera por aprovação com exception — só rejeita ações fora de
 * ordem, duplicadas, concorrentes (lock IN_PROGRESS) ou pós-terminal.
 */
@Component
public class AutoQaTransitionValidator {

    private static final String APPLY = "APPLY";
    private static final String EXECUTION = "EXECUTION";

    public void validateStart(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.CREATED, "start");
    }

    public void validateContinue(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.FAILED, "continue");
    }

    public void validateGenerate(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL, "generate");
    }

    public void validateRegisterApplyApproval(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL, "apply-approval");
        if (hasApproval(document, APPLY)) {
            throw new AutoQaExecutionConflictException("ApplyApproval já registrada para esta execução");
        }
    }

    public void validateApply(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL, "apply");
        if (!hasApproval(document, APPLY)) {
            throw new AutoQaInvalidTransitionException("apply exige ApplyApproval registrada previamente");
        }
    }

    public void validateRegisterExecutionApproval(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL, "execution-approval");
        if (hasApproval(document, EXECUTION)) {
            throw new AutoQaExecutionConflictException("ExecutionApproval já registrada para esta execução");
        }
    }

    public void validateExecute(AutoQaExecutionDocument document) {
        requireNotLocked(document);
        requireStatus(document, AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL, "execute");
        if (!hasApproval(document, EXECUTION)) {
            throw new AutoQaInvalidTransitionException("execute exige ExecutionApproval registrada previamente");
        }
    }

    public void validateCancel(AutoQaExecutionDocument document) {
        if (document.getWorkflowStatus().terminal()) {
            throw new AutoQaExecutionConflictException(
                    "Não é possível cancelar uma execução em estado terminal: " + document.getWorkflowStatus());
        }
    }

    private void requireNotLocked(AutoQaExecutionDocument document) {
        if (document.getOperationStatus() == AutoQaOperationStatus.IN_PROGRESS) {
            throw new AutoQaExecutionConflictException("Já existe uma operação em andamento para esta execução");
        }
    }

    private void requireStatus(AutoQaExecutionDocument document, AutoQaWorkflowStatus required, String action) {
        if (document.getWorkflowStatus() != required) {
            throw new AutoQaInvalidTransitionException(
                    action + " só é permitido a partir de " + required + " (atual: " + document.getWorkflowStatus() + ")");
        }
    }

    private boolean hasApproval(AutoQaExecutionDocument document, String type) {
        return document.getApprovals().stream().anyMatch(a -> a.type().equals(type) && a.approved());
    }
}
