# Fase 11 — Learning Agent: Plano Técnico (aprovado com ajustes)

Este documento incorpora as correções obrigatórias solicitadas na revisão do plano original.

## 0. Correção bloqueante da Fase 10 (autorizada)

Antes de implementar o `LearningAgent`, corrigir **exclusivamente** o registro Spring de `FailureAnalysisAgent.java`:

- adicionar `@Component`;
- confirmar `@Order(80)`;
- normalizar imports (`AutoQaAgent`, `FailureAnalysisResult` etc. via import no topo, não FQN inline);
- adicionar logger SLF4J apenas se necessário para seguir o padrão dos demais agentes.

Nenhum comportamento funcional do `FailureAnalysisAg ent` é alterado — só a integração Spring/estilo. Teste novo (`FailureAnalysisAgentTest.java`, hoje inexistente) confirmará `@Component`, `@Order(80)` e presença na lista ordenada do workflow.

## 1. Diagnóstico da arquitetura atual

(mantido do plano original — ver seção correspondente já validada: `AutoQaWorkflowService` agnóstico a agentes concretos; padrão `@Component`+`@Order`+`AutoQaAgent`; `AutoQaContext.registerXxx` com cadeia completa de pré-condições; `FailureAnalysisService` como modelo de orquestração com IA determinística-primeiro; `AiProviderResolver.getActiveProvider()/getFallbackProvider()`; `AiProvider.gerarResposta(system, user)`.)

## 2. Arquivos criados/alterados

Sem mudanças em relação ao plano original (ver lista completa de `model/learning/*`, `learning/*`, `agent/LearningAgent.java` e respectivos testes), **acrescido de**://
- `agent/FailureAnalysisAgentTest.java` (novo, cobre a correção da Fase 10).

Alterações de produção fora do pacote `learning`/`model/learning`/`agent/LearningAgent.java`: **somente** `FailureAnalysisAgent.java` (correção autorizada acima), `AutoQaContext.java` (registro de `LearningResult`). `AutoQaContextTest.java` recebe o novo bloco de testes.

## 3. `LearningScope` — regras corrigidas

`EXECUTION`, `PROJECT`, `FRAMEWORK` são os únicos scopes produzíveis nesta fase. `TEAM`/`GLOBAL` existem no enum como valores reservados, nunca atribuídos por nenhum collector/IA/service/builder — `LearningValidator` rejeita qualquer `LearningItem` com esses dois scopes (`LearningValidationException`).

**Confidence máxima por scope** (regra dura, aplicada em `LearningConfidenceResolver`):

| Scope | Confidence máxima nesta fase | approvalStatus | humanReviewRequired |
|---|---|---|---|
| `EXECUTION` | `HIGH` (com múltiplas evidências diretas e concordantes da mesma execução) | pode ser `NOT_REQUIRED` | conforme confidence (LOW/UNKNOWN → true) |
| `PROJECT` | `MEDIUM` (nunca HIGH numa única execução) | sempre `PENDING` | sempre `true` |
| `FRAMEWORK` | `MEDIUM` (nunca HIGH numa única execução) | sempre `PENDING` | sempre `true` |

Um item `EXECUTION` em `HIGH` significa alta confiança **naquela execução**, nunca um padrão global/recorrente. Múltiplas fontes estruturadas *da mesma execução* (ex. Knowledge + Generation concordando) podem elevar um item `EXECUTION` a `HIGH`, mas **nunca** servem de prova de recorrência para `PROJECT`/`FRAMEWORK` — itens desses dois scopes sempre recebem warning `SINGLE_EXECUTION_ONLY` nesta fase (não há histórico persistido de múltiplas execuções ainda).

## 4. Política de aprovação — regras corrigidas

- `EXECUTION`: `approvalStatus` pode ser `NOT_REQUIRED`; não é reutilizado automaticamente fora da execução; `reusable=true` só dentro do significado local do contrato (exige confidence HIGH/MEDIUM + evidência, como já definido).
- `PROJECT`/`FRAMEWORK`: `approvalStatus` obrigatoriamente `PENDING`; nunca `APPROVED` nem `NOT_REQUIRED` automaticamente; `humanReviewRequired=true` sempre; nunca reutilizados automaticamente (`reusable` pode ser `true` no dado, mas a reutilização automática pelo sistema não ocorre nesta fase — é apenas metadado para decisão humana futura).
- `APPROVED`/`REJECTED` só serão produzidos futuramente por decisão humana (fora do escopo desta fase) — nenhuma classe desta fase atribui esses dois valores.

## 5. Critérios exatos de chamada da IA (corrigidos)

**Não chamar IA** apenas para: melhorar redação, reformular título, produzir descrição mais elegante, resumir algo já consolidado deterministicamente.

**Chamar IA somente quando**: evidência conflitante entre itens determinísticos; classificação determinística insuficiente (`LearningType.UNKNOWN`); confidence determinística `LOW`; relação complexa entre evidências não resolvível deterministicamente.

Consequências diretas:
- item determinístico `MEDIUM`, coerente, sem conflito → **não** chama IA;
- item determinístico `HIGH` de scope `EXECUTION` → **não** chama IA;
- sem evidência → `SKIPPED`, **não** chama IA;
- status operacional → `BLOCKED`, **não** chama IA;
- `LOW` com conflito/insuficiência → **pode** chamar IA.

## 6. ID determinístico do `LearningItem` (corrigido)

Algoritmo: **SHA-256** sobre representação canônica UTF-8 contendo, nesta ordem fixa e delimitada:
```
type + "|" + scope + "|" + tituloNormalizado + "|" + join(relatedComponents ordenados, ",") + "|" + join(relatedFiles ordenados, ",")
```
onde `tituloNormalizado` = trim + lowercase + espaços colapsados (sem remoção de acentuação, para não colidir termos distintos do PT-BR). O digest SHA-256 é convertido em hex string e usado como `id`. Propriedades garantidas: mesma entrada → mesmo id; ordem original das listas não importa (listas são ordenadas antes do hash); não usa `UUID.randomUUID()`/`Math.random()`/`hashCode()`; não expõe dado sensível (título e nomes de componentes/arquivos já são metadados não sensíveis, nunca conteúdo de arquivo).

## 7. Separação de responsabilidades (reforçada)

- `LearningEvidenceExtractor`: só extrai `LearningEvidence` dos 9 records estruturados; não cria `LearningItem`; não calcula confidence; não chama IA; não acessa filesystem.
- `PositiveLearningCollector`/`NegativeLearningCollector`: recebem evidências já extraídas; criam itens determinísticos; não chamam IA; não deduplicam globalmente; não montam o `LearningResult` final.
- `LearningConfidenceResolver`: calcula/ajusta confidence respeitando o teto por scope; não altera `approvalStatus`; não chama IA.
- `LearningDeduplicator`: não modifica itens originais (sempre produz coleção nova); determinístico sempre vence IA; ordem estável; conflito real vira `LearningWarning` tipado, nunca é descartado silenciosamente.
- `LearningSummaryBuilder`: deriva `status`/contagens a partir dos itens já prontos; não corrige item inválido; não aprova nada; não chama IA.
- `LearningValidator`: retorna a mesma instância ou lança `LearningValidationException`; nunca corrige item (confidence, approvalStatus, dedup silenciosa).

## 8. Status — matriz corrigida

| Situação | Status |
|---|---|
| `PASSED` com itens válidos, sem item PROJECT/FRAMEWORK pendente | `COLLECTED` |
| `PASSED` com itens válidos + warnings não bloqueantes | `COLLECTED_WITH_WARNINGS` |
| `PASSED`/`FAILED` com item `PROJECT`/`FRAMEWORK` pendente | `REVIEW_REQUIRED` (exige ≥1 item `PENDING`) |
| `FAILED` com análise válida e aprendizado sustentado, sem pendência | `COLLECTED_WITH_WARNINGS` |
| `FAILED` com análise válida mas sem aprendizado sustentado | `SKIPPED` |
| Sem evidência (qualquer status) | `SKIPPED` (sem item inventado) |
| `ExecutionStatus` ∈ {ERROR, BLOCKED, TIMED_OUT, CANCELLED} | `BLOCKED` + warning operacional |
| `ApplyStatus` ∈ {FAILED, ROLLED_BACK} | `BLOCKED` + warning operacional |
| `FailureAnalysisResult.valid=false` (INVALID) | `BLOCKED` + warning operacional |

## 9. Fallback de IA (confirmado)

Dentro do `LearningService`. No máximo 1 chamada principal + 1 fallback. Fallback só para: `LearningTechnicalException`, `LearningParseException`, resposta `null`/blank/acima do limite, falha técnica do provider. **Nunca** para: `LearningValidationException`, escopo proibido, aprovação inválida, item sem evidência, conflito semântico, erro de programação, status operacional, ausência de aprendizado. Se `fallback` resolver para o mesmo provider (mesma instância/nome) do `active`, não chama de novo — lança `LearningTechnicalException` controlada, sem loop.

## 10. Restrições confirmadas

Sem persistência (MongoDB/JPA/JDBC), sem filesystem (`Files.*`), sem processo (`Runtime.exec`/`ProcessBuilder`), sem alteração de agentes anteriores (exceto a correção autorizada do `FailureAnalysisAgent`), sem alteração de prompts/geração/regras de workflow/frontend, sem início da Fase 12.

## 11. Validação planejada

`git status --short` antes e depois; `./gradlew clean test --tests "com.br.criarcenariotestes.business.autoqa.*" --no-daemon` como baseline e final; `./gradlew clean test --no-daemon` completo ao final; `git diff --check`/`git diff --stat`; greps estáticos de processo/filesystem/persistência sobre `learning/` e `agent/LearningAgent.java`. Nenhum commit é feito — tudo fica no working tree para revisão.
