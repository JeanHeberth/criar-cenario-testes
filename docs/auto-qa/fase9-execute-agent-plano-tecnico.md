# Fase 9 — Execute Agent: Plano Técnico (aguardando aprovação)

## 1. Diagnóstico da arquitetura atual

- **`AutoQaWorkflowService`** já é agnóstico a agentes concretos: recebe `List<AutoQaAgent> agents` via injeção Spring, que ordena automaticamente por `@Order` na injeção de lista de beans do mesmo tipo. **Nenhuma alteração é necessária nele** — `ExecuteAgent` só precisa de `@Component` + `@Order(70)` para entrar no fluxo na posição correta. Confirmado lendo o código: o loop chama `agent.execute(context)`, registra o `AgentExecutionResult`, e para o workflow no primeiro agente com `success()==false` ou exceção — não há acoplamento a tipos concretos.
- **Padrão dos agentes existentes** (`ReviewAgent`, `GenerationAgent`, `ApplyAgent`): guarda de pré-condições lida diretamente do `AutoQaContext` (sem chamar o service se faltar algo), delega a um service stateless, captura exceções de validação e converte em `AgentExecutionResult.failure(...)`, sempre loga início/fim com `executionId`.
- **Enums de discovery já existentes e reutilizáveis**: `AutomationFramework` (PLAYWRIGHT, CYPRESS, SELENIDE, SELENIUM, REST_ASSURED, ROBOT_FRAMEWORK, APPIUM, KARATE, PACT, UNKNOWN), `BuildTool` (GRADLE, MAVEN, NPM, YARN, PNPM, ROBOT, UNKNOWN), `PackageManager` (NPM, YARN, PNPM, PIP, POETRY, UNKNOWN). `ProjectDiscoveryResult` já expõe `normalizedProjectPath`, `configurationFile`, `evidenceFiles` — evidências que o `CommandResolver` pode usar sem inventar nada.
- **`ProjectKnowledgeResult`** já expõe `testDirectories()`/`sourceDirectories()` (List<String>) — úteis para resolver o argumento de diretório do Robot/Pytest sem adivinhação.
- **Padrão de segurança já estabelecido na Fase 8** (`ApplyPathResolver`, redação por regex em `CodeReviewInputSanitizer`) será reaproveitado: mesmo estilo de allowlist/denylist via `Pattern`, mesmo estilo de exceção por pacote (`XxxException extends RuntimeException` com hierarquia), mesmo padrão de record imutável com compact constructor que só faz validação estrutural (não corrige status).
- **Nenhum mecanismo de execução de processo existe hoje no projeto Auto QA** — Fase 9 introduz `ProcessBuilder` pela primeira vez, restrito ao pacote `execution`.

## 2. Arquivos que serão criados

**`model/execution/`** (7): `ExecutionApproval.java`, `ExecutionResult.java`, `CommandSpecification.java`, `ExecutionCommandType.java`, `ExecutionStatus.java`, `TestExecutionSummary.java`, `ExecutionWarning.java`

**`execution/`** (9 serviços + 6 exceções): `TestExecutionService.java`, `CommandPolicyService.java`, `CommandResolver.java`, `ProcessExecutionService.java`, `ProcessOutputCollector.java`, `ExecutionResultParser.java`, `ExecutionPreconditionValidator.java`, `ExecutionSummaryBuilder.java`, `ProcessTerminationService.java`
`exception/ExecutionException.java`, `exception/ExecutionValidationException.java`, `exception/CommandNotAllowedException.java`, `exception/ProcessStartException.java`, `exception/ProcessTimeoutException.java`, `exception/ProcessTerminationException.java`

**`agent/`** (1): `ExecuteAgent.java`

**Testes** (1:1 com cada classe acima) + `workflow/AutoQaWorkflowExecutionIntegrationTest.java`

## 3. Arquivos que serão alterados

```
AutoQaContext.java       — + executionApproval, executionResult, getters, registerExecutionApproval, registerExecutionResult
AutoQaContextTest.java   — + testes dos novos registros
```
Nada além disso — nenhum contrato das Fases 1-8 é tocado.

## 4. Contrato de `ExecutionApproval`

```java
record ExecutionApproval(
    boolean approved,
    String approvedBy,
    LocalDateTime approvedAt,
    List<String> allowedCommands,
    boolean allowTestExecution,
    boolean allowInstallCommand,
    boolean allowBuildCommand
)
```
- `approvedBy`/`approvedAt` obrigatórios no compact constructor (mesmo padrão de `ApplyApproval` — trusted input, nunca gerado por IA).
- `allowedCommands` é uma **allowlist de rótulos canônicos** definidos pelo `CommandPolicyService` (ex.: `"GRADLE_WRAPPER_TEST"`, `"NPM_TEST"`, `"PLAYWRIGHT_TEST"`), não texto livre — funciona como uma segunda porta de controle: o rótulo do comando resolvido pelo `CommandResolver` precisa estar em `allowedCommands`, senão `BLOCKED`/`COMMAND_NOT_ALLOWED`, mesmo que o comando em si esteja na allowlist estática do serviço.
- `allowTestExecution=true` é obrigatório para qualquer execução prosseguir; `allowBuildCommand` só libera variantes como `./gradlew clean test`; `allowInstallCommand` existe no contrato mas **nesta fase nenhum comando de install está na allowlist estática** — é reservado para uso futuro, sem efeito prático agora (decisão explícita, não overengineering).
- `registerExecutionApproval` no contexto: rejeita null, exige `ApplyResult` já registrado, exige `approved=true`, rejeita segundo registro — mesmo padrão de `registerApplyApproval`.

## 5. Contrato de `CommandSpecification`

```java
record CommandSpecification(
    String executable,
    List<String> arguments,
    String workingDirectoryReference,
    Duration timeout,
    Map<String, String> environment,
    ExecutionCommandType type
)
```
- `executable` validado contra allowlist fixa (`./gradlew`, `gradlew.bat`, `./mvnw`, `mvnw.cmd`, `npm`/`npm.cmd`, `npx`/`npx.cmd`, `python`, `python3`, `py`, `pytest`, `robot`) — nunca texto livre.
- `arguments` imutável (`List.copyOf`), cada argumento também validado (sem `;`, `|`, `&&`, `||`, `` ` ``, `$(`, `>`, `<`).
- `workingDirectoryReference` é uma **referência sanitizada** (ex.: nome final do diretório, como já feito em `ApplySummaryBuilder.sanitizeProjectRoot`) — nunca o path absoluto; o path absoluto real só existe internamente em `ProcessExecutionService`, passado por parâmetro separado, nunca armazenado no record que compõe o `ExecutionResult` público.
- `timeout` obrigatório, validado entre 1s e 30min (default 10min) por quem constrói (`CommandResolver`/`TestExecutionService`), não pelo compact constructor do record (regra: registros não corrigem, apenas guardam — a validação de faixa é responsabilidade de quem monta).
- `environment` nunca contém chaves cujo nome bata no padrão `(?i)(key|token|secret|password|credential)` — redigido antes de entrar no record.

## 6. Comandos permitidos por framework

| Framework/Build tool | Comando(s) permitido(s) | Condição |
|---|---|---|
| PLAYWRIGHT/NPM | `npm test` | script `test` existe em `package.json` (evidência via `ProjectDiscoveryResult`/knowledge) |
| | `npx playwright test` | sempre, se framework=PLAYWRIGHT |
| | `npm run test:e2e` | só se detectado E autorizado explicitamente em `allowedCommands` |
| CYPRESS | `npx cypress run` | sempre |
| | `npm run cy:run` / `npm test` | se script conhecido |
| GRADLE | `./gradlew test` (ou `gradlew.bat test` no Windows) | wrapper presente (prioridade máxima) |
| | `./gradlew clean test` | só se `allowBuildCommand=true` |
| MAVEN | `./mvnw test` (ou `mvnw.cmd test`) | wrapper presente (prioridade máxima) |
| | `mvn test` | só se wrapper ausente E permitido explicitamente |
| ROBOT | `robot` / `python -m robot` / `python3 -m robot` | só com diretório de testes conhecido (`knowledge.testDirectories()`) |
| PYTEST | `pytest` / `python -m pytest` / `python3 -m pytest` | sempre que framework de teste = PYTEST |

`AutomationFramework.UNKNOWN` ou `BuildTool.UNKNOWN` (sem comando seguro comprovado) → `ExecutionStatus.BLOCKED`, nenhum processo iniciado.

## 7. Política de allowlist

`CommandPolicyService` é o único ponto que valida um `CommandSpecification` já montado (defesa em profundidade sobre o que `CommandResolver` produziu):
- executável precisa estar na allowlist fixa acima (nunca aceita string arbitrária).
- cada argumento é validado contra uma denylist de metacaracteres de shell (`;`, `|`, `&&`, `||`, `` ` ``, `$(`, `>`, `<`, `\n`).
- comandos explicitamente proibidos (`npm install`, `npm ci`, `yarn install`, `pnpm install`, `gradle build`, `mvn install`, `docker`, `git`, `curl`, `wget`, `powershell`, `bash -c`, `sh -c`, `cmd /c`) nunca aparecem na allowlist — não é uma denylist dinâmica, é ausência estrutural: só o que está na allowlist positiva pode ser construído pelo `CommandResolver`.
- `ExecutionApproval.allowedCommands` atua como filtro adicional: mesmo um comando presente na allowlist estática é bloqueado se seu rótulo não estiver na lista aprovada para esta execução específica.

## 8. Política de ambiente

- Ambiente mínimo por padrão: apenas `PATH` (herdado do processo pai, necessário para localizar `npm`/`python`/etc. no `PATH`) + variáveis explicitamente necessárias.
- Reaproveito o padrão regex de redação já usado em `CodeReviewInputSanitizer` (`(?i)\b(password|senha|secret|apikey|api_key|token|private_key)\b`) para nunca repassar `OPENAI_API_KEY`, `GEMINI_API_KEY`, `JIRA_API_TOKEN` e afins ao processo filho nem a nenhum log — variáveis cujo nome bate no padrão são omitidas do `ProcessBuilder.environment()`, e um `ExecutionWarning` `ENVIRONMENT_REDACTED` (não bloqueante) é adicionado quando isso ocorre.
- `Map<String,String> environment` do `CommandSpecification` público nunca contém o valor real de nada sensível — só as chaves permitidas.

## 9. Política de timeout e encerramento

- Timeout default 10 minutos; limites obrigatórios 1s–30min, validados por `TestExecutionService`/`CommandResolver` antes de iniciar o processo.
- `ProcessTerminationService`: ao atingir timeout → `destroy()` → aguarda um período curto configurável → `destroyForcibly()` se ainda vivo → aguarda confirmação de término → registra `TIMED_OUT`. Nunca deixa processo órfão (verificação final `process.isAlive()==false` antes de retornar).
- Cancelamento explícito segue o mesmo caminho de encerramento, resultando em `CANCELLED`.

## 10. Política de stdout/stderr

- `ProcessOutputCollector` lê `stdout` e `stderr` em **duas threads separadas** (um `ExecutorService` de 2 threads, ou `Thread` dedicada por stream) para evitar deadlock por buffer cheio — nunca lê os dois sequencialmente na thread principal.
- Limite por stream e limite total (a definir em constantes, ex. 50.000 chars/stream) com truncamento explícito preservando as **últimas** linhas (mais úteis para diagnóstico de falha) e marcando `stdoutTruncated`/`stderrTruncated=true`.
- UTF-8 fixo; aplica a mesma regex de redação de segredos usada no ambiente, sobre o conteúdo capturado, antes de armazenar no `ExecutionResult`.
- Logs (INFO/DEBUG) nunca imprimem o conteúdo completo — só tamanho, truncamento e resumo.

## 11. Semântica dos status

| Status | Significado | `success()` do agente |
|---|---|---|
| `PASSED` | processo rodou, `exitCode=0` | `true` |
| `FAILED` | **processo executado corretamente**, testes falharam (`exitCode!=0`) | `true` — não é falha técnica do agente |
| `TIMED_OUT` | timeout excedido, processo encerrado à força | `false` — execução não concluiu corretamente |
| `BLOCKED` | política/aprovação impediu início; nenhum processo criado | `false` |
| `ERROR` | falha técnica ao iniciar/ler/encerrar o processo (infraestrutura) | `false` |
| `CANCELLED` | cancelamento explícito | `false` |

Esta distinção **PASSED/FAILED = execução válida do comando** vs. **ERROR = falha da infraestrutura de execução** é a pedra angular pedida para a Fase 10: o `FailureAgent` (fase futura, não criado agora) vai analisar `FAILED`, nunca `ERROR`.

## 12. Testes planejados

Distribuição alvo (~185–195 testes novos): modelos (~10), `CommandPolicyService` (~25), `CommandResolver` (~12), `ProcessOutputCollector` (~12), `ExecutionResultParser` (~15), `ProcessExecutionService` (~14), precondition/termination/summary (~20), `TestExecutionService` (~25), `ExecuteAgent` (~24), contexto (~10), workflow de integração (~18), utilitários/exceções (~5). Testes de processo real usarão executáveis triviais e não destrutivos (ex. comandos curtos reais fora da allowlist de produção, só para validar captura de exit code/stdout/stderr/timeout do `ProcessExecutionService` de forma isolada) — nunca `./gradlew test` real dentro dos próprios testes.

## 13. Riscos técnicos

- **Deadlock/vazamento de thread** na captura paralela de stdout/stderr se o processo travar: mitigado por timeout + `destroyForcibly()` + join com timeout nas threads coletoras.
- **Parsing de resultado é heurístico** (regex sobre stdout, por framework): risco de não bater com formatos não padronizados de saída — mitigado pela regra explícita "não inventar contagem", retornando `summary` vazio + `RESULT_PARSE_FAILED` não bloqueante em vez de dado errado.
- **Redação de segredos por regex** pode ter falsos negativos para nomes de variável não convencionais — mitigado por ambiente mínimo por padrão (menos exposição a redigir).
- **Testabilidade de `gradlew.bat`/Windows** sem rodar em Windows: a escolha do executável depende só de string (`System.getProperty("os.name")` injetável), então é testável via fake/injeção sem precisar de ambiente Windows real.
- **Diferença de comportamento entre macOS/Linux/CI** quanto a permissão de execução do wrapper (`./gradlew` precisa ser executável) — fora do controle desta fase; se falhar, cai em `ERROR`/`PROCESS_START_FAILED`, nunca trava a JVM.

## 14. Confirmação de que nenhum arquivo foi alterado

Confirmado. Até aqui só houve leitura (`AutoQaWorkflowService`, enums de discovery, `ProjectKnowledgeResult`, `NamingConvention`) para embasar este plano. Nenhum `Write`/`Edit` foi executado.

## 15. Confirmação de aguardo de aprovação

Confirmado. Nenhuma implementação será iniciada até sua aprovação explícita deste plano.
