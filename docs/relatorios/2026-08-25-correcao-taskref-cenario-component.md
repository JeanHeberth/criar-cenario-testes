# Correção da falha de `taskRef` no CenarioComponent

**Data:** 2026-08-25
**Repositório:** `front/gerar-cenario-teste-app`

---

## Sintoma

Teste de caracterização falhando:

```
CenarioComponent — POST {apiUrl}/cenario com o payload exato
Expected object not to have properties
    taskRef: null
```

## Diagnóstico

O **teste estava defasado**, não o componente.

O `CenarioComponent` envia `taskRef` no payload desde a integração com Jira. O
backend declara o campo em `CenarioRequest` e dele deriva o destino da
publicação — comportamento correto e intencional. O teste foi escrito antes
dessa integração e continuava afirmando o payload de três campos.

Verificação feita antes de alterar: `CenarioRequest.java` declara `taskRef` no
construtor, e `cenario.component.ts:467` o envia com `|| null`.

## Correção

Atualizado o contrato afirmado pelo teste para incluir `taskRef: null`, com
comentário registrando por que o campo existe e por que vai como null explícito
em vez de omitido — o backend distingue os dois casos.

## Arquivos alterados

| arquivo | alteração |
|---|---|
| `src/app/cenario/cenario.component.spec.ts` | contrato do payload atualizado; teste novo para task informada |

## Cobertura acrescentada

O caminho em que a task **é** informada não tinha teste nenhum. Adicionei um:
verifica que a `taskRef` vai crua para a API, que aceita tanto URL quanto chave.

Detalhe que o próprio teste expôs: o controle do formulário chama `jiraTaskKey`;
`taskRef` é apenas o nome no payload. A primeira versão do teste usava o nome do
payload e não compilava.

## Verificação

- `cenario.component.spec.ts`: **48 testes, verde**
- Suíte completa do frontend: **540 testes, verde** (antes: 539 com 1 falha)

## Nenhuma alteração de comportamento

Só arquivos de teste foram tocados. O componente e o backend permanecem como
estavam — o defeito era a expectativa, não a implementação.
