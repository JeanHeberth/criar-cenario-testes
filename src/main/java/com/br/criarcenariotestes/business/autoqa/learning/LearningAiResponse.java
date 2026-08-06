package com.br.criarcenariotestes.business.autoqa.learning;

import java.util.List;

/**
 * DTO intermediário e não confiável, deserializado diretamente da resposta
 * da IA. Nunca é usado como LearningResult final (que exige executionId não
 * nulo vindo do contexto, não da IA) — a materialização/validação de cada
 * item sugerido acontece em LearningService/LearningValidator.
 */
public record LearningAiResponse(
        List<Item> items,
        List<Warning> warnings,
        String confidence,
        Boolean humanReviewRequired,
        Boolean valid
) {
    public record Item(
            String type,
            String scope,
            String title,
            String description,
            String recommendation,
            List<String> relatedComponents,
            List<String> relatedFiles,
            List<String> tags
    ) {
    }

    public record Warning(String code, String description) {
    }
}
