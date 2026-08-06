package com.br.criarcenariotestes.business.autoqa.model.apply;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Registro de aprovação humana explícita. Nunca é inferido nem construído por
 * IA ou heurística — é sempre um input recebido de fora do sistema. Não
 * carrega credenciais nem segredos.
 */
public record ApplyApproval(
        boolean approved,
        String approvedBy,
        LocalDateTime approvedAt,
        List<ApplyOperation> approvedOperations,
        boolean allowFileUpdate,
        boolean allowWarnings
) {
    public ApplyApproval {
        approvedBy = approvedBy == null ? null : approvedBy.trim();
        if (approvedBy == null || approvedBy.isEmpty()) {
            throw new IllegalArgumentException("approvedBy must not be blank");
        }
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        approvedOperations = approvedOperations == null ? List.of() : List.copyOf(approvedOperations);
    }

    /**
     * CREATE exige aprovação explícita da operação. UPDATE exige, além disso,
     * allowFileUpdate=true. REUSE e NONE nunca escrevem, então não passam por aqui.
     */
    public boolean permits(ApplyOperation operation) {
        if (!approved || !approvedOperations.contains(operation)) {
            return false;
        }
        if (operation == ApplyOperation.UPDATE) {
            return allowFileUpdate;
        }
        return operation == ApplyOperation.CREATE;
    }
}
