package com.br.criarcenariotestes.business.autoqa.model.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlannedFileAction(
    String relativePath,
    FileOperation operation,
    PlanComponentType componentType,
    String reason,
    boolean existingFile,
    boolean required,
    ApprovalRequirement approvalRequirement,
    List<String> dependencies,
    List<String> warnings
) {
    public PlannedFileAction {
        relativePath = relativePath == null ? null : relativePath.trim();
        reason = reason == null ? null : reason.trim();
        dependencies = dependencies == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(dependencies));
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }
}
