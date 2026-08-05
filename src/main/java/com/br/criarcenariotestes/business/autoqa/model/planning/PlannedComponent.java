package com.br.criarcenariotestes.business.autoqa.model.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record PlannedComponent(
    String name,
    PlanComponentType type,
    String responsibility,
    String targetPath,
    List<String> reusableComponents,
    List<String> dependencies,
    List<String> warnings
) {
    public PlannedComponent {
        name = name == null ? null : name.trim();
        responsibility = responsibility == null ? null : responsibility.trim();
        targetPath = targetPath == null ? null : targetPath.trim();
        reusableComponents = reusableComponents == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(reusableComponents));
        dependencies = dependencies == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(dependencies));
        warnings = warnings == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }
}
