package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ExecutionValidationException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Valida as pré-condições estruturais da Fase 9: ApplyResult em status
 * proibido ou aprovação de execução ausente/negada. Assume applyResult não
 * nulo (a checagem de nulidade é responsabilidade de TestExecutionService).
 * Nunca produz um ExecutionResult — falhas aqui são erros de uso.
 */
@Component
public class ExecutionPreconditionValidator {

    public void validate(ApplyResult applyResult, ExecutionApproval approval) {
        Objects.requireNonNull(applyResult, "applyResult must not be null");

        if (applyResult.status() != ApplyStatus.COMPLETED && applyResult.status() != ApplyStatus.COMPLETED_WITH_WARNINGS) {
            throw new ExecutionValidationException("ApplyResult " + applyResult.status() + " não pode ser executado");
        }
        if (approval == null) {
            throw new ExecutionValidationException("ExecutionApproval é obrigatório");
        }
        if (!approval.approved()) {
            throw new ExecutionValidationException("ExecutionApproval.approved deve ser true");
        }
        if (!approval.allowTestExecution()) {
            throw new ExecutionValidationException("ExecutionApproval.allowTestExecution deve ser true");
        }
    }
}
