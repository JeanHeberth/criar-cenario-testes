package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

/**
 * Resposta estruturada do CodeGenerationAgent após parse da resposta da IA.
 * Arquivos com paths inválidos são rejeitados antes de chegar aqui.
 */
public record GeneratedCodeResponse(

        List<GeneratedFile> files,

        List<String> reusedComponents,

        List<String> missingComponents,

        List<String> warnings,

        String summary,

        boolean generationFailed,

        String failureReason

) {

    public static GeneratedCodeResponse failed(String reason) {
        return new GeneratedCodeResponse(
                List.of(), List.of(), List.of(), List.of(),
                null, true, reason
        );
    }

    public boolean hasFiles() {
        return files != null && !files.isEmpty();
    }
}
