package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
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
                - NUNCA escrever credencial, senha, token, apiKey ou secret literal no
                  código, nem como valor de constante, nem em comentário. Ler sempre de
                  variável de ambiente: process.env.X em TypeScript/JavaScript,
                  System.getenv("X") em Java, %{X} em Robot Framework. A revisão trata
                  segredo hardcoded como achado CRÍTICO e bloqueia a aplicação dos
                  arquivos, então gerar assim descarta a geração inteira.
                - A regra acima vale TAMBÉM para as credenciais PROPOSITALMENTE INVÁLIDAS
                  dos casos negativos. Não escreva const invalidPassword = 'SenhaErrada123'
                  nem equivalente: use process.env.AUTH_INVALID_PASSWORD (ou nome análogo).
                  Qualquer literal atribuído a um campo chamado senha/password/secret/token
                  é reprovado, mesmo que o valor seja falso, de exemplo ou de teste.
                - Não escrever URL de ambiente literal no teste: usar a configuração do
                  projeto (baseURL do playwright.config, variável de ambiente ou
                  equivalente do framework)
                - Quando a chamada usa baseURL, o caminho deve ser RELATIVO, sem barra
                  inicial: use 'v1/auth/login', nunca '/v1/auth/login'. A resolução segue
                  a regra de URL do padrão web, e um caminho com barra inicial DESCARTA o
                  prefixo da base — com baseURL 'http://host/api/' o path '/v1/login' vira
                  'http://host/v1/login' e devolve 404, silenciosamente.
                - Ao documentar variáveis de ambiente (README, .env.example, comentário),
                  colocar entre aspas todo valor que contenha '#', espaço ou caractere
                  especial: em arquivo .env o '#' inicia comentário e trunca o valor —
                  SENHA=abc#123 é lido como 'abc', e o teste falha com credencial inválida
                  sem nenhuma pista do motivo.
                - Quando não for possível gerar algum arquivo planejado, use status "PARTIAL" e inclua um warning explicando o motivo
                - Quando não for possível gerar nenhum arquivo, use status "FAILED" e não inclua itens em "files"

                Qualidade do teste (nível especialista, não apenas "funciona"):
                - NUNCA validar variáveis de ambiente no topo do módulo com throw. Isso
                  roda no CARREGAMENTO do arquivo: quebra a listagem de testes e derruba
                  o arquivo inteiro com erro de coleta, em vez de uma falha de teste
                  legível. Valide dentro de beforeAll ou de uma fixture.
                - Validar o ambiente em UM lugar só; não repita a mesma checagem no teste
                  e na camada de interação.
                - Asserte o status EXATO do contrato (toBe(401)), nunca faixa
                  (>=400 && <500). Faixa esconde regressão: se a API passar a recusar
                  credencial válida com outro código, o teste segue verde.
                - Usar o baseURL do próprio framework em vez de concatenar URL na mão.
                  Montar caminho com template string e regex para aparar barra é fonte
                  clássica de 404. No Playwright, criar o contexto assim:
                    test.beforeAll(async ({ playwright }) => {
                      ctx = await playwright.request.newContext({ baseURL: process.env.X });
                    });
                  ATENÇÃO: a fixture "{ request }" JÁ É um APIRequestContext e NÃO possui
                  newContext — chamar request.newContext() nela lança
                  "TypeError: request.newContext is not a function". Use a fixture
                  "playwright" (playwright.request.newContext) ou o "request" IMPORTADO
                  de '@playwright/test', nunca a fixture de mesmo nome.
                - Se declarar um tipo/interface para a resposta, USE-O: a camada de
                  interação deve devolver esse tipo. Interface declarada e nunca usada é
                  código morto.
                - Toda assertion leva MENSAGEM explicando o esperado:
                  expect(valor, 'token deve ter formato JWT').toMatch(...). Sem ela o
                  relatório diz apenas "expected undefined to be defined", sem indicar
                  campo nem cenário.
                - Preferir UMA assertion de contrato (toMatchObject) a várias campo a
                  campo: descreve a resposta inteira e mostra diff completo ao falhar.
                - Não duplicar assertions equivalentes: "toBeDefined()" seguido de
                  "typeof === 'string'" é redundante — a segunda já implica a primeira.
                - Validar o FORMATO do dado, não só o tipo: token JWT tem três partes
                  separadas por ponto; "typeof === 'string'" passa com string vazia.
                  Para JWT use [A-Za-z0-9_-] em cada parte, NUNCA \\w: base64url inclui
                  hífen, e "\\w+\\.\\w+\\.\\w+" reprova tokens legítimos.
                - Usar EXATAMENTE os nomes de campo e de variável de ambiente informados
                  no cenário ou já presentes no projeto. Não traduza nem "normalize":
                  se o contrato diz "senha" e "usuario", não gere "password" e "user";
                  se o projeto já usa AUTH_USERNAME, não invente AUTH_EMAIL. Nome
                  divergente falha em execução, não na geração — e o erro aponta para o
                  lugar errado.
                - Separar Arrange / Act / Assert com linha em branco, para a leitura
                  distinguir ação de verificação.

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
        if (input.automationType() != null && input.automationType() != AutomationType.UNKNOWN) {
            sb.append("Canal do teste: ").append(input.automationType()).append("\n");
            sb.append("Use a API do framework correspondente a esse canal ")
              .append("(ex.: no Playwright, WEB_UI usa page/locators; API usa request/APIRequestContext).\n");
        }
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
