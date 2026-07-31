Quero implementar um novo módulo chamado AUTO QA BMAD no projeto existente
“criar-cenario-testes”.

Antes de alterar qualquer arquivo, analise toda a estrutura atual do backend
Spring Boot e do frontend Angular.

IMPORTANTE:

1. Não remova nem quebre o fluxo existente de geração de cenários.
2. Não altere contratos existentes sem necessidade.
3. Não apague classes atuais.
4. Reutilize a arquitetura BMAD já existente no projeto.
5. Mantenha compatibilidade com Mac e Windows.
6. Utilize Java 21, Spring Boot e os padrões já adotados no projeto.
7. O projeto de automação que será analisado estará na mesma máquina em que
   o Spring Boot estiver sendo executado.
8. Utilize java.nio.file.Path para trabalhar com caminhos.
9. Nunca monte caminhos utilizando concatenação manual de barras.
10. Antes de criar arquivos, apresente um plano com todos os arquivos que
    serão criados ou alterados.
11. Implemente em pequenas etapas e garanta que o projeto compile após cada
    etapa.
12. Não implemente execução irrestrita de comandos enviados pela IA.
13. Somente comandos definidos e permitidos pelo backend poderão ser
    executados.
14. Não envie arquivos sensíveis, .env, credenciais ou diretórios de build
    para a IA.
15. Inicialmente, os arquivos gerados não devem sobrescrever diretamente o
    projeto de automação.
16. Grave inicialmente em uma pasta temporária:
    .auto-qa/generated/<identificador-da-execucao>/

==================================================
OBJETIVO DO MÓDULO
==================================================

O novo módulo deve permitir que o usuário:

1. Informe o caminho local de um projeto de automação.
2. Selecione ou informe o framework desejado.
3. Selecione ou informe a linguagem.
4. Selecione um cenário de teste já criado no sistema ou informe um cenário.
5. Solicite a análise do projeto de automação.
6. Visualize um plano técnico para implementação do teste.
7. Gere o código da automação.
8. Revise o código antes de aplicá-lo.
9. Aplique o código aprovado no projeto.
10. Execute o teste gerado.
11. Analise possíveis falhas.
12. Gere um relatório final da execução.

O Auto QA deve ser genérico e não preso a apenas um framework.

Na primeira versão, implemente suporte a:

- Playwright com TypeScript;
- Cypress com TypeScript.

A arquitetura deve permitir adicionar futuramente:

- Selenium;
- Selenide;
- Robot Framework;
- RestAssured;
- Playwright em outras linguagens;
- Cypress em JavaScript.

==================================================
ARQUITETURA GERAL
==================================================

O fluxo deve seguir esta estrutura:

Angular
↓
AutoQaController
↓
AutoQaWorkflowService
↓
AutoQaContext
↓
ProjectDiscovery
↓
ProjectAnalysis
↓
AutomationPlanning
↓
CodeGeneration
↓
CodeReview
↓
GeneratedFileStorage
↓
Aprovação do usuário
↓
ApplyGeneratedFiles
↓
TestExecution
↓
FailureAnalysis
↓
Relatório final

Os agentes não devem acessar diretamente Controller, Repository ou arquivos
sem passar pelos serviços responsáveis.

==================================================
NOVA ESTRUTURA DO BACKEND
==================================================

Crie a estrutura seguindo o pacote raiz já utilizado no projeto.

Utilize uma organização semelhante a:

business/
└── autoqa/
├── controller/
│   └── AutoQaController.java
│
├── workflow/
│   ├── AutoQaWorkflowService.java
│   └── AutoQaContext.java
│
├── agent/
│   ├── ProjectDiscoveryAgent.java
│   ├── ProjectAnalysisAgent.java
│   ├── AutomationPlannerAgent.java
│   ├── CodeGenerationAgent.java
│   ├── CodeReviewAgent.java
│   └── FailureAnalysisAgent.java
│
├── framework/
│   ├── AutomationFrameworkAdapter.java
│   ├── AutomationFrameworkResolver.java
│   ├── playwright/
│   │   └── PlaywrightAdapter.java
│   └── cypress/
│       └── CypressAdapter.java
│
├── service/
│   ├── ProjectPathValidationService.java
│   ├── ProjectScannerService.java
│   ├── ProjectCatalogService.java
│   ├── GeneratedFileStorageService.java
│   ├── GeneratedFileApplicationService.java
│   ├── TestExecutionService.java
│   └── CommandPolicyService.java
│
├── model/
│   ├── request/
│   ├── response/
│   ├── context/
│   └── enums/
│
├── prompt/
│   └── AutoQaPromptFactory.java
│
└── exception/
├── InvalidProjectPathException.java
├── UnsupportedFrameworkException.java
├── ProjectAnalysisException.java
└── TestExecutionException.java

Adapte os pacotes ao padrão real encontrado no projeto.

Não crie pacote controller dentro de business caso o projeto já tenha uma
camada controller separada. Primeiro verifique o padrão existente e respeite-o.

==================================================
ENUMS
==================================================

Crie enums semelhantes a:

AutomationFramework:
- PLAYWRIGHT
- CYPRESS
- SELENIUM
- SELENIDE
- ROBOT_FRAMEWORK
- REST_ASSURED
- UNKNOWN

AutomationLanguage:
- TYPESCRIPT
- JAVASCRIPT
- JAVA
- PYTHON
- CSHARP
- ROBOT
- UNKNOWN

AutomationType:
- WEB
- API
- MOBILE
- DESKTOP

AutoQaMode:
- PLAN_ONLY
- GENERATE_FOR_REVIEW
- GENERATE_AND_EXECUTE

AutoQaStatus:
- CREATED
- DISCOVERING_PROJECT
- PROJECT_DISCOVERED
- ANALYZING_PROJECT
- PROJECT_ANALYZED
- PLANNING
- PLAN_READY
- GENERATING
- CODE_GENERATED
- REVIEWING
- REVIEW_APPROVED
- REVIEW_REJECTED
- WAITING_USER_APPROVAL
- APPLYING_FILES
- EXECUTING
- EXECUTION_SUCCESS
- EXECUTION_FAILED
- ANALYZING_FAILURE
- FINISHED
- ERROR

GeneratedFileOperation:
- CREATE
- UPDATE
- DELETE

Na primeira versão, não permita operação DELETE gerada pela IA.

==================================================
REQUEST PRINCIPAL
==================================================

Crie um request semelhante a:

AutoQaRequest:

- String title;
- String scenarioId;
- String scenarioText;
- String projectPath;
- AutomationFramework framework;
- AutomationLanguage language;
- AutomationType automationType;
- AutoQaMode mode;
- boolean executeAfterGeneration;
- boolean allowFileUpdate;

Regras:

1. projectPath é obrigatório.
2. Deve existir scenarioId ou scenarioText.
3. Framework pode ser informado ou detectado.
4. Linguagem pode ser informada ou detectada.
5. allowFileUpdate deve ser false por padrão.
6. executeAfterGeneration deve ser false por padrão.
7. Não permitir execução automática antes da revisão do código.

==================================================
AUTO QA CONTEXT
==================================================

Crie uma classe AutoQaContext para armazenar todo o estado do workflow.

Ela deve conter pelo menos:

- UUID executionId;
- AutoQaRequest request;
- Path normalizedProjectPath;
- AutoQaStatus status;
- ProjectDiscoveryResult discoveryResult;
- ProjectAnalysisResult projectAnalysis;
- AutomationPlan automationPlan;
- List<GeneratedFile> generatedFiles;
- CodeReviewResult codeReviewResult;
- TestExecutionResult executionResult;
- FailureAnalysisResult failureAnalysisResult;
- List<WorkflowIssue> issues;
- List<WorkflowLog> workflowLogs;
- LocalDateTime startedAt;
- LocalDateTime finishedAt;

Crie métodos seguros para atualizar o status e registrar logs.

Não exponha Path diretamente no JSON de resposta sem conversão adequada
para String.

==================================================
VALIDAÇÃO DO CAMINHO
==================================================

Implemente ProjectPathValidationService utilizando java.nio.file.Path.

Exemplo conceitual:

Path projectPath = Path.of(request.projectPath())
.toAbsolutePath()
.normalize();

Validações obrigatórias:

1. O caminho não pode ser vazio.
2. O caminho deve existir.
3. O caminho deve ser um diretório.
4. O processo Java deve possuir permissão de leitura.
5. Para aplicar arquivos, o processo deve possuir permissão de escrita.
6. Não permitir que o projeto de automação seja a própria raiz do sistema.
7. Não permitir caminhos como apenas:
    - /
    - C:\
    - diretório pessoal inteiro do usuário.
8. Não permitir acesso fora das raízes configuradas quando a configuração
   de raízes permitidas estiver ativa.
9. Resolver caminhos relativos com segurança.
10. Validar possíveis tentativas de path traversal.

Crie configuração opcional no application.yml:

auto-qa:
enabled: true
allowed-roots: []
max-files: 500
max-file-size-kb: 500
max-total-content-kb: 5000
max-generation-retries: 3
max-execution-minutes: 10
generated-directory: .auto-qa/generated
backup-directory: .auto-qa/backups
allow-command-execution: false
allow-file-application: false

Quando allowed-roots estiver vazio, permita projetos locais, mas continue
bloqueando diretórios críticos.

==================================================
PROJECT DISCOVERY
==================================================

Implemente ProjectDiscoveryAgent e os serviços determinísticos necessários.

A descoberta não deve depender apenas da IA.

Detecte Playwright por arquivos como:

- playwright.config.ts
- playwright.config.js
- playwright.config.mts
- playwright.config.mjs
- dependência @playwright/test no package.json

Detecte Cypress por:

- cypress.config.ts
- cypress.config.js
- dependência cypress no package.json

Detecte gerenciador de pacotes por:

- package-lock.json → NPM
- yarn.lock → YARN
- pnpm-lock.yaml → PNPM

Detecte TypeScript por:

- tsconfig.json
- arquivos .ts
- dependências typescript

Detecte JavaScript por:

- arquivos .js
- ausência de configuração TypeScript

O resultado da descoberta deve conter:

- framework informado;
- framework detectado;
- linguagem informada;
- linguagem detectada;
- gerenciador de pacotes;
- arquivo de configuração;
- comandos sugeridos;
- evidências utilizadas na detecção;
- divergências encontradas;
- avisos.

Caso framework informado e detectado sejam diferentes, não prossiga
automaticamente. Retorne a divergência para o usuário.

==================================================
FRAMEWORK ADAPTERS
==================================================

Crie a interface AutomationFrameworkAdapter.

Ela deve fornecer pelo menos:

- AutomationFramework getFramework();
- Set<AutomationLanguage> supportedLanguages();
- boolean supports(AutomationLanguage language);
- List<String> configurationFiles();
- List<String> importantDirectories();
- List<String> ignoredDirectories();
- String buildFrameworkInstructions(ProjectAnalysisResult analysis);
- List<AllowedCommand> validationCommands(ProjectDiscoveryResult discovery);
- List<AllowedCommand> testCommands(ProjectDiscoveryResult discovery);
- String defaultTestFilePattern();
- String defaultTestDirectory();

Crie:

PlaywrightAdapter
CypressAdapter

O agente principal continua genérico.

As regras específicas devem ficar nos adapters e nos arquivos de perfil.

==================================================
PERFIL PLAYWRIGHT
==================================================

Crie:

frameworks/playwright.profile.md

Regras mínimas:

- respeitar a estrutura existente;
- reutilizar Page Objects;
- reutilizar fixtures;
- utilizar @playwright/test;
- preferir getByRole, getByTestId, getByLabel e locators estáveis;
- não utilizar waitForTimeout como sincronização;
- não utilizar XPath sem necessidade;
- utilizar expect do Playwright;
- não inventar métodos;
- não duplicar métodos existentes;
- respeitar baseURL;
- utilizar variáveis de ambiente conforme o padrão encontrado;
- não inserir credenciais diretamente;
- utilizar arquivos .spec.ts em projetos TypeScript;
- utilizar test.describe quando fizer sentido;
- não acessar propriedades privadas dos Page Objects;
- considerar setup, teardown, screenshot, video e trace já configurados.

==================================================
PERFIL CYPRESS
==================================================

Crie:

frameworks/cypress.profile.md

Regras mínimas:

- respeitar a estrutura existente;
- reutilizar custom commands;
- reutilizar fixtures;
- reutilizar Page Objects, quando o projeto adotar esse padrão;
- preferir seletores data-cy, data-testid ou seletores estáveis;
- não utilizar cy.wait com tempo fixo como sincronização;
- utilizar interceptações quando necessário;
- não inventar comandos customizados;
- respeitar baseUrl;
- não inserir credenciais diretamente;
- utilizar arquivos .cy.ts em projetos TypeScript;
- utilizar describe e it conforme o padrão existente;
- validar respostas e elementos de maneira determinística.

==================================================
PROJECT SCANNER
==================================================

Implemente ProjectScannerService.

Ele deve criar um mapa da estrutura do projeto sem carregar arquivos
desnecessários.

Ignore sempre:

- node_modules
- .git
- dist
- build
- target
- out
- coverage
- playwright-report
- test-results
- blob-report
- allure-results
- allure-report
- cypress/videos
- cypress/screenshots
- .idea
- .vscode
- .gradle
- logs
- arquivos binários
- arquivos compactados
- .env
- chaves privadas
- certificados
- arquivos com possíveis segredos

Considere prioritários:

- package.json
- playwright.config.*
- cypress.config.*
- tsconfig.json
- README.md
- arquivos de configuração
- tests
- test
- e2e
- pages
- pageobjects
- fixtures
- support
- commands
- helpers
- utils
- models
- clients
- data

Implemente limites configuráveis de:

- quantidade de arquivos;
- tamanho individual;
- volume total de texto;
- profundidade máxima de diretórios.

==================================================
PROJECT ANALYSIS
==================================================

Implemente ProjectAnalysisAgent.

Ele deve analisar o catálogo obtido pelo scanner e produzir:

- estrutura das pastas;
- padrões de nomenclatura;
- Page Objects encontrados;
- classes encontradas;
- métodos públicos;
- parâmetros;
- tipos de retorno;
- fixtures;
- hooks;
- comandos customizados;
- helpers;
- dados de teste;
- configuração de ambiente;
- testes existentes;
- convenções de assertions;
- convenções de imports;
- possíveis componentes reutilizáveis;
- lacunas técnicas.

Para TypeScript, inicialmente pode utilizar análise de texto estruturada,
mas organize a implementação para permitir futuramente análise AST.

Não invente classe ou método que não esteja no catálogo.

Exemplo de método que o analisador deve catalogar:

async login(
email: string,
password: string
): Promise<AccountPage>

O catálogo deve informar:

- classe: LoginPage;
- método: login;
- parâmetros: email e password;
- retorno: Promise<AccountPage>;
- arquivo em que foi localizado.

==================================================
AUTOMATION PLANNER
==================================================

Implemente AutomationPlannerAgent.

Ele deve receber:

- cenário funcional;
- descoberta do projeto;
- análise do projeto;
- framework adapter;
- regras do agente Auto QA.

Ele deve produzir um AutomationPlan estruturado contendo:

- nome do teste;
- objetivo;
- tipo de teste;
- prioridade;
- pré-condições;
- dados necessários;
- componentes existentes que serão reutilizados;
- classes existentes utilizadas;
- métodos existentes utilizados;
- arquivos que serão criados;
- arquivos que poderão ser atualizados;
- assertions;
- riscos;
- pendências;
- elementos que não foram encontrados;
- necessidade de criação de novo Page Object;
- necessidade de intervenção do usuário.

O Planner não deve gerar o código final.

Caso falte informação essencial, marque o plano como bloqueado e não avance
para geração.

==================================================
APROVAÇÃO DO PLANO
==================================================

Crie fluxo em duas etapas:

1. Analisar e planejar.
2. Gerar somente após aprovação.

Endpoints sugeridos:

POST /api/auto-qa/analyze
POST /api/auto-qa/{executionId}/generate
POST /api/auto-qa/{executionId}/apply
POST /api/auto-qa/{executionId}/execute
GET  /api/auto-qa/{executionId}
GET  /api/auto-qa/{executionId}/generated-files
GET  /api/auto-qa/{executionId}/generated-files/content
POST /api/auto-qa/{executionId}/discard

Adapte os caminhos ao padrão já utilizado no projeto.

Na primeira chamada, não gere nem aplique arquivos automaticamente.

==================================================
CODE GENERATION AGENT
==================================================

Implemente CodeGenerationAgent.

O prompt deve ser montado a partir de:

1. Instruções gerais do Auto QA.
2. Perfil do framework.
3. Descoberta do projeto.
4. Análise do projeto.
5. Catálogo de classes e métodos existentes.
6. Plano aprovado.
7. Cenário funcional.
8. Convenções encontradas.
9. Restrições de segurança.

A resposta da IA deve ser estruturada.

Crie DTO semelhante a:

GeneratedCodeResponse:
- List<GeneratedFile> files;
- List<String> reusedComponents;
- List<String> missingComponents;
- List<String> warnings;
- String summary;

GeneratedFile:
- String relativePath;
- GeneratedFileOperation operation;
- String content;
- String explanation;
- String originalHash;
- String generatedHash;

Regras:

1. relativePath deve ser sempre relativo à raiz do projeto.
2. Não aceitar caminho absoluto retornado pela IA.
3. Não aceitar “../”.
4. Não permitir escrita fora do projeto.
5. Não permitir DELETE inicialmente.
6. Não aplicar arquivos nesta etapa.
7. Gravar apenas em:
   .auto-qa/generated/<executionId>/
8. Validar se o JSON retornado pela IA é válido.
9. Caso necessário, utilizar o parser já existente no projeto ou criar um
   parser específico.
10. Limitar tentativas de correção da resposta.

==================================================
CODE REVIEW AGENT
==================================================

Implemente CodeReviewAgent.

Ele deve verificar:

- imports;
- classes;
- métodos;
- assinaturas;
- caminhos;
- extensões;
- duplicações;
- uso de APIs incompatíveis;
- dados sensíveis;
- credenciais hardcoded;
- esperas fixas;
- métodos inventados;
- arquivos fora do plano;
- alterações não autorizadas;
- aderência ao perfil do framework;
- assertions;
- padrão do projeto.

Crie:

CodeReviewResult:
- boolean approved;
- List<CodeReviewIssue> issues;
- List<String> suggestions;
- int revisionNumber;

CodeReviewIssue:
- severity: INFO, WARNING, ERROR;
- file;
- line;
- code;
- message;
- suggestion;

Não aprove o código quando existir issue de severidade ERROR.

Permita no máximo três ciclos:

CodeGeneration
↓
CodeReview
↓
Correção
↓
CodeReview

Se continuar reprovado, interrompa e devolva os erros ao usuário.

==================================================
ARMAZENAMENTO DOS ARQUIVOS GERADOS
==================================================

Implemente GeneratedFileStorageService.

Os arquivos devem ser armazenados em:

<projectPath>/.auto-qa/generated/<executionId>/

Exemplo:

.auto-qa/
└── generated/
└── 123e4567/
├── manifest.json
├── generation-report.md
└── files/
└── tests/
└── login/
└── login.spec.ts

O manifest deve conter:

- executionId;
- projeto;
- framework;
- linguagem;
- cenário;
- data;
- arquivos;
- operações;
- hashes;
- revisão;
- status.

Não grave dados sensíveis no relatório.

==================================================
APLICAÇÃO DOS ARQUIVOS
==================================================

Implemente GeneratedFileApplicationService.

Somente aplique arquivos quando:

1. codeReviewResult.approved for true;
2. usuário chamar explicitamente o endpoint de aplicação;
3. auto-qa.allow-file-application estiver habilitado;
4. os caminhos forem válidos;
5. os hashes forem compatíveis;
6. nenhuma alteração externa tiver ocorrido desde a análise.

Antes de atualizar arquivo existente:

1. crie backup em:
   .auto-qa/backups/<executionId>/
2. registre hash anterior;
3. grave de maneira segura;
4. registre hash posterior.

Para a primeira versão:

- permita CREATE;
- permita UPDATE somente quando allowFileUpdate for true;
- nunca permita DELETE;
- não sobrescreva arquivos sem backup.

==================================================
EXECUÇÃO SEGURA
==================================================

Implemente CommandPolicyService e TestExecutionService.

A IA não pode retornar um comando livre e mandar o backend executá-lo.

Os comandos devem ser montados pelo framework adapter.

Comandos permitidos inicialmente:

Playwright TypeScript:

- npm exec tsc -- --noEmit
- npx tsc --noEmit
- npx playwright test <arquivo-específico>

Cypress TypeScript:

- npm exec tsc -- --noEmit
- npx tsc --noEmit
- npx cypress run --spec <arquivo-específico>

Considere npm, yarn e pnpm conforme o projeto detectado.

Utilize ProcessBuilder.

Não utilize:

- Runtime.exec com String livre;
- shell concatenado;
- eval;
- bash -c com texto fornecido pela IA;
- cmd /c com texto fornecido pela IA.

Configure:

- working directory no projeto;
- timeout;
- captura de stdout;
- captura de stderr;
- exit code;
- duração;
- cancelamento em caso de timeout.

Para Windows, considere os executáveis adequados, como:

- npm.cmd
- npx.cmd

Para Mac/Linux:

- npm
- npx

Crie abstração para resolver executáveis por sistema operacional.

==================================================
FAILURE ANALYSIS
==================================================

Implemente FailureAnalysisAgent.

Ele deve receber:

- comando lógico executado;
- exit code;
- stdout;
- stderr;
- arquivos gerados;
- análise do projeto;
- plano;
- revisão.

Produza:

FailureAnalysisResult:
- failureType;
- summary;
- probableCause;
- affectedFiles;
- suggestedChanges;
- boolean canRetryAutomatically;
- boolean requiresUserIntervention;

Tipos iniciais:

- COMPILATION_ERROR
- IMPORT_ERROR
- NON_EXISTENT_METHOD
- LOCATOR_ERROR
- ASSERTION_ERROR
- TIMEOUT
- ENVIRONMENT_ERROR
- DEPENDENCY_ERROR
- CONFIGURATION_ERROR
- APPLICATION_ERROR
- UNKNOWN

Não altere automaticamente o código original.

Uma correção automática deve:

1. gerar nova versão na pasta temporária;
2. passar novamente pelo CodeReviewAgent;
3. exigir nova aprovação antes de aplicar;
4. respeitar o limite de tentativas.

==================================================
AGENTES BMAD EM MARKDOWN
==================================================

Crie os arquivos:

agents/auto-qa/auto_qa_orchestrator.agent.md
agents/auto-qa/project_discovery.agent.md
agents/auto-qa/project_analysis.agent.md
agents/auto-qa/automation_planner.agent.md
agents/auto-qa/code_generator.agent.md
agents/auto-qa/code_reviewer.agent.md
agents/auto-qa/failure_analysis.agent.md

Crie também:

workflows/auto_qa.workflow.md
frameworks/playwright.profile.md
frameworks/cypress.profile.md

Cada agente deve conter:

- nome;
- papel;
- objetivo;
- entradas;
- responsabilidades;
- regras;
- restrições;
- formato de saída;
- critérios de conclusão;
- situações em que deve interromper o fluxo.

Reutilize o AgentLoaderService já existente, caso seja compatível.

Se não for compatível, faça uma evolução sem quebrar os agentes atuais.

==================================================
WORKFLOW BMAD
==================================================

O workflow AUTO_QA deve seguir:

1. VALIDATE_REQUEST
2. VALIDATE_PROJECT_PATH
3. DISCOVER_PROJECT
4. CHECK_FRAMEWORK_CONSISTENCY
5. SCAN_PROJECT
6. ANALYZE_PROJECT
7. CREATE_AUTOMATION_PLAN
8. WAIT_FOR_PLAN_APPROVAL
9. GENERATE_CODE
10. REVIEW_CODE
11. SAVE_GENERATED_FILES
12. WAIT_FOR_APPLICATION_APPROVAL
13. APPLY_FILES
14. WAIT_FOR_EXECUTION_APPROVAL
15. EXECUTE_VALIDATION
16. EXECUTE_TEST
17. ANALYZE_FAILURE_IF_NEEDED
18. BUILD_FINAL_REPORT
19. FINISH

O workflow deve permitir pausa entre:

- planejamento e geração;
- geração e aplicação;
- aplicação e execução.

==================================================
PERSISTÊNCIA
==================================================

Analise a estrutura atual do MongoDB e dos repositories.

Crie persistência para a execução Auto QA sem misturar diretamente com o
documento de cenário funcional.

Crie uma entidade/documento semelhante a:

AutoQaExecutionDocument:
- executionId;
- scenarioId;
- title;
- projectPath;
- framework;
- language;
- mode;
- status;
- discoveryResult;
- automationPlan;
- generatedFileMetadata;
- codeReviewResult;
- executionResult;
- failureAnalysisResult;
- issues;
- createdAt;
- updatedAt;
- finishedAt;

Não salve o conteúdo completo de todo o projeto no MongoDB.

Não salve credenciais.

O conteúdo dos arquivos gerados pode permanecer no filesystem, enquanto o
MongoDB guarda metadados e caminhos relativos.

Caso o MongoDB esteja indisponível, trate a falha claramente e não finja que
a execução foi salva.

==================================================
FRONTEND ANGULAR
==================================================

Antes de implementar, analise a estrutura atual do Angular e respeite o
padrão de componentes, services, rotas e formulários já utilizado.

Crie uma nova funcionalidade “Auto QA”.

Campos da tela:

- cenário existente ou texto do cenário;
- título;
- pasta local do projeto;
- tipo de automação;
- framework;
- linguagem;
- modo;
- permitir atualização de arquivos;
- executar após aprovação.

Botões:

- Analisar projeto;
- Gerar plano;
- Aprovar plano e gerar;
- Visualizar arquivos;
- Aprovar e aplicar;
- Executar teste;
- Descartar geração.

A tela deve mostrar etapas:

1. Projeto
2. Descoberta
3. Análise
4. Plano
5. Geração
6. Revisão
7. Aplicação
8. Execução
9. Resultado

Exiba:

- framework informado;
- framework detectado;
- linguagem;
- arquivo de configuração;
- comandos detectados;
- classes encontradas;
- Page Objects;
- métodos reutilizáveis;
- plano técnico;
- arquivos gerados;
- conteúdo dos arquivos;
- issues da revisão;
- stdout;
- stderr;
- resultado final.

IMPORTANTE SOBRE O CAMPO DA PASTA:

O Angular apenas envia o texto do caminho local informado pelo usuário.

Exemplo Mac:

/Users/jeanheberth/Development/qa/automacao-playwright

Exemplo Windows:

C:\Development\qa\automacao-playwright

A aplicação não deve afirmar que consegue obter automaticamente o caminho
absoluto por um seletor de pasta do navegador.

Adicione textos explicativos informando que o Spring Boot e o projeto de
automação devem estar na mesma máquina.

==================================================
ENDPOINT DE VALIDAÇÃO RÁPIDA DA PASTA
==================================================

Crie endpoint para validar o caminho antes de iniciar o workflow.

Exemplo:

POST /api/auto-qa/project/validate

Request:

{
"projectPath": "/Users/usuario/projetos/automacao"
}

Response:

{
"valid": true,
"normalizedPath": "/Users/usuario/projetos/automacao",
"readable": true,
"writable": true,
"detectedFramework": "PLAYWRIGHT",
"detectedLanguage": "TYPESCRIPT",
"warnings": []
}

Nunca exponha informações desnecessárias sobre outros diretórios do sistema.

==================================================
TRATAMENTO DE ERROS
==================================================

Utilize o tratamento global de exceções já existente no projeto.

Crie mensagens claras para:

- pasta inexistente;
- caminho inválido;
- sem permissão;
- framework não suportado;
- framework divergente;
- linguagem incompatível;
- projeto muito grande;
- arquivo muito grande;
- resposta inválida da IA;
- plano bloqueado;
- revisão reprovada;
- aplicação desabilitada;
- execução desabilitada;
- timeout;
- comando não permitido;
- falha ao criar backup;
- falha ao escrever arquivo;
- falha de conexão com MongoDB;
- falha de conexão com OpenAI ou Gemini.

==================================================
LOGS
==================================================

Adicione logs úteis sem registrar credenciais ou conteúdo sensível.

Exemplos:

- executionId;
- título;
- framework;
- linguagem;
- status;
- caminho normalizado;
- quantidade de arquivos analisados;
- quantidade de classes encontradas;
- quantidade de arquivos gerados;
- comando lógico executado;
- exit code;
- duração;
- quantidade de tentativas.

Não registre:

- senhas;
- tokens;
- conteúdo de .env;
- API keys;
- dados confidenciais de testes.

==================================================
TESTES AUTOMATIZADOS DO NOVO MÓDULO
==================================================

Crie testes unitários para:

1. Validação de caminho Mac.
2. Validação de caminho Windows, usando caminhos simulados quando necessário.
3. Bloqueio de path traversal.
4. Detecção de Playwright.
5. Detecção de Cypress.
6. Detecção de TypeScript.
7. Divergência entre framework informado e detectado.
8. Exclusão de node_modules.
9. Exclusão de .env.
10. Limites de arquivos.
11. Resolver de adapters.
12. Compatibilidade framework e linguagem.
13. Validação de relativePath gerado pela IA.
14. Bloqueio de caminhos absolutos.
15. Bloqueio de DELETE.
16. Geração na pasta temporária.
17. Criação de backup.
18. Política de comandos permitidos.
19. Timeout de execução.
20. Revisão com método inexistente.
21. Workflow interrompido por plano bloqueado.
22. Workflow interrompido por revisão reprovada.

Use JUnit 5, Mockito e as dependências já existentes.

Não execute Playwright ou Cypress reais nos testes unitários.

Utilize diretórios temporários com @TempDir.

==================================================
DOCUMENTAÇÃO
==================================================

Crie:

docs/auto-qa/README.md
docs/auto-qa/architecture.md
docs/auto-qa/workflow.md
docs/auto-qa/security.md
docs/auto-qa/mac-windows.md

Inclua:

- visão geral;
- responsabilidades dos agentes;
- fluxo;
- estrutura;
- configuração;
- como utilizar no Mac;
- como utilizar no Windows;
- como cadastrar um projeto;
- como adicionar um novo framework adapter;
- segurança;
- limitações da primeira versão.

==================================================
ORDEM OBRIGATÓRIA DE IMPLEMENTAÇÃO
==================================================

Implemente em fases.

FASE 1 — Estrutura e contratos

- enums;
- requests;
- responses;
- AutoQaContext;
- configuração;
- validação de caminho;
- endpoint de validação;
- testes da validação.

FASE 2 — Descoberta

- AutomationFrameworkAdapter;
- resolver;
- PlaywrightAdapter;
- CypressAdapter;
- ProjectDiscoveryAgent;
- testes de detecção.

FASE 3 — Scanner e análise

- scanner;
- filtros;
- limites;
- catálogo;
- ProjectAnalysisAgent;
- testes.

FASE 4 — Planejamento

- AutomationPlannerAgent;
- AutomationPlan;
- persistência inicial;
- endpoint analyze;
- testes.

FASE 5 — Geração

- perfis markdown;
- CodeGenerationAgent;
- parser da resposta;
- armazenamento temporário;
- testes.

FASE 6 — Revisão

- CodeReviewAgent;
- regras de revisão;
- limite de tentativas;
- testes.

FASE 7 — Frontend

- service;
- models;
- rota;
- componente;
- formulário;
- visualização do plano;
- visualização do código;
- mensagens de erro.

FASE 8 — Aplicação

- aprovação explícita;
- backup;
- CREATE;
- UPDATE opcional;
- hashes;
- testes.

FASE 9 — Execução

- política de comandos;
- ProcessBuilder;
- timeout;
- stdout;
- stderr;
- Playwright;
- Cypress;
- testes.

FASE 10 — Análise de falha e relatório

- FailureAnalysisAgent;
- relatório final;
- documentação.

Após cada fase:

1. Compile o backend.
2. Execute os testes.
3. Compile o frontend.
4. Informe os arquivos criados.
5. Informe os arquivos alterados.
6. Informe eventuais pendências.
7. Não avance automaticamente para a próxima fase sem confirmar que a fase
   atual está compilando.

==================================================
PRIMEIRA TAREFA
==================================================

Neste momento, implemente SOMENTE A FASE 1.

Antes de alterar qualquer arquivo:

1. Analise a estrutura real do projeto.
2. Identifique os padrões atuais.
3. Liste todos os arquivos que pretende criar.
4. Liste todos os arquivos que pretende alterar.
5. Explique como evitará quebrar o workflow atual de geração de cenários.
6. Aguarde minha aprovação antes de realizar as alterações.

Depois da minha aprovação:

1. Implemente a FASE 1.
2. Envie o conteúdo completo de cada arquivo criado ou alterado.
3. Não envie apenas trechos.
4. Garanta que os imports estejam corretos.
5. Execute ou informe os comandos de compilação e teste adequados.
6. Corrija todos os erros encontrados antes de considerar a fase concluída.