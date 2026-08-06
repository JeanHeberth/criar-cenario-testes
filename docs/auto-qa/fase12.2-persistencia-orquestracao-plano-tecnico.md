# Fase 12.2 — Persistência, Orquestração e Endpoints: Plano Técnico (aguardando aprovação)

Esta fase implementa **somente backend**: persistência, snapshot operacional, reidratação, orquestração persistente, endpoints, segurança por configuração, idempotência, concorrência e retomada após reinício. Nenhum frontend, nenhum SSE/WebSocket, nenhuma alteração funcional nas Fases 1–11, nenhuma Fase 12.3.

## 0. Verificações técnicas feitas antes deste plano (só leitura)

- `ApplyApproval`/`ExecutionApproval`: records já existentes, com `approvedBy`, `approvedAt`, `approved`, e listas/sets de operações/comandos autorizados — reaproveitáveis diretamente como base dos DTOs de request de aprovação.
- `application.yml` bloco `auto-qa:` confirmado (linhas 50–61) — vou mapear exatamente essas chaves na `AutoQaProperties`, sem inventar nomes novos.
- `build.gradle`: Spring Boot 3.4.4, Java 21, `spring-boot-starter-data-mongodb` e `spring-boot-starter-validation` já presentes (então `@ConfigurationProperties` + `@Validated` funcionam sem dependência nova). **Nenhum Testcontainers/`flapdoodle` no projeto** — ver seção de testes.
- `spring.data.mongodb.uri: ${MONGO_URI_NUVEM}` — o Mongo configurado é uma instância remota/nuvem compartilhada (mesma usada por `cenario`/`chat_sessions`). Reforça a necessidade de um Mongo efêmero para os testes (nunca testar contra `MONGO_URI_NUVEM`).
- `Cenario`/`ChatSession` (únicos `@Document` do projeto): `@Data` (Lombok, mutável) + `@Id private String id`. **Nenhum `@Version` em uso hoje** — será a primeira introdução desse padrão.
- `ApiExceptionHandler`: um `@RestControllerAdvice` com `ResponseStatusException` mapeada + fallback genérico sempre 500. `ErrorResponse` é um record simples (`timestamp, status, error, message, path`).
- `getName()` confirmado nos 10 agentes: `project-discovery`, `scenario-analysis`, `project-knowledge`, `planning`, `generation`, `review`, `apply`, `execute`, `failure-analysis`, `learning`.
- Diretório `.auto-qa/generated/{executionId}/files/...` já existe fisicamente e já tem um leitor (`GeneratedArtifactReader`) — reaproveitável na reidratação para conteúdo de arquivo gerado (ver seção 7).

---

## 1. Diagnóstico do backend atual

Igual ao já aprovado na Fase 12.1 (não repito aqui por completo — ver `docs/auto-qa/fase12.1-diagnostico-e-contratos.md`), com os detalhes técnicos adicionais da seção 0 acima.

---

## 2. Arquivos que serão criados

```
src/main/java/com/br/criarcenariotestes/business/autoqa/executionapi/
├── model/
│   ├── AutoQaWorkflowStatus.java
│   ├── AutoQaStage.java
│   ├── AutoQaOperationStatus.java
│   └── AutoQaAvailableAction.java
├── persistence/
│   ├── AutoQaExecutionDocument.java
│   ├── AutoQaStageRecord.java              (sub-documento embutido)
│   ├── AutoQaApprovalRecord.java           (sub-documento embutido)
│   ├── AutoQaWarningRecord.java            (sub-documento embutido)
│   ├── AutoQaErrorRecord.java              (sub-documento embutido)
│   ├── AutoQaExecutionRepository.java
│   ├── AutoQaExecutionSnapshot.java
│   ├── AutoQaExecutionSnapshotRepository.java
│   └── snapshot/                            (sub-documentos "Sanitized*Snapshot", um por fase — ver seção 6)
│       ├── SanitizedProjectDiscoverySnapshot.java
│       ├── SanitizedScenarioAnalysisSnapshot.java
│       ├── SanitizedProjectKnowledgeSnapshot.java
│       ├── SanitizedTechnicalPlanSnapshot.java
│       ├── SanitizedGenerationSnapshot.java
│       ├── SanitizedGeneratedFileSnapshot.java
│       ├── SanitizedCodeReviewSnapshot.java
│       ├── SanitizedApplyApprovalSnapshot.java
│       ├── SanitizedApplySnapshot.java
│       ├── SanitizedExecutionApprovalSnapshot.java
│       ├── SanitizedExecutionSnapshot.java
│       ├── SanitizedFailureAnalysisSnapshot.java
│       └── SanitizedLearningSnapshot.java
├── mapper/
│   ├── AutoQaContextSnapshotMapper.java
│   └── AutoQaExecutionResponseMapper.java
├── orchestrator/
│   ├── AutoQaExecutionOrchestrator.java
│   ├── AutoQaAgentRegistry.java              (mapeia AutoQaStage → AutoQaAgent concreto)
│   ├── AutoQaAvailableActionResolver.java
│   └── AutoQaTransitionValidator.java
├── service/
│   ├── AutoQaExecutionQueryService.java
│   └── AutoQaExecutionCommandService.java
├── dto/
│   ├── AutoQaCreateExecutionRequest.java
│   ├── AutoQaExecutionResponse.java
│   ├── AutoQaExecutionListResponse.java
│   ├── AutoQaStageResponse.java
│   ├── AutoQaApplyApprovalRequest.java
│   ├── AutoQaExecutionApprovalRequest.java
│   ├── AutoQaCancelRequest.java
│   ├── AutoQaPublicWarning.java
│   └── AutoQaPublicError.java
├── config/
│   └── AutoQaProperties.java
└── exception/
    ├── AutoQaExecutionNotFoundException.java
    ├── AutoQaInvalidTransitionException.java
    ├── AutoQaExecutionConflictException.java
    ├── AutoQaExecutionDisabledException.java
    ├── AutoQaSensitiveActionDisabledException.java
    ├── AutoQaSnapshotException.java
    └── AutoQaOptimisticLockException.java

src/main/java/com/br/criarcenariotestes/controller/
├── AutoQaExecutionController.java
└── AutoQaExecutionExceptionHandler.java      (handler dedicado, ver seção 15 — NÃO altera o ApiExceptionHandler genérico)
```

Testes espelham 1:1 essa árvore em `src/test/java/.../business/autoqa/executionapi/**` e `src/test/java/.../controller/AutoQaExecutionControllerTest.java`.

**Nota de nomenclatura:** o pacote fica dentro de `business/autoqa/` (não em `controller`/`infrastructure` genéricos) para manter o módulo Auto QA coeso num único lugar — desvio pontual do padrão `controller/`+`infrastructure/repository/` do resto do projeto, justificado por ser um módulo autocontido com 11 fases já organizadas assim. Sinalizando explicitamente essa escolha para aprovação.

---

## 3. Arquivos que serão alterados

**Nenhum arquivo das Fases 1–11 é alterado.** Único arquivo de produção fora do novo pacote a ser tocado:

- `src/main/resources/application.yml` — apenas a seção `auto-qa:` existente recebe as chaves novas necessárias (`sensitive-actions-enabled`, `retention-days`, `max-concurrent-executions`), com **valores seguros por padrão** (ver seção 13). Nenhuma chave existente muda de significado.

Nada além disso.

---

## 4. Estrutura de `AutoQaExecutionDocument`

```java
@Document(collection = "autoqa_executions")
@Data                                    // segue o padrão Cenario/ChatSession (mutável, Lombok)
public class AutoQaExecutionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID executionId;

    private String scenarioSummary;      // truncado a um tamanho máximo (ex. 500 chars)
    private String projectReference;     // NUNCA path absoluto — alias/nome informado

    private AutoQaWorkflowStatus workflowStatus;
    private AutoQaStage currentStage;
    private AutoQaOperationStatus operationStatus;
    private int progress;                // 0-100, calculado a partir de currentStage

    private List<AutoQaStageRecord> stages;
    private List<AutoQaApprovalRecord> approvals;
    private List<AutoQaWarningRecord> warnings;
    private List<AutoQaErrorRecord> errors;
    private Set<AutoQaAvailableAction> availableActions;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    @Version
    private Long version;
}
```

`AutoQaStageRecord(AutoQaStage stage, String status, Instant startedAt, Instant finishedAt, String summary)`, `AutoQaApprovalRecord(String type, String approvedBy, Instant approvedAt, boolean approved)`, `AutoQaWarningRecord(String code, String description, boolean blocking)`, `AutoQaErrorRecord(String code, String message)` — todos records imutáveis simples (não `@Document` próprios, embutidos).

**Decisão de estilo a confirmar:** `Document` mutável com Lombok `@Data` (seguindo `Cenario`/`ChatSession`) em vez de record — Spring Data Mongo suporta ambos, mas o padrão comprovado do projeto é mutável. DTOs de API e sub-documentos continuam records (estilo já usado em todo o módulo Auto QA).

---

## 5. Estrutura de `AutoQaExecutionSnapshot`

```java
@Document(collection = "autoqa_execution_snapshots")
@Data
public class AutoQaExecutionSnapshot {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID executionId;

    private AutoQaStage lastCompletedStage;
    private AutoQaWorkflowStatus workflowStatus;

    private SanitizedProjectDiscoverySnapshot discovery;
    private SanitizedScenarioAnalysisSnapshot scenarioAnalysis;
    private SanitizedProjectKnowledgeSnapshot projectKnowledge;
    private SanitizedTechnicalPlanSnapshot technicalPlan;
    private SanitizedGenerationSnapshot generation;
    private SanitizedCodeReviewSnapshot codeReview;
    private SanitizedApplyApprovalSnapshot applyApproval;
    private SanitizedApplySnapshot apply;
    private SanitizedExecutionApprovalSnapshot executionApproval;
    // execução/failure-analysis/learning NÃO precisam sobreviver a uma reidratação
    // (ver justificativa na seção 7 — rodam sempre em um único bloco atômico)

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
```

**Cada `SanitizedXxxSnapshot` espelha só os campos necessários para reconstruir o record real correspondente via seu construtor público** (nunca reflection). Exemplo (`SanitizedProjectDiscoverySnapshot`):

```java
public record SanitizedProjectDiscoverySnapshot(
        AutomationFramework automationFramework,
        AutomationLanguage language,
        PackageManager packageManager,
        BuildTool buildTool,
        Set<TestingFramework> testingFrameworks,
        Set<AutomationFramework> detectedFrameworks,
        List<String> libraries,
        String configurationFile,
        List<String> evidenceFiles,
        List<String> warnings,
        DiscoveryConfidence confidence,
        boolean valid
) {}
```

Note: **omite `normalizedProjectPath`** (o único campo sensível do record original) — na reidratação, o `projectPath` vem do próprio `AutoQaExecutionDocument`/request, nunca do snapshot.

Para `SanitizedGenerationSnapshot`/`SanitizedGeneratedFileSnapshot`: armazenam **metadados** de cada `GeneratedFile` (`relativePath`, `operation`, `componentType`, `sha256`, `status`, `reusedComponents`, `dependencies`) — **nunca `content`**. Na reidratação, o conteúdo é relido do disco (`.auto-qa/generated/{executionId}/files/{relativePath}`, via o `GeneratedArtifactReader` já existente) — reaproveitando a mesma infraestrutura que a Fase 6/7 já usa, sem inventar I/O novo.

Para `SanitizedExecutionSnapshot` (se algum dia precisar existir): **não armazena `stdout`/`stderr`** — mas como Execute nunca é interrompido para aprovação (roda no mesmo bloco atômico que FailureAnalysis+Learning, ver seção 7), esse snapshot **não é necessário nesta fase** e por isso nem consta no documento acima.

---

## 6. Regras do snapshot (tamanho e conteúdo)

- Nunca: conteúdo de arquivo, stdout/stderr, `projectPath` absoluto, prompts, respostas brutas de IA, segredos, `environment`, backups integrais — reforçando o que já é garantido pelos records de fase (que já não carregam a maioria disso) mais a omissão explícita de `content`/paths absolutos nos `Sanitized*Snapshot`.
- Limites explícitos (reaproveitando os já existentes nas Fases 1–11, sem reinventar): findings/issues/warnings já vêm limitados a 10–50 itens pelos extractors/collectors de cada fase; o snapshot só copia o que já veio limitado, não trunca de novo.
- Guard defensivo: logar warning se o documento serializado (`AutoQaExecutionSnapshot` ou `AutoQaExecutionDocument`) passar de um limiar configurável (ex. 500KB) — não bloqueante nesta fase, só observabilidade.

---

## 7. Estratégia de reidratação

`AutoQaContextSnapshotMapper`:

```java
public AutoQaExecutionSnapshot toSnapshot(AutoQaContext context, AutoQaStage lastCompletedStage) { ... }
public AutoQaContext toContext(AutoQaExecutionSnapshot snapshot, String scenario, String projectPath) { ... }
```

**Ordem de reconstrução (idêntica à cadeia de `registerXxx` do `AutoQaContext`):**
`ProjectDiscovery → ScenarioAnalysis → ProjectKnowledge → TechnicalPlan → Generation → CodeReview → ApplyApproval → Apply → ExecutionApproval`.

**Decisão de design chave (motivada por análise dos dados de cada fase, não assumida):** o snapshot só precisa preservar até `ApplyApproval`/`Apply` porque as fases seguintes (Execute, FailureAnalysis, Learning) **sempre rodam juntas, em um único bloco atômico**, dentro de uma mesma chamada de `AutoQaWorkflowService` (ver seção 8/9) — nunca há uma pausa de aprovação *entre* Execute e FailureAnalysis, nem entre FailureAnalysis e Learning. Logo, essas três fases nunca precisam ser "reidratadas" isoladamente: ou o bloco inteiro completou em memória e o resultado final já foi persistido no `AutoQaExecutionDocument` (fim de linha, `workflowStatus=COMPLETED`/`FAILED`), ou o processo caiu no meio dele — caso em que **não há estado parcial seguro para retomar** (ver limitação explícita na seção 14) e a única ação disponível é `RETRY` do bloco inteiro a partir do último snapshot válido (que é o de após `Apply`).

**Regras do mapper:**
- Usa exclusivamente os métodos públicos `registerXxx()` do `AutoQaContext` — a própria cadeia de pré-condições dos `registerXxx` já é a validação de consistência (se o snapshot estiver incompleto/fora de ordem, o `registerXxx` correspondente lança `IllegalStateException`/`NullPointerException`, capturada e relançada como `AutoQaSnapshotException`).
- Sem reflection, sem acesso a campo privado, sem setter genérico.
- Não modifica o `AutoQaContext`/snapshot originais (produz um novo `AutoQaContext` a cada chamada).
- Determinística: mesmo snapshot → mesmo `AutoQaContext` reconstruído (mesmos valores, exceto os campos deliberadamente omitidos como `content`).

---

## 8. Estratégia de orquestração

`AutoQaExecutionOrchestrator` — ponto único de decisão, **não substitui nem refatora `AutoQaWorkflowService`**, só o invoca com sublistas:

```java
public AutoQaExecutionDocument create(String scenario, String projectReference);
public AutoQaExecutionDocument start(UUID executionId);
public AutoQaExecutionDocument continueExecution(UUID executionId);
public AutoQaExecutionDocument generate(UUID executionId);
public AutoQaExecutionDocument registerApplyApproval(UUID executionId, ApplyApproval approval);
public AutoQaExecutionDocument apply(UUID executionId);
public AutoQaExecutionDocument registerExecutionApproval(UUID executionId, ExecutionApproval approval);
public AutoQaExecutionDocument execute(UUID executionId);
public AutoQaExecutionDocument cancel(UUID executionId, String reason);
```

Fluxo interno comum a `start`/`generate`/`apply`/`execute`/`continueExecution`:
1. Carrega `AutoQaExecutionDocument` (404 se ausente).
2. `AutoQaTransitionValidator` valida se a transição pedida é permitida no `workflowStatus` atual (409 se não).
3. Verifica `operationStatus != IN_PROGRESS` (409 se já em andamento — lock).
4. Marca `operationStatus=IN_PROGRESS`, persiste imediatamente (grava o lock **antes** de rodar qualquer agente).
5. Carrega `AutoQaExecutionSnapshot`, reidrata `AutoQaContext` via `AutoQaContextSnapshotMapper`.
6. Monta a sublista de agentes do bloco correspondente via `AutoQaAgentRegistry` (ver seção 9).
7. Chama `new AutoQaWorkflowService(sublista).execute(context)` — **zero alteração no `AutoQaWorkflowService`**.
8. Inspeciona o `AutoQaContext` resultante, deriva `workflowStatus`/`currentStage` novos (`AutoQaTransitionValidator`/lógica de transição, seção 9).
9. Gera novo snapshot (se o bloco chegou a um ponto de pausa válido) via `AutoQaContextSnapshotMapper.toSnapshot(...)`.
10. Persiste documento + snapshot atualizados, `operationStatus=SUCCEEDED` ou `FAILED`, dentro do mesmo ciclo de escrita (usando `@Version` — `OptimisticLockingFailureException` do Spring Data vira `AutoQaOptimisticLockException` → 409).

`AutoQaAgentRegistry`: `Map<AutoQaStage, AutoQaAgent>` construído a partir do `List<AutoQaAgent>` já injetado pelo Spring (casando por `getName()`), usado só para montar sublistas — **não altera o contrato de `AutoQaAgent`**.

`AutoQaAvailableActionResolver`: função pura `(AutoQaWorkflowStatus, List<AutoQaApprovalRecord>, AutoQaProperties) -> Set<AutoQaAvailableAction>` — nenhuma dependência de I/O, 100% testável por matriz de estado×ação (seção 21).

O Orchestrator **não**: substitui `AutoQaWorkflowService`, duplica lógica interna dos agentes, acessa frontend, persiste prompt, expõe domínio diretamente, executa ação sensível sem checar `AutoQaProperties`.

---

## 9. Estratégia de execução por etapas (blocos)

| Bloco | Endpoint que dispara | Agentes (Order) | `workflowStatus` de origem | `workflowStatus` de destino (sucesso) |
|---|---|---|---|---|
| 1 | `/start` | DISCOVERY(0), SCENARIO_ANALYSIS(10), PROJECT_KNOWLEDGE(20), PLANNING(30) | `CREATED` | `WAITING_GENERATION_APPROVAL` |
| 2 | `/generate` | GENERATION(40), REVIEW(50) | `WAITING_GENERATION_APPROVAL` | `WAITING_APPLY_APPROVAL` |
| 3 | `/apply` | APPLY(60) | `WAITING_APPLY_APPROVAL` **com** `ApplyApproval` já registrada | `WAITING_EXECUTION_APPROVAL` |
| 4 | `/execute` | EXECUTION(70), FAILURE_ANALYSIS(80), LEARNING(90) | `WAITING_EXECUTION_APPROVAL` **com** `ExecutionApproval` já registrada | `COMPLETED` |

Qualquer bloco que termine com `AutoQaWorkflowService` retornando `context.getStatus()==AutoQaStatus.ERROR` (halt técnico do próprio motor) → `workflowStatus=FAILED`, com o `AutoQaErrorRecord` guardando a mensagem já sanitizada que o `AutoQaWorkflowService` produz (`agentName + ": " + result.message()`).

**Ponto crítico já validado pela Fase 1–11 e reaproveitado aqui sem mudança:** dentro do Bloco 4, `ExecuteAgent` retorna `success()==true` tanto para `ExecutionStatus.PASSED` quanto `FAILED` (testes rodaram e falharam — não é erro técnico), então o bloco **continua normalmente** até `FailureAnalysisAgent`/`LearningAgent` e o Orchestrator marca `workflowStatus=COMPLETED` **mesmo que os testes tenham falhado** — `COMPLETED` significa "o workflow terminou de rodar todas as etapas aplicáveis", não "os testes passaram". O resultado real dos testes fica no resumo/`AutoQaStageRecord` de `EXECUTION`, inspecionável separadamente. Só um `ExecutionStatus` operacional (`ERROR`/`BLOCKED`/`TIMED_OUT`/`CANCELLED`) faz `ExecuteAgent` retornar `failure()`, interrompendo o Bloco 4 antes de `FailureAnalysisAgent` — nesse caso `workflowStatus=FAILED`, com `RETRY` disponível.

`/continue` reexecuta o **mesmo bloco que falhou tecnicamente por último** (só válido quando `workflowStatus==FAILED` e as aprovações daquele bloco já estavam satisfeitas) — não avança além disso nem substitui `/generate`/`/apply`/`/execute` para os pontos que exigem aprovação nova.

**Nenhum `AgentExecutionResult.failure()` é usado para representar pausa aprovada** — a pausa é inteiramente controlada pelo Orchestrator, que simplesmente não invoca o próximo bloco até a ação HTTP correspondente ser chamada.

---

## 10. Estados e transições

`AutoQaWorkflowStatus`: `CREATED, RUNNING, WAITING_GENERATION_APPROVAL, WAITING_APPLY_APPROVAL, WAITING_EXECUTION_APPROVAL, COMPLETED, FAILED, CANCELLED` (exatamente como sugerido).

`AutoQaStage`: `DISCOVERY, SCENARIO_ANALYSIS, PROJECT_KNOWLEDGE, PLANNING, GENERATION, REVIEW, APPLY, EXECUTION, FAILURE_ANALYSIS, LEARNING`.

`AutoQaOperationStatus`: `IDLE` (nenhuma operação rodou ainda), `IN_PROGRESS` (lock ativo — bloqueia novas ações), `SUCCEEDED`/`FAILED` (resultado da última operação, informativo até a próxima ação começar). É o campo que sustenta o lock de concorrência (seção 16) — **não se mistura com `currentStage`/`workflowStatus`**, que representam o FSM geral.

Diagrama de transição (texto): `CREATED -[start]-> RUNNING -[bloco1 ok]-> WAITING_GENERATION_APPROVAL -[generate]-> RUNNING -[bloco2 ok]-> WAITING_APPLY_APPROVAL -[apply-approval]-> (mesmo estado, approval registrada) -[apply]-> RUNNING -[bloco3 ok]-> WAITING_EXECUTION_APPROVAL -[execution-approval]-> (mesmo estado, approval registrada) -[execute]-> RUNNING -[bloco4 ok]-> COMPLETED`. Qualquer bloco pode terminar em `FAILED`. Qualquer estado não-terminal aceita `[cancel]-> CANCELLED`. `FAILED` aceita `[continue]-> RUNNING` (retry do mesmo bloco).

---

## 11. Endpoints e contratos (só os 11 desta fase)

| Método | Path | Sucesso | Precondição | Erros | Sensível? |
|---|---|---|---|---|---|
| `POST` | `/api/auto-qa/executions` | `201` | `scenario` não vazio, `projectReference` válido | `400`, `422` | Não |
| `GET` | `/api/auto-qa/executions` | `200` (paginado) | — | `400` (filtro inválido) | Não |
| `GET` | `/api/auto-qa/executions/{id}` | `200` | existe | `404` | Não |
| `POST` | `/{id}/start` | `202` | `status==CREATED`, lock livre | `404`, `409` | Não |
| `POST` | `/{id}/continue` | `202` | `status==FAILED`, aprovações do bloco satisfeitas, lock livre | `404`, `409` | Depende do bloco |
| `POST` | `/{id}/generate` | `202` | `status==WAITING_GENERATION_APPROVAL`, lock livre | `404`, `409` | Não (staging apenas) |
| `POST` | `/{id}/apply-approval` | `200`/`201` | `status==WAITING_APPLY_APPROVAL`, aprovação ainda não registrada | `404`, `409` | Não (só registra) |
| `POST` | `/{id}/apply` | `202` | approval registrada, `allow-file-application=true`, `sensitive-actions-enabled=true` | `403` (flag off), `404`, `409` | **Sim** |
| `POST` | `/{id}/execution-approval` | `200`/`201` | `status==WAITING_EXECUTION_APPROVAL`, ainda não registrada | `404`, `409` | Não (só registra) |
| `POST` | `/{id}/execute` | `202` | approval registrada, `allow-command-execution=true`, `sensitive-actions-enabled=true` | `403` (flag off), `404`, `409` | **Sim** |
| `POST` | `/{id}/cancel` | `200` | `status` não-terminal | `404`, `409` (já terminal) | Não |

Não implementados nesta fase (confirmado): SSE, WebSocket, preview de arquivo, download, diff, logs completos, delete físico, histórico visual, endpoints de learning approval.

---

## 12. DTO público

```java
public record AutoQaExecutionResponse(
        UUID executionId,
        AutoQaWorkflowStatus status,
        AutoQaStage currentStage,
        int progress,
        List<AutoQaStageResponse> stages,
        Set<AutoQaAvailableAction> availableActions,
        List<String> pendingApprovals,
        AutoQaExecutionSummaries summaries,     // tipado, NÃO Map<String,Object>
        List<AutoQaPublicWarning> warnings,
        List<AutoQaPublicError> errors,
        Instant createdAt, updatedAt, startedAt, finishedAt
) {}
```

`AutoQaExecutionSummaries` — record tipado com um sub-record leve por fase (`DiscoverySummaryView`, `PlanningSummaryView` etc., cada um só com status/confidence/contagens/top-N mensagens), **não `Map<String,Object>`** — quebraria o padrão de tipagem forte que o módulo Auto QA já mantém em todas as 11 fases.

Nunca expõe: `AutoQaContext`, documentos Mongo, records internos das fases diretamente, `projectPath` absoluto, prompt, resposta bruta, stacktrace, `environment`, segredo, conteúdo completo de arquivo, stdout/stderr completos. Sempre via `AutoQaExecutionResponseMapper` dedicado (campo a campo, nunca reflection/serialização genérica do documento Mongo).

---

## 13. Configuração tipada

```java
@ConfigurationProperties(prefix = "auto-qa")
@Validated
public record AutoQaProperties(
        boolean enabled,
        List<String> allowedRoots,           // String, não Path — binding mais simples/direto
        @Positive int maxFiles,
        @Positive int maxFileSizeKb,
        @Positive int maxTotalContentKb,
        @Positive int maxGenerationRetries,
        Duration maxExecutionDuration,        // reaproveita max-execution-minutes convertido
        String generatedDirectory,
        String backupDirectory,
        boolean allowCommandExecution,
        boolean allowFileApplication,
        boolean sensitiveActionsEnabled,
        @PositiveOrZero int retentionDays,
        @Positive int maxConcurrentExecutions
) {}
```

`application.yml` (chaves **já existentes** reaproveitadas + as **3 novas**, valores seguros por padrão):

```yaml
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
  allow-command-execution: false      # era true — corrigido para seguro por padrão
  allow-file-application: false       # era true — corrigido para seguro por padrão
  sensitive-actions-enabled: false    # nova
  retention-days: 30                  # nova
  max-concurrent-executions: 5        # nova
```

**Nota importante:** os valores atuais de `allow-command-execution`/`allow-file-application` no yml são `true` — como esta fase os CONECTA pela primeira vez a uma classe real, mudar o default para `false` é uma correção de segurança explícita pedida ("valores seguros por padrão"), não uma mudança funcional das Fases 1–11 (esses valores nunca foram lidos por nada até agora). Sinalizando para aprovação explícita antes de aplicar.

---

## 14. Segurança das ações sensíveis

`apply` e `execute` checam, nesta ordem, antes de qualquer outra coisa: (1) `AutoQaProperties.sensitiveActionsEnabled` — se `false`, `403 AutoQaSensitiveActionDisabledException`, sem tocar no Orchestrator; (2) a flag específica (`allowFileApplication`/`allowCommandExecution`) — mesmo tratamento; (3) `allowedRoots` validado em `POST /executions` (na criação, não em cada ação) — se `projectReference` resolvido não bater com a allowlist, `422`.

Sem autenticação completa nesta fase (confirmado, fora de escopo) — mas toda ação de aprovação (`apply-approval`/`execution-approval`) exige `approvedBy` não vazio no request (já garantido pelo próprio record `ApplyApproval`/`ExecutionApproval`), servindo de auditoria mínima mesmo sem identidade verificada.

**Limitação explícita:** se o processo cair exatamente durante o Bloco 4 (Execute+FailureAnalysis+Learning), não há como retomar de forma fina — só `RETRY` do bloco inteiro a partir do snapshot pós-Apply (ver seção 7). Um reconciliador de startup (`ApplicationRunner`, execução única, não scheduler recorrente) varre documentos com `operationStatus==IN_PROGRESS` e os marca `FAILED`/`operationStatus=FAILED` com mensagem controlada ("operação interrompida por reinício da aplicação"), tornando-os elegíveis para `RETRY` via `/continue`.

---

## 15. Exception handling

Handler **dedicado** (`AutoQaExecutionExceptionHandler`, próprio `@RestControllerAdvice` escopado só ao pacote/controller novo — não altera `ApiExceptionHandler` genérico):

| Exception | HTTP |
|---|---|
| Bean Validation (`MethodArgumentNotValidException`) | 400 |
| `AutoQaSensitiveActionDisabledException` | 403 |
| `AutoQaExecutionNotFoundException` | 404 |
| `AutoQaInvalidTransitionException`, `AutoQaExecutionConflictException`, `AutoQaOptimisticLockException` (incl. `OptimisticLockingFailureException` do Spring Data) | 409 |
| `AutoQaSnapshotException` (semanticamente impossível) | 422 |
| qualquer outra | 500 (cai no `ApiExceptionHandler` genérico já existente, sem mudança) |

Nunca retorna stacktrace/mensagem interna bruta — todas as exceptions novas carregam mensagem já controlada/sanitizada no próprio construtor.

---

## 16. Concorrência e idempotência

- `@Version` em `AutoQaExecutionDocument` e `AutoQaExecutionSnapshot` — toda escrita passa pelo optimistic locking nativo do Spring Data Mongo.
- `operationStatus==IN_PROGRESS` como lock local — setado e persistido **antes** de rodar qualquer agente; qualquer nova ação enquanto `IN_PROGRESS` → `409` imediato (dois `apply`, dois `execute`, `execute` durante `apply` em andamento, etc.).
- Aprovação duplicada: barrada estruturalmente por `AutoQaTransitionValidator` (só aceita `apply-approval` quando ainda não registrada) — mapeada para `409`.
- Ação após cancelamento/conclusão: `AutoQaTransitionValidator` rejeita qualquer ação em estado terminal (`COMPLETED`/`CANCELLED`) → `409`.
- `Idempotency-Key` opcional em `POST /executions` (evita duas execuções duplicadas em duplo clique antes de existir `executionId`) — se ausente, comportamento atual (sempre cria).
- Sem locks distribuídos (Redis etc.) — fora de escopo, explícito.
- Retry de optimistic locking: **nenhum retry automático nesta fase** — um conflito de versão vira `409` direto para o cliente decidir recarregar e tentar de novo (retry automático limitado fica como evolução futura, não implementado agora, para não mascarar concorrência real).

---

## 17. Estratégia de polling

`GET /executions/{executionId}` já contém tudo que o polling do frontend (fase futura) vai precisar: `status`, `currentStage`, `progress`, `updatedAt`, `availableActions`, `summaries`. Nenhuma infraestrutura adicional nesta fase — SSE/WebSocket explicitamente não implementados.

**Assincronismo desta fase:** `start`/`generate`/`apply`/`execute` retornam `202` e disparam a execução em uma thread separada (`@Async` do Spring, `ThreadPoolTaskExecutor` dedicado com tamanho limitado — `maxConcurrentExecutions` da `AutoQaProperties` governa o pool). Estado é persistido **antes** de iniciar (lock) e **depois** de terminar (resultado) — nunca perde uma execução silenciosamente; se a thread lançar exceção não tratada, um `try/catch` no runnable assíncrono garante que o documento seja marcado `FAILED`/`operationStatus=FAILED` mesmo assim. Testável via `@Async` síncrono em teste (Spring permite configurar executor síncrono só para testes) — sem infraestrutura de fila (Kafka/RabbitMQ), confirmado.

---

## 18. Testes planejados

Ver seção 21 do pedido original — plano de testes aceito integralmente, com uma adição: como **não há Testcontainers/`flapdoodle` no projeto hoje**, proponho adicionar `de.flapdoodle.embed.mongo.spring3x` como dependência **de teste apenas** (`testImplementation`) — Mongo embarcado, sem exigir Docker no ambiente, evita testar contra o `MONGO_URI_NUVEM` real (que é compartilhado com `cenario`/`chat_sessions` de produção). Isso é uma dependência de teste, não uma infraestrutura de runtime — mas sinalizo explicitamente para aprovação, já que é uma dependência nova. Alternativa, se preferir zero dependência nova: testar repository/persistência só com mocks (mais fraco, não valida `@Version`/queries reais do Spring Data) — deixo a decisão para você.

Estimativa: ~35–45 arquivos de teste novos, seguindo a densidade das Fases 1–11 (framework de teste igual: JUnit5+Mockito+AssertJ, nomes `deveXxx` em português).

---

## 19. Riscos técnicos

- Reidratação (`AutoQaContextSnapshotMapper`) é a peça de maior risco técnico — mitigada por reaproveitar a própria cadeia de validação do `AutoQaContext` (não reinventa validação).
- Assincronismo local (`@Async`) sem fila dedicada — aceitável para o volume esperado (uso local/interno), mas é um limite explícito de escala (não serve para produção com muitos usuários simultâneos sem evolução futura).
- Reconciliação de `operationStatus=IN_PROGRESS` travado após crash exige o `ApplicationRunner` de startup — se esquecido/com bug, uma execução pode ficar "presa" indefinidamente (mitigado por teste dedicado).
- Mudar o default de `allow-command-execution`/`allow-file-application` para `false` é tecnicamente uma mudança de comportamento *potencial* (hoje esses valores nunca foram lidos, então não hpá comportamento real mudando) — mas sinalizando para não passar despercebido.
- Dependência de teste nova (`flapdoodle`) — decisão a confirmar.

---

## 20. Dívidas técnicas não corrigidas nesta fase

- `FailureAnalysisService` (`aiFindings = List.of()`) — herdada, não tocada.
- Autenticação/autorização real de usuário — segue não implementada (fora de escopo, conforme pedido).
- CORS `*` — não revisado nesta fase (fora do pedido explícito de escopo).
- Retry automático de optimistic locking — não implementado, decisão consciente.
- Limpeza/TTL automática de execuções antigas — só mapeada (`retention-days`), TTL index físico fica como dívida documentada (o pedido permite isso explicitamente).
- Frontend antigo órfão — inalterado, fora de escopo desta fase (backend apenas).

---

## 21. Lista de arquivos já modificados antes desta implementação (`git status --short`)

Backend (herdados da Fase 11, ainda não commitados — ver relatório da Fase 11 para detalhe completo): `FailureAnalysisAgent.java`, `AutoQaContext.java`, `AutoQaContextTest.java` modificados; diretórios `learning/`, `model/learning/` e respectivos testes untracked; `docs/auto-qa/fase11-*.md`, `fase12.1-*.md` untracked. **Mais**: `FailureAnalysisService.java` e `LearningService.java` aparecem modificados (adição de `@Service` — não fui eu, preservado como está).

Frontend: 6 arquivos com mudanças locais não commitadas na feature `autoqa-artifacts` (preexistentes, não tocadas por mim).

Nenhum arquivo novo foi adicionado por esta resposta — só este documento de plano.

---

## 22. Confirmação de que nenhum arquivo foi alterado nesta resposta

Confirmado — esta etapa foi só leitura (arquivos citados na seção 0) mais a escrita deste documento de plano em `docs/auto-qa/`. Nenhum arquivo de produção, teste ou configuração foi criado ou alterado.

## 23. Confirmação de que aguardará aprovação explícita

Nenhuma implementação será iniciada até você aprovar este plano (incluindo as decisões sinalizadas explicitamente: estrutura de pacote dentro de `business/autoqa/`, `@Data` mutável para os `@Document`, mudança de default `false` para as flags sensíveis, e a dependência de teste `flapdoodle`).
