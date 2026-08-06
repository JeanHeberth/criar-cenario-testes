package com.br.criarcenariotestes.business.autoqa.model.execution;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Registro de aprovação humana explícita para execução de testes. Nunca é
 * inferido nem construído por IA ou heurística. allowedCommands usa o enum
 * fechado ExecutionCommandId — nunca aceita identificador textual arbitrário
 * vindo do usuário.
 */
public record ExecutionApproval(
        boolean approved,
        String approvedBy,
        LocalDateTime approvedAt,
        Set<ExecutionCommandId> allowedCommands,
        boolean allowTestExecution,
        boolean allowInstallCommand,
        boolean allowBuildCommand
) {
    public ExecutionApproval {
        approvedBy = approvedBy == null ? null : approvedBy.trim();
        if (approvedBy == null || approvedBy.isEmpty()) {
            throw new IllegalArgumentException("approvedBy must not be blank");
        }
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        allowedCommands = allowedCommands == null ? Set.of() : Set.copyOf(allowedCommands);
    }

    public boolean permits(ExecutionCommandId commandId) {
        return approved && allowTestExecution && allowedCommands.contains(commandId);
    }
}
