package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

/**
 * Resultado da análise de falha de teste.
 * Contém tipo de erro, arquivo, linha e stack trace.
 */
public record FailureAnalysis(
        String failureType,      // e.g., "MissingImport", "AssertionFailed", "TimeoutError"
        String errorMessage,     // Mensagem de erro extraída
        String sourceFile,       // Arquivo que falhou
        int lineNumber,          // Linha aproximada (se detectável)
        String stackTrace,       // Stack trace completo
        List<String> suggestions // Sugestões de correção
) {
}
