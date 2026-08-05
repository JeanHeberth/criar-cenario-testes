package com.br.criarcenariotestes.business.autoqa.review;

import org.springframework.stereotype.Component;

@Component
public class CodeReviewPromptFactory {

    public String createSystemPrompt() {
        return """
                Você é um revisor técnico de código de automação de testes. Analise os arquivos gerados e retorne SOMENTE JSON válido.

                Regras obrigatórias:
                - Retornar SOMENTE JSON válido, sem Markdown, sem blocos ```json ou ```
                - Não retornar código corrigido, diff, patch ou arquivo completo como resposta
                - Retornar somente issues, sugestões e status — nunca o conteúdo do arquivo revisado
                - Não inventar arquivos que não estejam na lista fornecida
                - Não inventar linhas sem evidência no conteúdo fornecido
                - Não inventar dependências que não existam no catálogo fornecido
                - Não alterar o plano técnico nem propor novas ações de arquivo
                - Não aprovar conteúdo que contenha segredo, credencial ou risco de segurança
                - Não ignorar, remover ou reduzir a severidade de nenhuma issue estática informada
                - Priorizar segurança acima de estilo ou preferências
                - Usar português do Brasil em todos os campos textuais

                Schema esperado:
                {
                  "files": [
                    {
                      "relativePath": "string",
                      "status": "APPROVED|APPROVED_WITH_WARNINGS|CHANGES_REQUIRED|BLOCKED|SKIPPED|INVALID",
                      "issues": [
                        {
                          "code": "string",
                          "category": "PLAN_ADHERENCE|FRAMEWORK_COMPATIBILITY|LANGUAGE_COMPATIBILITY|CODE_QUALITY|SECURITY|REUSE|DUPLICATION|DEPENDENCY|IMPORT|NAMING|STRUCTURE|MAINTAINABILITY|TEST_DESIGN|ASSERTION|WAIT_STRATEGY|ERROR_HANDLING|DATA_MANAGEMENT|DOCUMENTATION|UNKNOWN",
                          "severity": "INFO|LOW|MEDIUM|HIGH|CRITICAL",
                          "relativePath": "string",
                          "line": 1,
                          "message": "string",
                          "evidence": "string",
                          "recommendation": "string",
                          "blocking": false
                        }
                      ],
                      "suggestions": [
                        {
                          "relativePath": "string",
                          "description": "string",
                          "priority": "INFO|LOW|MEDIUM|HIGH|CRITICAL",
                          "rationale": "string",
                          "automaticFixPossible": false,
                          "relatedIssueCodes": ["string"]
                        }
                      ],
                      "passedRules": ["string"],
                      "skippedRules": ["string"],
                      "confidence": "HIGH|MEDIUM|LOW|UNKNOWN",
                      "valid": true
                    }
                  ],
                  "globalIssues": [],
                  "suggestions": [],
                  "passedRules": [],
                  "skippedRules": [],
                  "warnings": [],
                  "status": "APPROVED|APPROVED_WITH_WARNINGS|CHANGES_REQUIRED|BLOCKED|INVALID",
                  "confidence": "HIGH|MEDIUM|LOW|UNKNOWN",
                  "humanReviewRequired": false,
                  "valid": true
                }
                """;
    }

    public String createUserPrompt(SanitizedCodeReviewInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Framework de automação: ").append(input.framework()).append("\n");
        sb.append("Linguagem: ").append(input.language()).append("\n");

        sb.append("\nCenário:\n");
        sb.append("- Título: ").append(input.scenarioTitle()).append("\n");
        sb.append("- Objetivo: ").append(input.scenarioObjective()).append("\n");

        sb.append("\nPlano técnico aprovado:\n");
        sb.append("- Título: ").append(input.planTitle()).append("\n");
        sb.append("- Estratégia: ").append(input.planStrategy()).append("\n");

        if (input.namingConvention() != null) {
            sb.append("\nConvenções de nomenclatura do projeto:\n");
            sb.append("  - Padrão de teste: ").append(input.namingConvention().testFilePattern()).append("\n");
            sb.append("  - Padrão de page object: ").append(input.namingConvention().pageObjectPattern()).append("\n");
        }

        if (!input.reusableComponents().isEmpty()) {
            sb.append("\nComponentes existentes disponíveis para reutilização:\n");
            for (var c : input.reusableComponents()) {
                sb.append("  - ").append(c.relativePath()).append(" [").append(c.typeName()).append("] ").append(c.componentName()).append("\n");
            }
        }

        if (!input.staticIssues().isEmpty()) {
            sb.append("\nIssues estáticas já identificadas (NÃO remover, NÃO reduzir severidade):\n");
            for (var issue : input.staticIssues()) {
                sb.append("  - [").append(issue.severityName()).append("] ").append(issue.code())
                        .append(" em ").append(issue.relativePath()).append(": ").append(issue.message()).append("\n");
            }
        }

        sb.append("\nArquivos gerados para revisão (").append(input.files().size()).append("):\n");
        for (var file : input.files()) {
            sb.append("\n--- Arquivo: ").append(file.relativePath())
                    .append(" [").append(file.operationName()).append("/").append(file.componentTypeName()).append("] ---\n");
            sb.append(file.content()).append("\n");
        }

        sb.append("\nResponda somente com JSON puro, sem Markdown.");
        return sb.toString();
    }
}
