package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
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
                - esta etapa é ANÁLISE, não implementação: descreva passos em
                  linguagem natural. O código do projeto aparece no contexto
                  apenas para você entender as convenções existentes - copiá-lo
                  (ou escrever trechos novos) faz a resposta inteira ser
                  descartada pelo validador
                - classificar dados sensíveis como SECRET
                - registrar ambiguidades de forma explícita
                - ambiguidades BLOQUEANTES (blocking=true): informação sem a qual
                  é impossível criar qualquer teste útil — por exemplo, não há
                  nenhum passo descrito, ou a ação principal é completamente
                  indefinida. Use com parcimônia.
                - endpoint, URL, host ou porta NÃO informados NÃO são bloqueantes:
                  o teste lê esses valores da configuração do projeto (baseURL,
                  variável de ambiente), então dá para automatizar sem eles.
                  Registre como ambiguidade com blocking=false.
                - ambiguidades NÃO BLOQUEANTES (blocking=false): detalhes que
                  melhorariam o teste mas que permitem criar uma automação
                  razoável com base no contexto do projeto — por exemplo, formato
                  exato da resposta, campos opcionais, validações secundárias.
                  Na dúvida, prefira blocking=false.
                - usar português do Brasil
                - campos marcados como ["string"] (preconditions, entities,
                  dependencies, warnings) são listas de TEXTO SIMPLES: cada
                  item é uma string, nunca um objeto. Observado na prática:
                  com entradas longas o modelo passa a enriquecê-los como
                  {"description": "..."}, e a resposta inteira é descartada
                  no parse por incompatibilidade de tipo.

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
                      "blocking": false
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
        return createUserPrompt(scenario, discovery, null);
    }

    /**
     * @param informedAutomationType canal escolhido pelo usuário, ou null.
     *
     * Quando informado, entra no prompt como fato — e não como mais um dado de
     * descoberta. Sem isso o modelo trata "canal não informado" como
     * ambiguidade BLOQUEANTE e a análise inteira é reprovada antes do
     * planejamento; resolver o campo depois da resposta não desfaz a
     * ambiguidade que ele já registrou.
     */
    public String createUserPrompt(String scenario, ProjectDiscoveryResult discovery,
                                    AutomationType informedAutomationType) {
        String detectedFrameworks = discovery.getDetectedFrameworks().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String testingFrameworks = discovery.getTestingFrameworks().stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String warnings = discovery.getWarnings().isEmpty()
                ? "Nenhum"
                : String.join(" | ", discovery.getWarnings());

        String canal = (informedAutomationType == null || informedAutomationType == AutomationType.UNKNOWN)
                ? ""
                : """

                        Canal de automação DEFINIDO pelo usuário: %s
                        Use exatamente esse valor em "automationType". O canal
                        está decidido — não registre ambiguidade perguntando se
                        os testes são de web, mobile ou API.
                        """.formatted(informedAutomationType.name());

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
                %s
                Responda somente com JSON puro.
                """.formatted(
                scenario,
                discovery.getAutomationFramework(),
                discovery.getLanguage(),
                discovery.getBuildTool(),
                discovery.getPackageManager(),
                testingFrameworks.isBlank() ? "Nenhum" : testingFrameworks,
                detectedFrameworks.isBlank() ? "Nenhum" : detectedFrameworks,
                warnings,
                canal
        );
    }
}
