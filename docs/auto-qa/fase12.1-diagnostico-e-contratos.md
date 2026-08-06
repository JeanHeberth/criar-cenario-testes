# Fase 12.1 — Diagnóstico, Contratos e Planejamento Arquitetural (aguardando aprovação)

Esta é uma etapa **somente de diagnóstico e planejamento**. Nenhum arquivo de backend ou frontend foi alterado. Nenhum endpoint foi criado. Nenhum componente Angular foi criado. Nada foi persistido no MongoDB. Nenhuma das Fases 1–11 foi tocada.

---

## 1. Diagnóstico do backend atual

Repositório: `/Users/jeanheberth/Development/api/criar-cenario-testes`.

- **Zero controllers Auto QA hoje.** Os únicos controllers do projeto são `AgentController` (`/api/agents`), `CenarioController` (`/cenario`) e `JiraController` (`/jira/tasks`), nenhum relacionado ao módulo `business/autoqa`. Confirmado por busca exaustiva.
- **`AutoQaWorkflowService`** (`business/autoqa/workflow/AutoQaWorkflowService.java`): `@Service` simples, injeta `List<AutoQaAgent>` (ordem via `@Order`), método único `execute(AutoQaContext)` **síncrono**, roda a lista inteira em loop e para no primeiro `AgentExecutionResult.failure()`/exceção/`null`. Não é chamado por nenhum controller ou service de aplicação hoje — só por testes. Não existe pausa para aprovação, retomada, nem noção de "etapa opcional".
- **`AutoQaContext`**: 12 métodos `registerXxx` (Discovery→Learning) com validação de pré-condição em cadeia. Sem anotação Jackson, mas serializável via introspecção de bean padrão (getters públicos) — **não desserializável** (construtor privado, só `create(scenario, projectPath)`). **Não existe nenhum Map/cache/repository que guarde instâncias por `executionId`** — cada contexto vive só na variável local de quem chamou, descartado ao final do método.
- **MongoDB**: `spring.data.mongodb` configurado via auto-configuração padrão (sem `MongoTemplate` custom). As únicas 2 coleções do projeto inteiro são `cenario` (`Cenario` + `CenarioRepository`) e `chat_sessions` (`ChatSession` + `ChatSessionRepository`) — ambas fora do Auto QA. Padrão: `@Id private String id`, Lombok, **sem `@Version` (nenhum optimistic locking em todo o projeto), sem TTL, sem `@Indexed`**. Persistência Auto QA hoje: **nenhuma**.
- **DTOs de API para Auto QA**: nenhum. A única pasta de DTOs (`business/dto/`) não referencia nada de `business.autoqa.*`.
- **CORS**: `CorsConfig` global e permissivo — `/**`, `allowedOriginPatterns("*")`, métodos GET/POST/PUT/DELETE/OPTIONS. Já cobriria endpoints novos de Auto QA automaticamente.
- **Exception handling**: um único `@RestControllerAdvice` (`ApiExceptionHandler`) — trata `ResponseStatusException` explicitamente e tudo o mais cai num handler genérico que **sempre retorna 500**. Todas as exceptions do Auto QA (`FailureAnalysisException`, `LearningException`, `ApplyException`, etc.) estendem `RuntimeException` diretamente, sem handler dedicado — **hoje toda falha de negócio do Auto QA vira HTTP 500**, mesmo as `*ValidationException` que deveriam ser 400/422.
- **Segurança**: **não existe Spring Security no projeto** (sem a dependência, sem `SecurityConfig`). Todos os endpoints são públicos. O único conceito de "sessão" é `ChatSession` (sessão de chat por `sessionId` livre), não é identidade de usuário — não há alicerce para "aprovação por usuário" hoje.
- **Diretórios `generated`/`backups`**: default `.auto-qa/generated` e `.auto-qa/backups`, definidos como `Path` hardcoded em várias classes (`GenerationService`, `FileApplicationService`, `GeneratedArtifactReader`, `ApplyManifestValidator`), **relativos ao cwd do processo JVM, não ao `projectPath` do usuário**. Setters existem mas são package-private (só para testes injetarem `@TempDir`). **Achado crítico**: o `application.yml` já tem um bloco `auto-qa:` completo (`allowed-roots`, `max-files`, `max-file-size-kb`, diretórios, `allow-command-execution` etc.) mas **nenhuma classe `@ConfigurationProperties` o lê** — está totalmente desconectado do código, é hoje "decorativo". Sem limpeza/retenção automática (nenhum `@Scheduled` no projeto).
- **`executionId`**: sempre `UUID.randomUUID()` gerado dentro do construtor privado de `AutoQaContext`. Nenhum ponto do código aceita um `executionId` externo.
- **Entry point de produção**: **não existe nenhum**. A cadeia `AutoQaContext.create(...)` → `AutoQaWorkflowService.execute(...)` só aparece em testes JUnit (`AutoQaWorkflow*IntegrationTest`, `AutoQaWorkflowServiceTest`). O módulo Auto QA de 11 fases hoje só roda dentro da suíte de testes.
- **Estrutura do resto do backend**: `controller/` (fino) → `business/service/*` → `infrastructure/repository/*` (Mongo) → `infrastructure/entity/*` (`@Document`). DTOs em `business/dto/` (pasta plana). Propriedades externas via `business/properties/*` com `@ConfigurationProperties` dedicado por integração (`OpenAiProperties`, `GeminiProperties`, `JiraProperties`) — **padrão que o Auto QA ainda não segue**, apesar do bloco `auto-qa:` já existir no yml.

### Respostas às 10 perguntas do diagnóstico de backend

1. **Quais endpoints Auto QA já existem?** Nenhum no backend atual.
2. **Quais ainda são úteis?** Nenhum tecnicamente (não existem); os *nomes* de alguns endpoints que o frontend antigo espera (`validate`, `select-folder`, `generated-files`, `download`) são úteis como referência conceitual de fluxo.
3. **Quais são legados?** Todos os que o `AutoQaService` do frontend antigo chama são, na prática, órfãos — contratos sem implementação correspondente no backend atual.
4. **Quais devem ser substituídos?** Não há o que substituir tecnicamente; conceitualmente, o fluxo que os endpoints antigos definiam (validate project → select folder → analyze → executions/{id} → apply/generate/execute → download) inspira, mas não vincula, os novos contratos propostos abaixo.
5. **Quais estão acoplados à tela antiga?** Todos os do frontend antigo — mas o acoplamento existe só do lado do frontend; não há nada a desacoplar no backend.
6. **Existe persistência real do `AutoQaContext`?** Não, nenhuma.
7. **Como uma execução pode ser retomada após reinício da aplicação?** Hoje: impossível — sem persistência, sem mecanismo de retomada.
8. **Quais dados existem somente em memória?** Todo o `AutoQaContext` — as 12 fases completas, incluindo campos potencialmente sensíveis (stdout/stderr, paths).
9. **Quais dados não podem ser enviados ao frontend?** `projectPath` absoluto, prompts, respostas brutas de IA, stdout/stderr completos, variáveis de ambiente, stacktraces, conteúdo completo de backup — detalhado na seção 8 (DTO público).
10. **Quais ações alteram arquivos ou executam comandos hoje?** `GenerationAgent`/`GenerationService` grava em área de *staging* (`.auto-qa/generated/{executionId}`, não o projeto real). `ApplyAgent`/`FileApplicationService` é quem efetivamente copia para o projeto real (com backup). `ExecuteAgent`/`TestExecutionService` executa o comando de teste real contra o projeto. `FailureAnalysisAgent` e `LearningAgent` são somente leitura/análise.

---

## 2. Diagnóstico do frontend atual

Repositório: `/Users/jeanheberth/Development/front/gerar-cenario-teste-app` (git separado).

- **Angular 19.2.25**, standalone components (sem NgModules), SSR habilitado, TypeScript `strict: true`.
- **Sem gerenciador de estado formal** — nem NgRx, nem ComponentStore, nem Signals em uso hoje. Estado local por componente + RxJS (`firstValueFrom`) para chamadas HTTP.
- **UI**: Bootstrap listado no `package.json` mas carregado via CDN em `index.html` (versão divergente, `5.3.3` vs `^5.3.5`) e **não usado de forma sistemática** — cada componente tem CSS próprio, sem design system/tokens (`styles.css` tem 25 linhas, zero CSS variables).
- **Estrutura**: pasta **plana** — cada feature é uma pasta no nível raiz de `src/app/` (`cenario/`, `cenario-list/`, `chat-agentes/`, `autoqa-artifacts/`), sem `features/`/`pages/`/`shared/`. `models/` e `services/` são pastas transversais, mas só a feature Auto QA as usa de fato.
- **Tela Auto QA antiga** (`AutoqaArtifactsComponent`, 604 linhas): isolada em feature própria, rotas `/auto-qa` e `/autoqa-artifacts` (duplicadas, apontando para o mesmo componente). Consome `AutoQaService` (`services/autoqa.service.ts`) chamando endpoints (`/api/auto-qa/project/validate`, `/analyze`, `/executions/{id}`, `/apply`, `/generate`, `/execute`, `/discard`, `/generated-files`, `/download` etc.) **que não existem no backend atual** — a tela está funcionalmente órfã hoje. `models/autoqa.interface.ts` (265 linhas) é uma referência rica de nomenclatura de tipos, mas não reaproveitável tecnicamente (contrato novo). Mapeamento "status do backend → step visual" (`statusOrder`, `stepState()`) é o principal artefato de lógica reaproveitável **conceitualmente**.
- **Services HTTP**: só a feature Auto QA tem service dedicado; as outras 3 features chamam `HttpClient` direto no componente. Sem service base/genérico.
- **Interceptors**: nenhum. `provideHttpClient(withFetch())` sem `withInterceptors`.
- **Tratamento de erro HTTP**: nenhum centralizado — mensagens de erro como string local por componente, um `alert()` nativo isolado.
- **Componentes compartilhados**: nenhum (não existe pasta `shared/`/`common/`) — tudo duplicado por feature.
- **Testes**: 2 arquivos `.spec.ts` no projeto inteiro, ambos da feature Auto QA (Karma/Jasmine). **Sem e2e** (nenhum Cypress/Playwright/Protractor).
- **Estado**: nenhum `signal()`/`BehaviorSubject`/`Subject` usado como estado compartilhado hoje.

### Respostas às 10 perguntas do diagnóstico de frontend

1. **Estrutura atual?** Pasta plana por domínio na raiz de `src/app/`.
2. **Versão do Angular?** 19.2.25, standalone.
3. **Material/Bootstrap/CSS próprio?** Bootstrap nominalmente presente mas não efetivamente usado; CSS próprio de fato, sem tokens.
4. **Tela antiga isolada ou acoplada?** Isolada (feature própria), porém órfã (backend correspondente não existe).
5. **Services HTTP reutilizáveis?** `AutoQaService` só como referência de nomenclatura — será reescrito por completo (contrato novo).
6. **Componentes reutilizáveis?** Nenhum tecnicamente (componente monolítico, sem filhos extraídos).
7. **Componentes a substituir?** Todo o `AutoqaArtifactsComponent`.
8. **Existem testes frontend?** Sim, 2 arquivos, só da feature Auto QA.
9. **Existe interceptor de erro?** Não.
10. **Existe padrão de estado global?** Não.

---

## 3. Endpoints existentes

**No backend real: zero.** O que o frontend antigo *espera* (mas não existe hoje) está documentado no item 2 acima, para referência histórica — não serve de contrato a preservar.

---

## 4. Endpoints propostos

Convenção geral: JSON, `Content-Type: application/json`, todos sob `/api/auto-qa`. Toda ação sensível (`generate`, `apply`, `execute`) exige que a aprovação correspondente já tenha sido registrada. Respostas de ação (`start`/`generate`/`apply`/`execute`) propostas como **assíncronas (202 Accepted)** — ver seção 6 (justificativa: `ExecuteAgent` pode levar até `max-execution-minutes: 10`, inviável manter uma requisição HTTP síncrona aberta por isso).

| Endpoint | Finalidade |
|---|---|
| `POST /executions` | Cria uma nova execução (registra `scenario` + `projectReference`), status inicial `CREATED`. Não roda nenhum agente ainda. |
| `GET /executions` | Lista execuções (histórico), paginado, com filtros básicos (status, período). |
| `GET /executions/{executionId}` | Consulta o estado atual — é o DTO público (seção 8), fonte de verdade para polling/exibição. |
| `POST /executions/{executionId}/start` | Dispara Discovery→ScenarioAnalysis→ProjectKnowledge→Planning (Order 0–30). |
| `POST /executions/{executionId}/continue` | Avança automaticamente o que não exigir aprovação humana (uso genérico/retry após falha técnica transitória). |
| `POST /executions/{executionId}/generate` | Dispara Generation+Review (Order 40–50). A própria chamada é o ato de aprovação humana da geração (ver nota de design abaixo). |
| `POST /executions/{executionId}/apply-approval` | Registra `ApplyApproval` (operações autorizadas: CREATE/UPDATE/REUSE). |
| `POST /executions/{executionId}/apply` | Dispara Apply (Order 60) — exige `apply-approval` já registrada. |
| `POST /executions/{executionId}/execution-approval` | Registra `ExecutionApproval`. |
| `POST /executions/{executionId}/execute` | Dispara Execute+FailureAnalysis+Learning (Order 70–90) — exige `execution-approval` já registrada. |
| `GET /executions/{executionId}/generated-files` | Lista metadados dos arquivos gerados (sem conteúdo). |
| `GET /executions/{executionId}/generated-files/{fileId}` | Retorna conteúdo de UM arquivo (com limite de tamanho, sanitizado). |
| `GET /executions/{executionId}/events` | Streaming SSE de progresso (ver seção 7). |
| `POST /executions/{executionId}/cancel` | Marca `CANCELLED`; não interrompe agente já em execução, só impede novas etapas. |
| `DELETE /executions/{executionId}` | Remove do histórico (soft — nunca apaga `.auto-qa/generated`/`backups` físicos). |

**Nota de design — por que não existe `POST /generation-approval`:** `AutoQaContext` hoje só tem objetos de aprovação formais para Apply e Execution (`ApplyApproval`/`ExecutionApproval`) — não há um terceiro objeto para "aprovar geração". Proponho **não inventar um novo conceito de domínio** para isso: o próprio ato de o usuário clicar em "Aprovar e gerar" no frontend, que dispara `POST /generate`, já É a aprovação (auditável via log + timestamp do próprio documento de execução). Fica marcado como decisão a confirmar explicitamente na 12.2.

### Detalhamento por endpoint (finalidade / request / response / status / precondições / erros / segurança / idempotência / altera arquivos / executa comando / exige aprovação / dados sanitizados)

**`POST /executions`**
- Request: `{ scenario: string, projectReference: string }` (nunca path absoluto — ver seção 11).
- Response: `AutoQaExecutionResponse` (seção 8) com `status=CREATED`.
- Status: `201 Created` + `Location`.
- Precondições: `scenario` não vazio; `projectReference` válido contra allowlist (a reconectar — ver dívidas técnicas).
- Erros: `400` (validação), `422` (projectReference fora da allowlist).
- Segurança: nenhuma hoje (sem Spring Security) — propor header mínimo `X-User` para auditoria.
- Idempotente: não por padrão; suportar `Idempotency-Key` opcional para evitar duplo-clique criando duas execuções.
- Altera arquivos / executa comando: não.
- Exige aprovação: não (é o próprio início).
- Sanitização: `projectReference` nunca ecoa o path absoluto de volta.

**`GET /executions`** — leitura, paginada, `200`, sem precondição, sem erro exceto `400` em filtro inválido, idempotente, não altera nada, sanitizado (lista usa o mesmo DTO resumido).

**`GET /executions/{executionId}`** — leitura, `200`/`404`, idempotente, não altera nada, sanitizado (DTO completo da seção 8).

**`POST .../start`**
- Response: `202 Accepted` (processamento assíncrono) com `workflowStatus=RUNNING`.
- Precondições: `status==CREATED` (ou `BLOCKED`/`ERROR` para retry, a definir).
- Erros: `404`, `409` (já iniciado / operação em andamento).
- Não altera arquivos, não executa comando (Discovery..Planning são só leitura/análise).
- Não exige aprovação prévia.
- Idempotente: não (mas protegido por lock `operationInProgress`, ver seção 10).

**`POST .../continue`** — mesmo contrato de `/start`, mas calculado pelo Orchestrator a partir do estado atual (avança o que puder sem aprovação pendente); `409` se não houver nada a continuar.

**`POST .../generate`**
- Precondições: `TechnicalPlanResult` presente e `PlanningStatus` pronto.
- Altera arquivos: sim, mas apenas em área de *staging* (`.auto-qa/generated`), nunca no projeto real.
- Executa comando: não.
- Exige aprovação: sim (a própria chamada, ver nota de design acima).
- Erros: `409` (etapa anterior não concluída), `422` (plano inválido).

**`POST .../apply-approval`**
- Request: `{ approved: true, approvedBy: string, authorizedOperations: ["CREATE","UPDATE","REUSE"] }`.
- `200`/`201`, `409` se já registrada (natural do `AutoQaContext.registerApplyApproval`, que já lança `IllegalStateException` em segundo registro).
- Não altera arquivo, não executa comando. É o próprio ato de aprovação.

**`POST .../apply`**
- Precondições: `apply-approval` registrada, `CodeReviewResult` aprovado.
- Altera arquivos: **sim, no projeto real** (com backup automático).
- Executa comando: não.
- Erros: `409` (sem aprovação / etapa anterior ausente), `422` (conflito de arquivo).

**`POST .../execution-approval`** — mesmo padrão de `apply-approval`, para `ExecutionApproval` (`allowTestExecution`).

**`POST .../execute`**
- Precondições: `execution-approval` registrada, `ApplyResult` concluído.
- Altera arquivos: não (só lê o projeto já aplicado).
- Executa comando: **sim** (comando de teste real).
- Ao concluir, dispara automaticamente FailureAnalysis+Learning (não exigem aprovação separada).
- Erros: `409` (sem aprovação), `500` (falha técnica de execução, distinta de `ExecutionStatus.FAILED`, que é sucesso do agente).

**`GET .../generated-files`** / **`GET .../generated-files/{fileId}`** — leitura, `200`/`404`, `413`/`400` se conteúdo exceder limite configurado (a reconectar — `max-file-size-kb` hoje morto no yml), idempotente, sanitizado (nunca stdout/stderr, nunca path absoluto).

**`GET .../events`** — SSE, `200` com `text/event-stream`, sem alterar nada, ver seção 7.

**`POST .../cancel`** — `200`/`409` (já finalizada), não reverte arquivos já aplicados (limitação explícita, fora de escopo).

**`DELETE /executions/{executionId}`** — `204`/`404`, soft-delete, nunca apaga diretórios físicos.

---

## 5. Proposta de persistência

**`AutoQaExecutionDocument`** (coleção `autoqa_executions`, seguindo o padrão já usado por `Cenario`/`ChatSession`):

```java
@Document(collection = "autoqa_executions")
public class AutoQaExecutionDocument {
    @Id private String id;                 // Mongo id, padrão do projeto
    private UUID executionId;              // indexado, único
    private String scenario;
    private String projectReference;       // NUNCA path absoluto
    private Instant createdAt, updatedAt, startedAt, finishedAt;
    private String currentStage;
    private String workflowStatus;         // CREATED, RUNNING, WAITING_APPROVAL, BLOCKED, FINISHED, ERROR, CANCELLED
    private List<StageRecord> stages;      // nome, status, timestamps, resumo curto
    private ApprovalsSnapshot approvals;   // metadados de apply/execution approval (quem, quando)
    private DiscoverySummary discoverySummary;
    private ScenarioAnalysisSummary scenarioAnalysisSummary;
    private KnowledgeSummary knowledgeSummary;
    private PlanningSummary planningSummary;
    private GenerationSummary generationSummary;
    private ReviewSummary reviewSummary;
    private ApplySummary applySummary;
    private ExecutionSummary executionSummary;
    private FailureSummary failureSummary;
    private LearningSummary learningSummary;
    private List<WarningRecord> warnings;
    private List<String> errors;           // mensagens curtas, sanitizadas
    private boolean operationInProgress;   // lock local (seção 10)
    @Version private Long version;         // optimistic locking — NOVO padrão no projeto
    private int schemaVersion;             // versionamento de formato, distinto do @Version acima
}
```

**O que persistir integralmente:** listas pequenas já estruturadas e limitadas pelas próprias Fases 1–11 (findings, issues, warnings — já bounded a 10–50 itens, evidence já truncada a 300–512 chars).

**O que persistir resumido:** os records completos de cada fase (`GenerationResult`, `ExecutionResult` etc.) **não** vão para o documento como estão — campos como `GeneratedFile.content()` e `ExecutionResult.stdout()/stderr()` são grandes/sensíveis. Propor um `SummaryXxx` por fase (status, confidence, contagens, timestamps, top N mensagens/paths).

**Decisão a confirmar na 12.2** — retomada real (reconstruir `AutoQaContext` para *continuar* rodando agentes, não só exibir) exige mais do que os resumos de exibição: ou (a) persistir também o record completo sanitizado de cada fase concluída (removendo só os campos proibidos) em um campo interno nunca exposto pela API pública, permitindo reidratação fiel; ou (b) aceitar que, ao retomar, o último estágio incompleto sempre reexecuta do zero. Recomendo (a), mas fica como decisão explícita da 12.2, não desta rodada.

**Nunca persistir:** credenciais, API keys, conteúdo integral desnecessário, `projectPath` absoluto, prompts, respostas brutas de IA, stdout/stderr ilimitado, variáveis de ambiente, backups integrais.

**Tamanho:** com os limites já aplicados pelas Fases 1–11, um documento resumido deve ficar na casa de dezenas de KB — muito abaixo do limite de 16MB do Mongo. Propor guard defensivo (log de warning se o documento serializado passar de ~500KB).

**Versionamento:** `@Version Long version` (Spring Data, **primeira introdução desse padrão no projeto**) para optimistic locking; `int schemaVersion` separado para migração de formato do documento no tempo.

**TTL:** propor índice TTL opcional só para execuções em estado terminal antigo (ex. 90 dias após `finishedAt`), nunca para execuções ativas — detalhe de implementação para 12.2.

**Auditoria:** reaproveitar os campos de aprovação já existentes (`approvedBy`/timestamp) de `ApplyApproval`/`ExecutionApproval`.

**Concorrência/optimistic locking/retomada:** ver seções 6 e 10.

---

## 6. Orquestração e retomada

### O `AutoQaWorkflowService` atual suporta...

| Capacidade | Suporta hoje? |
|---|---|
| Pausas para aprovação | Não |
| Retomada do workflow | Não |
| Persistência entre etapas | Não |
| Continuação após geração/aplicação/execução | Não diretamente — mas cada **agente** já valida suas próprias pré-condições lendo o contexto (ex. `ExecuteAgent` já checa `ApplyStatus`) |
| Status `BLOCKED` que não seja erro técnico | Não distingue — hoje qualquer `success()==false` é tratado igual, mesmo um `LearningResult.BLOCKED` semântico |
| `ExecutionStatus.FAILED` ainda permitindo `FailureAnalysisAgent` | **Sim, já funciona hoje** — `ExecuteAgent` retorna `success()==true` para `FAILED`, então o workflow continua normalmente |
| Etapas opcionais | Não existe o conceito |
| Cancelamento | Não existe |

### Opções avaliadas

**A — Workflow completo sem pausas.** Contradiz o requisito de aprovação humana explícita. Descartada.

**B — Workflow por etapas** (uma instância de `AutoQaWorkflowService` por chamada HTTP, com sublista de agentes — exatamente como os testes de integração já fazem: `runFase1a6`, `runApply`, `runExecute`). Reaproveita 100% do `AutoQaWorkflowService`, zero alteração nele.

**C — State machine explícita** (Spring State Machine). Overhead desproporcional a um fluxo essencialmente linear; reimplementaria o que a cadeia de `registerXxx` já garante.

**D — Orchestrator persistente com `continueFromStage`.** Formaliza a Opção B: um `AutoQaExecutionOrchestrator` novo que (1) carrega o documento, (2) reidrata o `AutoQaContext`, (3) decide qual sublista de agentes rodar dado o comando recebido, (4) invoca `AutoQaWorkflowService` inalterado, (5) persiste o novo estado.

**Recomendação: D.** Menor risco técnico (zero mudança no motor já testado com 1500+ testes), ponto único de decisão testável isoladamente, sem dependência nova.

### Execução síncrona vs. assíncrona

Dado que `ExecuteAgent` pode levar até `max-execution-minutes: 10` (hoje morto no yml, mas sinaliza a expectativa), recomendo que as ações (`start`/`generate`/`apply`/`execute`) respondam **`202 Accepted`** imediatamente, processando em background (`@Async`/executor dedicado), com acompanhamento via `GET`/SSE. Decisão a confirmar na 12.2, mas já sinalizada aqui por afetar diretamente o DTO (precisa expor "operação em andamento") e a concorrência (lock por `executionId`).

### `BLOCKED` não-técnico

Proponho que o Orchestrator, após cada chamada de agente, **inspecione** o resultado registrado no contexto (`FailureAnalysisResult.status()`/`LearningResult.status()`) e mapeie `BLOCKED` semântico para `workflowStatus=BLOCKED` no documento — distinto de `ERROR` — sem exigir nenhuma mudança nos Agents/Services das Fases 1–11.

### Cancelamento / etapas opcionais

Cancelamento: `workflowStatus=CANCELLED`, checado pelo Orchestrator **antes** de iniciar qualquer nova etapa (não interrompe agente já em execução — não há hook de interrupção nos agentes hoje, fora de escopo introduzir). Etapas opcionais: nenhuma necessidade identificada nesta fase.

---

## 7. Atualização em tempo real

| Critério | Polling | SSE | WebSocket |
|---|---|---|---|
| Complexidade | Baixíssima | Média | Alta |
| Confiabilidade | Alta (stateless) | Boa (reconexão nativa do `EventSource`) | Exige lib/reconexão manual |
| Suporte Angular | Nativo (`HttpClient`) | Nativo (`EventSource`) | Exige lib nova (nenhuma presente hoje) |
| Reconexão | N/A | Automática | Manual |
| Escalabilidade | Trivial (baixo volume esperado) | Ok para poucas execuções simultâneas; atenção a timeout de proxy | Mais pesado, complica deploy atrás de proxy |
| Segurança | Igual a qualquer REST | Unidirecional, sem superfície nova | Superfície bidirecional maior |
| Compatibilidade com backend atual | Total | Boa (`SseEmitter` nativo do Spring MVC) | Exige nova dependência/infra |

**Recomendação: SSE**, com **polling como fallback/ponto de partida** (a 12.2/12.4 pode nascer só com polling via `GET /executions/{id}`, introduzindo SSE como enhancement na 12.4 sem quebrar contrato). Justificativa: caso de uso estritamente unidirecional (servidor→cliente), SSE cobre isso nativamente sem dependência nova em nenhum dos dois lados, com reconexão automática — relevante para uma operação de até 10 minutos. WebSocket seria desproporcional (bidirecionalidade não é necessária) — não proponho por preferência, e sim por comparação explícita acima.

---

## 8. DTO público proposto

```java
public record AutoQaExecutionResponse(
    UUID executionId,
    String scenario,
    AutoQaWorkflowStatus status,
    String currentStage,
    ProgressInfo progress,                    // completedStages, totalStages
    List<StageStatusView> stages,             // nome, status, timestamps, resumo curto
    AutoQaSummariesView summaries,             // um resumo leve por fase (tipado, não Map genérico)
    List<PendingApprovalView> pendingApprovals,
    List<AutoQaAvailableAction> availableActions,
    Instant createdAt, updatedAt, startedAt, finishedAt,
    List<LearningWarningView> warnings,
    List<ControlledErrorView> errors
) {}
```

**Nunca inclui:** `projectPath` absoluto (usa `projectReference`), prompt, resposta bruta de IA, segredo, `environment`, conteúdo completo de backup, stacktrace, exceção interna, qualquer campo não necessário ao frontend. Este DTO **nunca** serializa diretamente os records de `model/**` das fases — sempre via um `AutoQaExecutionMapper` dedicado, campo a campo (nunca reflection genérica), para impedir vazamento acidental de um campo sensível adicionado no futuro.

### `AutoQaAvailableAction`

```java
public enum AutoQaAvailableAction {
    START, CONTINUE, APPROVE_GENERATION, APPROVE_FILE_UPDATE, APPLY,
    APPROVE_EXECUTION, EXECUTE, CANCEL, RETRY,
    VIEW_GENERATED_FILES, VIEW_DIFF, VIEW_LOGS, VIEW_LEARNING, NONE
}
```

Calculadas **sempre no backend** (um `AutoQaAvailableActionsResolver` dedicado, chamado pelo Orchestrator) — o frontend só habilita/desabilita botões a partir da lista recebida, nunca decide sozinho.

---

## 9. Estrutura Angular proposta

Adaptando ao padrão real do projeto (pasta plana), com desvio pontual justificado pela complexidade (agrupamento interno por camada, algo que nenhuma feature existente tem hoje):

```
src/app/auto-qa-bmad/
├── auto-qa-bmad.routes.ts
├── pages/
│   ├── execution-dashboard/
│   ├── execution-new/
│   ├── execution-detail/
│   └── execution-history/
├── components/
│   ├── workflow-timeline/
│   ├── execution-summary/
│   ├── discovery-panel/  scenario-panel/  knowledge-panel/  planning-panel/
│   ├── generated-files-panel/  code-preview/  file-diff/
│   ├── review-panel/  apply-panel/  execution-panel/
│   ├── failure-panel/  learning-panel/
│   ├── approval-dialog/  status-badge/
├── services/
│   ├── auto-qa-api.service.ts
│   ├── auto-qa-state.service.ts
│   └── auto-qa-events.service.ts
├── models/
└── guards/
```

Componentes genuinamente compartilháveis (badge de status, spinner, empty-state, confirm-dialog) vão para `src/app/shared/` **(primeira introdução desse padrão no projeto)**.

**Rota proposta: `/auto-qa-bmad`** — evita colisão/confusão com `/auto-qa` e `/autoqa-artifacts` existentes (que continuam apontando para a tela antiga até decisão futura sobre seu destino).

---

## 10. Estratégia de estado frontend

Avaliado: NgRx (descartado — sem precedente no projeto, overhead desproporcional), ComponentStore (dependência extra sem necessidade, já coberta por Signals nativos), RxJS puro com `BehaviorSubject` (funcional, mas não é o caminho atual recomendado pelo próprio Angular 19).

**Recomendação: `AutoQaStateService` com Signals nativos do Angular** (`signal`/`computed`/`effect`), expondo o estado da execução ativa e derivados computados (`currentStageView`, `availableActions`, `pendingApprovals`). RxJS continua usado internamente só para HTTP/polling/SSE, convertido para signal via `toSignal()` (`@angular/core/rxjs-interop`, já disponível no Angular 19). Zero dependência nova, compatível com a versão atual.

---

## 11. Estratégia visual

Layout 3 colunas do mockup (timeline | conteúdo da etapa | resumo), colapsando em telas menores para stepper horizontal compacto + card de resumo colapsável.

**Estados a cobrir em cada painel:** loading, empty, error (falha técnica), blocked (bloqueio semântico — visualmente distinto de error, cor/ícone diferentes), success.

**Timeline vertical:** badge de status por etapa + timestamp + clique expande o painel central (inclusive para revisar etapas passadas, não só a atual).

**Preview/diff:** `code-preview` (syntax highlight leve, lib a avaliar na 12.5) e `file-diff` para UPDATE — inspirado conceitualmente na tela antiga, sem reaproveitar código.

**Logs:** nunca stdout/stderr completo (o backend já não expõe) — só o que o DTO público traz, com nota explícita de que logs completos não são expostos.

**Tabelas** de issues/findings/aprendizados: simples, com filtro por severidade/categoria, paginação client-side (volume pequeno, já limitado pelas Fases 1–11).

**Modal de aprovação** (`approval-dialog`): reutilizável, detalha o que será afetado, exige confirmação explícita nomeada (não um "OK" genérico) — usado para geração, UPDATE, aplicação, execução.

**Acessibilidade:** ARIA nos badges de status (não só cor), navegação por teclado na timeline (setas + Enter), foco visível, contraste AA, nunca depender só de cor para status (ícone + texto).

---

## 12. Backlog das subfases 12.2–12.8

**12.2 — Persistência e endpoints.** Objetivo: `AutoQaExecutionDocument`+`Repository`, `AutoQaProperties` (reconectando o yml morto), `AutoQaExecutionOrchestrator`, `AutoQaExecutionMapper`/DTOs, `AutoQaAvailableActionsResolver`, controller com os 14 endpoints, exception handlers dedicados (400/404/409/422). Nenhuma alteração nas Fases 1–11. Critério de aceite: fluxo completo via HTTP sobrevivendo a restart do processo. Risco principal: reidratação do `AutoQaContext`. Não implementado: SSE, qualquer UI.

**12.3 — Estrutura Angular (esqueleto).** `auto-qa-bmad/` com rotas, `AutoQaApiService`, `AutoQaStateService`, models, `shared/` básico. Critério: criar execução real via UI sem estilo refinado. Não implementado: timeline/painéis completos.

**12.4 — Execução e timeline.** `workflow-timeline`, `execution-summary`, `execution-detail`, polling (SSE se já decidido), painéis básicos de leitura (Discovery..Planning). Critério: acompanhar Discovery→Planning em tempo real na UI.

**12.5 — Arquivos, preview, diff e review.** `generated-files-panel`, `code-preview`, `file-diff`, `review-panel`. Critério: visualizar arquivos gerados, diff de UPDATE, issues do review.

**12.6 — Aprovações, apply e execute.** `approval-dialog`, `apply-panel`, `execution-panel`, fluxo completo com confirmação explícita e proteção de duplo clique. Critério: aplicar e executar de ponta a ponta via UI.

**12.7 — Failure, learning e histórico.** `failure-panel`, `learning-panel`, `execution-history`, `execution-dashboard`. Critério: visualizar FailureAnalysisResult/LearningResult, navegar histórico.

**12.8 — Testes e refinamento.** Fecha cobertura planejada na seção 13, acessibilidade/responsividade, revisão de segurança (allowed-roots, limites reconectados). Critério: suíte completa verde, sem regressão nas Fases 1–11.

---

## 13. Testes planejados

**Backend:** controller (`MockMvc`, por endpoint incl. 400/404/409), orchestrator (unit, mockando repository e `AutoQaWorkflowService`), mapper (unit — garantir nunca vazar campo proibido), repository (integração Mongo), retomada (persistir→"reiniciar"→reidratar→continuar), concorrência (dois requests simultâneos → um 200 um 409), idempotência, segurança (payload sanitizado, `allowed-roots` rejeitando path fora da lista), available actions (matriz estado→ações), status HTTP. Infra de teste Mongo (Testcontainers vs. `flapdoodle` embarcado) — decisão para 12.2, não investigada nesta rodada (não avaliei o pipeline de CI atual do backend).

**Frontend:** services HTTP (`HttpTestingController`, padrão já usado em `autoqa.service.spec.ts`), state service (transições de signal), timeline (renderização por status), painéis (5 estados: loading/empty/error/blocked/success), aprovações (modal→confirma→chama service certo), responsividade (limitação: Angular não tem boa cobertura automatizada disso), acessibilidade (checklist manual ou `axe-core` se e2e for introduzido), navegação, integração, e2e do fluxo principal (exige introduzir Cypress/Playwright do zero — decisão para 12.8).

---

## 14. Riscos

- Reidratação do `AutoQaContext` a partir do Mongo — maior risco técnico novo, sem precedente nas Fases 1–11.
- Execução potencialmente longa (até 10 min) — precisa decisão explícita assíncrona desde a 12.2, não como retrofit.
- `.auto-qa/generated`/`.auto-qa/backups` relativos ao cwd — risco real em deploy Windows/Tomcat.
- Bloco `auto-qa:` do yml morto (`allowed-roots`, limites de payload) — falsa sensação de segurança hoje.
- Zero autenticação — qualquer exposição fora de localhost precisa de camada mínima antes.
- CORS `*` — revisar antes de produção.
- Frontend antigo órfão — decisão futura (fora desta rodada) sobre removê-lo/redirecioná-lo.
- Infra de teste Mongo não confirmada no projeto.
- Zero precedente de state management/componentes compartilhados no frontend — primeira introdução, maior superfície de decisão a acertar cedo.

---

## 15. Dívidas técnicas

**Obrigatória (herdada):** `FailureAnalysisService` atualmente não combina findings válidos da IA (`aiFindings = List.of()`) — registrada, **não corrigida** nesta rodada nem nesta fase.

**Identificadas neste diagnóstico:**
- Bloco `auto-qa:` do `application.yml` totalmente desconectado (`allowed-roots`, limites, diretórios) — "decorativo" hoje.
- `.auto-qa/generated`/`.auto-qa/backups` relativos ao cwd do processo, não ao `projectPath` nem a config explícita.
- Exceções de negócio do Auto QA caem todas no handler HTTP genérico → sempre 500, mesmo as `*ValidationException`.
- Nenhuma persistência de Auto QA hoje — motivador central da Fase 12.
- Nenhum `@Version`/optimistic locking usado em nenhum lugar do projeto hoje — será a primeira introdução.
- Sem Spring Security/identidade de usuário no projeto inteiro.
- Frontend antigo (`AutoqaArtifactsComponent`) órfão, chama endpoints inexistentes.
- Rota duplicada `/auto-qa` e `/autoqa-artifacts` no frontend.
- Nenhum componente/service compartilhado real no frontend hoje.
- Sem testes E2E no frontend, sem Testcontainers confirmado no backend.

---

## 16. Arquivos que foram apenas lidos

**Backend** (via agente de exploração, somente leitura): controllers (`AgentController`, `CenarioController`, `JiraController`, `ApiExceptionHandler`), `AutoQaWorkflowService.java`, `AutoQaContext.java`, `application.yml`, `infrastructure/entity/Cenario.java`, `infrastructure/entity/chat/ChatSession.java`, `infrastructure/repository/CenarioRepository.java`, `infrastructure/repository/ChatSessionRepository.java`, `business/dto/*` (11 arquivos), `business/config/CorsConfig.java`, `business/autoqa/generation/GenerationService.java`, `business/autoqa/apply/FileApplicationService.java`, `business/autoqa/review/GeneratedArtifactReader.java`, `business/autoqa/apply/ApplyManifestValidator.java`, `business/autoqa/apply/FileBackupService.java`, `business/autoqa/generation/GenerationManifestWriter.java`, `business/properties/*`, e a suíte `AutoQaWorkflow*IntegrationTest.java`/`AutoQaWorkflowServiceTest.java`.

**Frontend** (via agente de exploração, somente leitura): `package.json`, `angular.json`, `tsconfig.json`, `src/index.html`, `src/styles.css`, `src/app/app.routes.ts`, `src/app/app.routes.server.ts`, `src/app/app.component.{ts,html}`, `src/app/app.config.ts`, `src/app/autoqa-artifacts/*` (component/html/css/spec), `src/app/services/autoqa.service.{ts,spec.ts}`, `src/app/models/autoqa.interface.ts`, `src/app/models/workflow-info.interface.ts`, `src/app/cenario/cenario.component.ts`, `src/app/cenario-list/cenario-list.component.ts`, `src/app/chat-agentes/chat-agentes.component.ts`, `src/app/enviroment/enviroment.{dev,prd}.ts`.

---

## 17. Confirmação de que nenhum arquivo foi alterado

Confirmado. Toda a investigação foi feita por dois agentes de exploração somente-leitura (sem acesso a `Edit`/`Write`). Nenhum controller, service, model, config, componente Angular ou arquivo de qualquer natureza foi criado ou modificado nesta etapa. `git status` de ambos os repositórios permanece inalterado por este diagnóstico.

## 18. Confirmação de que aguardará aprovação

Nenhuma implementação (12.2 em diante) será iniciada até aprovação explícita.
