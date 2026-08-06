package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyValidationException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Valida as pré-condições estruturais da Fase 8: status proibidos das fases
 * anteriores e aprovação humana ausente/negada. Assume argumentos não nulos
 * (a checagem de nulidade é responsabilidade de FileApplicationService).
 * Nunca produz ApplyConflict — falhas aqui são erros de uso, não conflitos
 * de conteúdo detectáveis apenas com os dados.
 */
@Component
public class ApplyPreconditionValidator {

    public void validate(TechnicalPlanResult plan, GenerationResult generation, CodeReviewResult review, ApplyApproval approval) {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(generation, "generation must not be null");
        Objects.requireNonNull(review, "review must not be null");

        if (plan.status() == PlanningStatus.BLOCKED || plan.status() == PlanningStatus.INVALID) {
            throw new ApplyValidationException("Plano " + plan.status() + " não pode ser aplicado");
        }
        if (generation.status() == GenerationStatus.PARTIAL || generation.status() == GenerationStatus.FAILED) {
            throw new ApplyValidationException("Geração " + generation.status() + " não pode ser aplicada");
        }
        if (review.status() == ReviewStatus.CHANGES_REQUIRED
                || review.status() == ReviewStatus.BLOCKED
                || review.status() == ReviewStatus.INVALID) {
            throw new ApplyValidationException("Review " + review.status() + " não pode ser aplicada");
        }

        if (approval == null) {
            throw new ApplyValidationException("ApplyApproval é obrigatório");
        }
        if (!approval.approved()) {
            throw new ApplyValidationException("ApplyApproval.approved deve ser true");
        }
        if (review.status() == ReviewStatus.APPROVED_WITH_WARNINGS && !approval.allowWarnings()) {
            throw new ApplyValidationException("Review APPROVED_WITH_WARNINGS exige ApplyApproval.allowWarnings=true");
        }

        boolean needsCreate = generation.files().stream()
                .anyMatch(f -> f != null && f.operation() == GeneratedFileOperation.CREATE);
        boolean needsUpdate = generation.files().stream()
                .anyMatch(f -> f != null && f.operation() == GeneratedFileOperation.UPDATE);
        if (needsCreate && !approval.permits(ApplyOperation.CREATE)) {
            throw new ApplyValidationException("ApplyApproval não cobre a operação CREATE presente na geração");
        }
        if (needsUpdate && !approval.permits(ApplyOperation.UPDATE)) {
            throw new ApplyValidationException(
                    "ApplyApproval não cobre a operação UPDATE presente na geração (approvedOperations/allowFileUpdate)");
        }
    }
}
