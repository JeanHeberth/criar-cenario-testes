package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import org.springframework.stereotype.Component;

@Component
public class PlanningPromptFactory {

    public String createSystemPrompt() {
        return """
                Você é um planejador técnico de automação de testes. Analise as informações fornecidas e retorne SOMENTE JSON válido.

                Regras obrigatórias:
                - Retornar SOMENTE JSON válido, sem Markdown, sem blocos ```json ou ```
                - Não gerar código, imports, classes, locators, comandos, diffs
                - Não usar a operação DELETE (somente CREATE, UPDATE, REUSE, NONE)
                - Não incluir caminhos absolutos nem path traversal (../)
                - Não inventar componentes que não existam no catálogo fornecido
                - Preferir REUSE sobre CREATE quando possível
                - UPDATE sempre requer approvalRequirement diferente de NONE (usar FILE_UPDATE_REQUIRED ou superior)
                - Indicar approvalRequirement para cada ação de arquivo
                - Usar português do Brasil em todos os campos textuais
                - Quando knowledge for PARTIAL ou EMPTY, incluir warnings explicando as limitações

                Schema esperado:
                {
                  "title": "string",
                  "strategy": "string",
                  "fileActions": [
                    {
                      "relativePath": "string",
                      "operation": "CREATE|UPDATE|REUSE|NONE",
                      "componentType": "TEST|PAGE_OBJECT|COMPONENT_OBJECT|FIXTURE|HELPER|UTILITY|API_CLIENT|SERVICE|MODEL|DTO|FACTORY|BUILDER|RESOURCE|KEYWORD|VARIABLE_FILE|CONFIGURATION|UNKNOWN",
                      "reason": "string",
                      "existingFile": false,
                      "required": true,
                      "approvalRequirement": "NONE|REVIEW_REQUIRED|FILE_UPDATE_REQUIRED|DEPENDENCY_CHANGE_REQUIRED|CONFIGURATION_CHANGE_REQUIRED|MANUAL_DECISION_REQUIRED",
                      "dependencies": ["string"],
                      "warnings": []
                    }
                  ],
                  "components": [
                    {
                      "name": "string",
                      "type": "TEST|PAGE_OBJECT|...",
                      "responsibility": "string",
                      "targetPath": "string",
                      "reusableComponents": ["string"],
                      "dependencies": ["string"],
                      "warnings": []
                    }
                  ],
                  "reuseDecisions": [
                    {
                      "componentPath": "string",
                      "componentType": "TEST|PAGE_OBJECT|...",
                      "reuse": true,
                      "reason": "string",
                      "confidence": "HIGH|MEDIUM|LOW|UNKNOWN",
                      "matchedTerms": ["string"],
                      "limitations": []
                    }
                  ],
                  "risks": [
                    {
                      "description": "string",
                      "impact": "string",
                      "mitigation": "string",
                      "blocking": false
                    }
                  ],
                  "warnings": [
                    {
                      "code": "string",
                      "description": "string",
                      "requiresHumanDecision": false
                    }
                  ],
                  "assumptions": ["string"],
                  "constraints": ["string"],
                  "requiredApprovals": ["string"],
                  "status": "READY|READY_WITH_WARNINGS|BLOCKED|INVALID",
                  "confidence": "HIGH|MEDIUM|LOW|UNKNOWN",
                  "valid": true
                }
                """;
    }

    public String createUserPrompt(SanitizedPlanningInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("Framework de automação: ").append(input.framework()).append("\n");
        sb.append("Linguagem: ").append(input.language()).append("\n");
        sb.append("Build tool: ").append(input.buildTool()).append("\n");
        sb.append("Package manager: ").append(input.packageManager()).append("\n");
        sb.append("Confiança do discovery: ").append(input.discoveryConfidence()).append("\n");

        if (!input.testingFrameworks().isEmpty()) {
            sb.append("Testing frameworks: ").append(String.join(", ", input.testingFrameworks())).append("\n");
        }

        sb.append("\nCenário:\n");
        sb.append("- Título: ").append(input.scenarioTitle()).append("\n");
        sb.append("- Objetivo: ").append(input.scenarioObjective()).append("\n");
        if (!input.scenarioPreconditions().isEmpty()) {
            sb.append("- Pré-condições: ").append(String.join("; ", input.scenarioPreconditions())).append("\n");
        }
        if (!input.entities().isEmpty()) {
            sb.append("- Entidades: ").append(String.join(", ", input.entities())).append("\n");
        }
        sb.append("- Status do cenário: ").append(input.scenarioStatus()).append("\n");

        if (!input.steps().isEmpty()) {
            sb.append("\nPassos:\n");
            for (var step : input.steps()) {
                sb.append("  ").append(step.order()).append(". ").append(step.action())
                  .append(" → ").append(step.expectedResult()).append("\n");
            }
        }

        sb.append("\nKnowledge do projeto:\n");
        sb.append("- Status: ").append(input.knowledgeStatus()).append("\n");

        if (input.knowledgeStatus() == KnowledgeStatus.PARTIAL || input.knowledgeStatus() == KnowledgeStatus.EMPTY) {
            sb.append("- ATENÇÃO: Knowledge ").append(input.knowledgeStatus())
              .append(" - inclua warnings sobre as limitações do plano\n");
        }

        if (!input.sourceDirectories().isEmpty()) {
            sb.append("- Diretórios fonte: ").append(String.join(", ", input.sourceDirectories())).append("\n");
        }
        if (!input.testDirectories().isEmpty()) {
            sb.append("- Diretórios de teste: ").append(String.join(", ", input.testDirectories())).append("\n");
        }

        if (!input.components().isEmpty()) {
            sb.append("\nCatálogo de componentes existentes (").append(input.components().size()).append("):\n");
            for (var c : input.components()) {
                sb.append("  - ").append(c.relativePath()).append(" [").append(c.typeName()).append("] ").append(c.componentName()).append("\n");
            }
        }

        if (!input.candidates().isEmpty()) {
            sb.append("\nCandidatos para reutilização:\n");
            for (var c : input.candidates()) {
                sb.append("  - ").append(c.componentPath()).append(" [").append(c.typeName()).append("] confiança=")
                  .append(c.confidenceName());
                if (!c.matchingTerms().isEmpty()) {
                    sb.append(" termos=").append(String.join(",", c.matchingTerms()));
                }
                sb.append("\n");
            }
        }

        if (!input.knowledgeWarnings().isEmpty()) {
            sb.append("\nAvisos do knowledge:\n");
            for (var w : input.knowledgeWarnings()) {
                sb.append("  - ").append(w).append("\n");
            }
        }

        sb.append("\nResponda somente com JSON puro, sem Markdown.");
        return sb.toString();
    }
}
