package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.Set;

public record AutoQaExecutionApprovalRequest(
        @NotBlank String approvedBy,
        @NotEmpty Set<ExecutionCommandId> allowedCommands,
        boolean allowTestExecution,
        boolean allowInstallCommand,
        boolean allowBuildCommand
) {
    /** approved=true sempre — o próprio ato de chamar este endpoint é a aprovação. */
    public ExecutionApproval toDomain() {
        return new ExecutionApproval(true, approvedBy, LocalDateTime.now(), allowedCommands, allowTestExecution,
                allowInstallCommand, allowBuildCommand);
    }
}
