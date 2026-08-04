package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ScenarioAnalysisPromptFactory {

    public String createSystemPrompt() {
        return """
                Você analisa um cenário funcional e devolve somente JSON válido.

                Regras obrigatórias:
                - não gerar código
                - não gerar Markdown
                - não usar blocos ```json ou ```
                - não inventar passos, regras ou dados
                - não incluir imports, classes, locators, comandos, caminhos absolutos, tokens ou API keys
                - classificar dados sensíveis como SECRET
                - registrar ambiguidades de forma explícita
                - usar português do Brasil

                Schema esperado:
                {
                  "title": "string",
                  "objective": "string",
                  "preconditions": ["string"],
                  "steps": [
                    {
                      "order": 1,
                      "action": "string",
                      "expectedResult": "string",
                      "dependencies": ["string"]
                    }
                  ],
                  "testData": [
                    {
                      "name": "string",
                      "type": "STATIC|RANDOM|ENVIRONMENT_VARIABLE|SECRET|DATABASE|API_RESPONSE|FILE|USER_INPUT|UNKNOWN",
                      "required": true,
                      "description": "string",
                      "example": null
                    }
                  ],
                  "businessRules": [
                    {
                      "identifier": "BR-001",
                      "description": "string",
                      "explicit": true
                    }
                  ],
                  "risks": [
                    {
                      "description": "string",
                      "level": "LOW|MEDIUM|HIGH|CRITICAL",
                      "mitigation": "string"
                    }
                  ],
                  "ambiguities": [
                    {
                      "description": "string",
                      "question": "string",
                      "blocking": true
                    }
                  ],
                  "entities": ["string"],
                  "dependencies": ["string"],
                  "automationType": "WEB_UI|API|MOBILE|DATABASE|FILE|INTEGRATION|HYBRID|UNKNOWN",
                  "status": "VALID|VALID_WITH_WARNINGS|INVALID",
                  "warnings": ["string"],
                  "valid": true
                }
                """;
    }

    public String createUserPrompt(String scenario, ProjectDiscoveryResult discovery) {
        String detectedFrameworks = discovery.getDetectedFrameworks().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String testingFrameworks = discovery.getTestingFrameworks().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String warnings = discovery.getWarnings().isEmpty()
                ? "Nenhum"
                : String.join(" | ", discovery.getWarnings());

        return """
                Cenário funcional:
                %s

                Descoberta do projeto:
                - automationFramework: %s
                - language: %s
                - buildTool: %s
                - packageManager: %s
                - testingFrameworks: %s
                - detectedFrameworks: %s
                - warnings: %s

                Responda somente com JSON puro.
                """.formatted(
                scenario,
                discovery.getAutomationFramework(),
                discovery.getLanguage(),
                discovery.getBuildTool(),
                discovery.getPackageManager(),
                testingFrameworks.isBlank() ? "Nenhum" : testingFrameworks,
                detectedFrameworks.isBlank() ? "Nenhum" : detectedFrameworks,
                warnings
        );
    }
}
