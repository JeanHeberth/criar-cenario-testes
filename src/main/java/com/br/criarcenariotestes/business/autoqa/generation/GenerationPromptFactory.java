package com.br.criarcenariotestes.business.autoqa.generation;

import org.springframework.stereotype.Component;

@Component
public class GenerationPromptFactory {

    public String createSystemPrompt() {
        return """
                Você é um gerador de código de automação de testes. Analise as informações fornecidas e retorne SOMENTE JSON válido.

                Regras obrigatórias:
                - Retornar SOMENTE JSON válido, sem Markdown, sem blocos ```json ou ```
                - Gerar exatamente um item em "files" para cada ação CREATE ou UPDATE do plano recebido
                - Não gerar itens para ações REUSE ou NONE do plano
                - Não usar a operação DELETE (somente CREATE ou UPDATE)
                - Não incluir caminhos absolutos nem path traversal (../)
                - Não inventar arquivos que não estejam no plano recebido
                - Não inventar componentes ou dependências que não existam no catálogo fornecido
                - Cada campo "content" deve conter o código de um único arquivo, nunca múltiplos arquivos concatenados
                - O campo "content" nunca deve conter blocos de código Markdown (```)
                - Respeitar o framework e a linguagem informados
                - Reutilizar os componentes existentes informados quando aplicável, referenciando-os em "reusedComponents"
                - Usar português do Brasil apenas nos campos textuais descritivos (ex.: "description" de warnings)
                - O código gerado deve ser escrito na linguagem correta: TypeScript/JavaScript para Playwright/Cypress, Java para Selenide/Selenium/RestAssured, Robot Framework para Robot
                - Quando não for possível gerar algum arquivo planejado, use status "PARTIAL" e inclua um warning explicando o motivo
                - Quando não for possível gerar nenhum arquivo, use status "FAILED" e não inclua itens em "files"

                Schema esperado:
                {
                  "files": [
                    {
                      "relativePath": "string",
                      "operation": "CREATE|UPDATE",
                      "componentType": "TEST|PAGE_OBJECT|COMPONENT_OBJECT|FIXTURE|HELPER|UTILITY|API_CLIENT|SERVICE|MODEL|DTO|FACTORY|BUILDER|RESOURCE|KEYWORD|VARIABLE_FILE|CONFIGURATION|UNKNOWN",
                      "content": "string",
                      "encoding": "UTF-8",
                      "existingFile": false,
                      "reusedComponents": ["string"],
                      "dependencies": ["string"],
                      "warnings": []
                    }
                  ],
                  "warnings": [
                    {
                      "code": "string",
                      "description": "string",
                      "blocking": false
                    }
                  ],
                  "status": "COMPLETED|COMPLETED_WITH_WARNINGS|PARTIAL|FAILED",
                  "confidence": "HIGH|MEDIUM|LOW|UNKNOWN",
                  "valid": true
                }
                """;
    }

    public String createUserPrompt(SanitizedGenerationInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Framework de automação: ").append(input.framework()).append("\n");
        sb.append("Linguagem: ").append(input.language()).append("\n");
        sb.append("Build tool: ").append(input.buildTool()).append("\n");
        if (!input.testingFrameworks().isEmpty()) {
            sb.append("Testing frameworks: ").append(String.join(", ", input.testingFrameworks())).append("\n");
        }

        sb.append("\nCenário:\n");
        sb.append("- Título: ").append(input.scenarioTitle()).append("\n");
        sb.append("- Objetivo: ").append(input.scenarioObjective()).append("\n");
        if (!input.steps().isEmpty()) {
            sb.append("\nPassos:\n");
            for (var step : input.steps()) {
                sb.append("  ").append(step.order()).append(". ").append(step.action())
                        .append(" → ").append(step.expectedResult()).append("\n");
            }
        }

        sb.append("\nPlano técnico aprovado:\n");
        sb.append("- Título: ").append(input.planTitle()).append("\n");
        sb.append("- Estratégia: ").append(input.planStrategy()).append("\n");

        sb.append("\nAções de arquivo planejadas (gerar 'files' somente para CREATE/UPDATE):\n");
        for (var action : input.fileActions()) {
            sb.append("  - ").append(action.relativePath())
                    .append(" [").append(action.operationName()).append("/").append(action.componentTypeName()).append("] ")
                    .append(action.reason()).append("\n");
        }

        if (!input.reusableComponents().isEmpty()) {
            sb.append("\nComponentes existentes disponíveis para reutilização:\n");
            for (var c : input.reusableComponents()) {
                sb.append("  - ").append(c.relativePath()).append(" [").append(c.typeName()).append("] ").append(c.componentName());
                if (!c.declaredMethods().isEmpty()) {
                    sb.append(" métodos=").append(String.join(",", c.declaredMethods()));
                }
                sb.append("\n");
            }
        }

        if (input.namingConvention() != null) {
            sb.append("\nConvenções de nomenclatura do projeto:\n");
            sb.append("  - Padrão de teste: ").append(input.namingConvention().testFilePattern()).append("\n");
            sb.append("  - Padrão de page object: ").append(input.namingConvention().pageObjectPattern()).append("\n");
        }

        if (!input.planWarnings().isEmpty()) {
            sb.append("\nAvisos do plano:\n");
            for (var w : input.planWarnings()) {
                sb.append("  - ").append(w).append("\n");
            }
        }

        sb.append("\nResponda somente com JSON puro, sem Markdown.");
        return sb.toString();
    }
}
