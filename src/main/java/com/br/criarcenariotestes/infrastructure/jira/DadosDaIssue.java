package com.br.criarcenariotestes.infrastructure.jira;

import java.util.List;

/**
 * Os campos da issue que interessam para decidir o destino da publicação.
 * Deliberadamente enxuto: só o que alimenta a estratégia de pasta.
 */
public record DadosDaIssue(
        String key,
        String id,
        String summary,
        List<String> componentes,
        List<String> labels
) {
}
