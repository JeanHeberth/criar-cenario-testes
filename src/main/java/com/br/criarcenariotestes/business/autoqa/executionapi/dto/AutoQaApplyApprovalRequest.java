package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record AutoQaApplyApprovalRequest(
        @NotBlank String approvedBy,
        @NotEmpty List<ApplyOperation> authorizedOperations,
        boolean allowFileUpdate,
        boolean allowWarnings
) {
    /** approved=true sempre — o próprio ato de chamar este endpoint é a aprovação. */
    public ApplyApproval toDomain() {
        return new ApplyApproval(true, approvedBy, LocalDateTime.now(), authorizedOperations, allowFileUpdate, allowWarnings);
    }
}
