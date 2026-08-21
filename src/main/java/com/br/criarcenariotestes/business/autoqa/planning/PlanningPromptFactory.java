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

                Estrutura de arquivos:
                - SEMPRE separar a camada de interação do teste. O teste descreve o
                  cenário; a camada de interação encapsula seletores, chamadas HTTP e
                  detalhes do framework. O TIPO depende do canal:
                    WEB_UI -> Page Object (componentType PAGE_OBJECT)
                    API    -> API Client  (componentType API_CLIENT)
                    MOBILE -> Screen Object (componentType PAGE_OBJECT)
                  Não use "page object" em teste de API: não existe página ali.
                - Agrupar por FUNCIONALIDADE, uma pasta por feature, com o teste e sua
                  camada de interação JUNTOS na mesma pasta. Exemplos por framework:
                    Playwright/Cypress (API) -> tests/api/<feature>/
                    Playwright/Cypress (UI)  -> tests/e2e/<feature>/
                    Selenide/Selenium/RestAssured -> src/test/java/<pacote>/<feature>/
                    Robot -> tests/<feature>/ com recursos em resources/<feature>/
                - EXCEÇÃO: infraestrutura compartilhada — autenticação/sessão e
                  configuração base de HTTP — vai para pasta comum (ex.: tests/api/shared/
                  ou o pacote equivalente), nunca duplicada por feature. Praticamente todo
                  teste precisa de token; duplicar isso por funcionalidade é pior.
                - Se o projeto JÁ TEM padrão de diretórios (informado no contexto), ele
                  PREVALECE sobre os exemplos acima. Reorganizar um projeto existente não
                  é papel do plano — fato observado ganha de convenção sugerida.
                - Se os testes planejados forem ler variáveis de ambiente (credenciais,
                  URL base, endpoint), SEMPRE incluir um warning listando os NOMES dessas
                  variáveis. É por esse warning que quem roda os testes descobre o que
                  precisa configurar.
                - Só planejar CREATE de ".env.example" (componentType CONFIGURATION) se
                  ele NÃO existir no projeto (confira o catálogo de componentes existentes).
                  Quando já existe, NÃO o inclua no plano: o arquivo é documentação de
                  setup, criada uma vez e mantida por quem cuida do projeto. Planejar
                  CREATE sobre arquivo existente é recusado como conflito e bloqueia a
                  aplicação do LOTE INTEIRO — inclusive dos testes, que estavam corretos.
                - Quando planejar o ".env.example", usar valor VAZIO
                  (ex.: "AUTH_PASSWORD="), nunca valores de exemplo: é a convenção do
                  arquivo, e qualquer literal ali — mesmo fictício como "senha_teste123" —
                  é tratado como possível segredo pela revisão.

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
        if (input.automationType() != null) {
            sb.append("Canal do teste: ").append(input.automationType()).append("\n");
        }

        // A convenção do projeto precisa CHEGAR ao modelo para poder prevalecer
        // sobre o layout padrão. Ela já era coletada na etapa de Conhecimento,
        // mas não era escrita no prompt — então o plano decidia caminhos sem
        // saber como o projeto se organiza.
        var convencao = input.namingConvention();
        if (convencao != null) {
            if (convencao.directoryPattern() != null && !convencao.directoryPattern().isBlank()) {
                sb.append("Padrão de diretórios do projeto: ").append(convencao.directoryPattern()).append("\n");
            }
            if (convencao.testFilePattern() != null && !convencao.testFilePattern().isBlank()) {
                sb.append("Padrão de nome de teste: ").append(convencao.testFilePattern()).append("\n");
            }
            if (convencao.pageObjectPattern() != null && !convencao.pageObjectPattern().isBlank()) {
                sb.append("Padrão de camada de interação: ").append(convencao.pageObjectPattern()).append("\n");
            }
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
