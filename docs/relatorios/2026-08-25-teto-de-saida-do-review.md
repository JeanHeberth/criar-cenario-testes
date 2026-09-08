# Teto de saída dedicado para a revisão de código

**Data:** 2026-08-25
**Método:** SDD → TDD

---

## Sintoma

```
Gemini truncou a resposta pelo limite de tokens de saída
(finishReason='MAX_TOKENS'). maxOutputTokens=8000, responseLength=29458
Review failed after fallback. primaryProvider=openai, fallbackProvider=gemini
```

Revisão de 5 arquivos truncou no meio do JSON e falhou nos dois provedores —
com as duas chamadas já pagas.

## Diagnóstico

A saída do review **cresce com o número de arquivos revisados**: cada um traz
seus achados, sugestões e regras avaliadas. O teto de 8000 tokens equivale a
cerca de 29 mil caracteres, e a revisão passou disso.

Teto dimensionado para uma resposta pequena reprova justamente as execuções
maiores — que são as que mais precisam de revisão.

A geração já tinha teto próprio de 16.000 desde a correção anterior; o review
ficou no default e o problema reapareceu no estágio seguinte.

## Correção

`MAX_TOKENS_REVIEW = 16_000` em `CodeReviewService`, passado explicitamente ao
provedor — mesmo tratamento que a geração recebeu.

## Arquivos criados

| arquivo | conteúdo |
|---|---|
| `test/.../review/LimiteDeSaidaDoReviewTest.java` | garante teto acima de 8000 |

## Arquivos alterados

| arquivo | alteração |
|---|---|
| `business/autoqa/review/CodeReviewService.java` | constante e chamada com teto explícito |
| `test/.../review/CodeReviewServiceTest.java` | mocks e verify com o novo parâmetro |

## Verificação

Suíte completa: **2159 testes, verde**.

## Observação sobre o padrão

É o terceiro estágio a precisar de teto próprio (geração, análise de cenário,
agora revisão). O default de 8000 serve para respostas curtas; qualquer estágio
que emita conteúdo proporcional ao tamanho do projeto vai estourá-lo.

Vale considerar dimensionar o teto pelo volume de entrada em vez de fixar por
estágio — mas isso é mudança de desenho, não correção, e fica registrado como
observação.
