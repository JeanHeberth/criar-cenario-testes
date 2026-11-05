# Plano técnico exposto no GET da execução

**Data:** 2026-08-25
**Método:** SDD → TDD (spec, teste vermelho, implementação mínima)

---

## Spec

O `GET /api/auto-qa/executions/{id}` passa a devolver o plano técnico quando
ele já existe para a execução: título, estratégia, ações de arquivo com
operação e motivo, e as advertências do plano — inclusive as da auditoria de
reuso.

Enquanto o planejamento não rodou, o campo vem **nulo**. É assim que o front
distingue "ainda não planejado" de "planejado sem ações".

## Motivação

O portão de aprovação era cego: o usuário clicava em "Aprovar e gerar código"
sem ver o que seria gerado. E as advertências da auditoria de reuso — criadas
justamente para informar essa decisão — não chegavam a ele.

## Descoberta que reduziu o trabalho

O plano **já era persistido**: `AutoQaExecutionSnapshot.technicalPlan` existe e
é gravado a cada fronteira de estágio. Não foi preciso criar persistência —
faltava apenas expor.

## Arquivos criados

| arquivo | conteúdo |
|---|---|
| `executionapi/dto/AutoQaPublicPlan.java` | visão pública do plano |
| `test/.../executionapi/mapper/PlanoNaRespostaTest.java` | 4 testes de mapeamento |
| `test/.../executionapi/service/QueryServiceComPlanoTest.java` | 2 testes de carga do snapshot |

## Arquivos alterados

| arquivo | alteração |
|---|---|
| `executionapi/dto/AutoQaExecutionResponse.java` | novo campo `plan` |
| `executionapi/mapper/AutoQaExecutionResponseMapper.java` | sobrecarga `toResponse(document, snapshot)` |
| `executionapi/service/AutoQaExecutionQueryService.java` | carrega o snapshot no `get` |
| `test/.../AutoQaExecutionQueryServiceTest.java` | novo colaborador no construtor |
| `test/.../controller/AutoQaExecutionControllerTest.java` | novo argumento nas construções |

## Decisões de projeto

**Visão pública curada, não o `TechnicalPlanResult` cru.** Expõe o que sustenta
a decisão de aprovar — o que será criado, por quê, e o que a auditoria
questionou — e deixa de fora o detalhe interno de planejamento.

**A listagem não carrega snapshot.** `toResponse(document)` continua existindo e
passa nulo: buscar um snapshot por item para montar uma lista seria custo sem
uso, já que a lista não mostra plano.

**Nulo antes do planejamento, não objeto vazio.** Um plano vazio e um plano
inexistente são estados diferentes, e o front precisa distingui-los.

## Verificação

Suíte completa: **2158 testes, verde**.

## Próximo passo natural

O front ainda não consome o campo. O painel de aprovação de geração continua
mostrando apenas os botões; exibir as ações planejadas e as advertências é o que
torna a decisão informada de fato.
