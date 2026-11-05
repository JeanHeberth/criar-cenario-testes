# Plano exibido no portão de aprovação (frontend)

**Data:** 2026-08-25
**Método:** SDD → TDD (spec, teste vermelho, implementação mínima)
**Repositório:** `front/gerar-cenario-teste-app`

---

## Spec

O painel de detalhe da execução exibe o plano técnico **acima da barra de
ações**: título, estratégia, uma linha por arquivo planejado (operação, caminho
e motivo) e as advertências do plano, com destaque para as que exigem decisão
humana.

Enquanto não há plano, o painel **não renderiza nada** — nem moldura vazia.

## Motivação

O portão era cego: o usuário clicava em "Aprovar e gerar código" sem ver o que
seria gerado. As advertências da auditoria de reuso, criadas para informar essa
decisão, existiam na API e não chegavam à tela.

## Arquivos criados

| arquivo | conteúdo |
|---|---|
| `components/plan-panel/plan-panel.component.ts` | componente apresentacional |
| `components/plan-panel/plan-panel.component.html` | template |
| `components/plan-panel/plan-panel.component.scss` | estilos |
| `components/plan-panel/plan-panel.component.spec.ts` | 4 testes |

## Arquivos alterados

| arquivo | alteração |
|---|---|
| `models/auto-qa-execution.model.ts` | tipos `AutoQaPublicPlan`, `AutoQaPublicFileAction`, `AutoQaPublicPlanWarning` e campo `plan` |
| `pages/execution-detail-page/...component.ts` | import e registro do componente |
| `pages/execution-detail-page/...component.html` | painel antes da barra de ações |

## Decisões de projeto

**O painel vem antes da barra de ações.** O plano é o que sustenta a decisão de
aprovar; precisa ser lido antes do botão, não depois.

**Nada renderiza sem plano.** Moldura vazia comunica "não há nada a mostrar"
quando a verdade é "ainda não foi planejado" — estados diferentes.

**Só interpolação, nunca `innerHTML`.** Segue a convenção já registrada nos
componentes existentes: conteúdo vindo do backend não vira HTML.

**Advertência com decisão humana tem marca visual própria.** É ela que muda o
que o usuário faz no portão; misturá-la com as demais anularia o propósito.

**Tipos espelham 1:1 os DTOs do backend**, conforme o cabeçalho do arquivo de
modelos já determinava — nenhum tipo inventado no front.

## Verificação

- Suíte do componente: **4 testes, verde**
- Suíte completa do frontend: **539 testes, 538 verdes**
- Build de desenvolvimento: **sem erros**

## Falha pré-existente, não introduzida aqui

`CenarioComponent` — "POST /cenario com o payload exato" falha por
`taskRef: null` presente no payload. É do fluxo Gerar Cenário, fora do escopo
desta alteração: nenhum arquivo desse componente foi tocado (`git status`
confirma). Registrada para tratamento separado.

## O que fica em aberto

O painel exibe o plano, mas a **auditoria de reuso ainda não foi vista em
execução real** — depende de uma rodada completa do pipeline, hoje bloqueada por
crédito na OpenAI.
