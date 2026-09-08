# Mensagem não especificada e log de veredito BLOCKED

**Data:** 2026-08-25
**Método:** SDD → TDD

---

## Motivação

Execução real produziu código que compila e passa em 10 de 22 testes. Das 12
falhas, **6 vieram de mensagens de erro que o modelo inventou**:

```
Esperado: "E-mail deve ser um endereço de e-mail válido"
Recebido: "E-mail deve ter formato válido"
```

O contrato especificava a estrutura da resposta 400, mas não o texto da mensagem
de validação de formato. O modelo preencheu com algo plausível.

A regra `ASSERCAO_SOBRE_COMPORTAMENTO_EXPLORATORIO` não pegou porque verifica
apenas status HTTP — lacuna da regra escrita anteriormente.

E o veredito `BLOCKED` da mesma execução ficou sem rastro: o `ReviewAgent`
registrava os achados em `CHANGES_REQUIRED` e não em `BLOCKED`, o mais grave dos
dois.

---

## Parte 1 — `MENSAGEM_NAO_ESPECIFICADA`

### Spec

Acusar string literal, de várias palavras, afirmada como expectativa sobre a API
e ausente do texto do cenário.

**Posições lidas** — as duas em que a string é inequivocamente expectativa:

| posição | exemplo |
|---|---|
| propriedade com prefixo `expected` | `expectedMessages: ['...']` |
| argumento de matcher | `.toBe('...')`, `.toEqual`, `.toContain` |

**Fora do escopo, de propósito:** título de teste e mensagem de asserção são
texto do autor, não afirmação sobre o sistema. Acusá-los encheria o relatório de
ruído.

**Filtro de várias palavras:** token único (`Bearer`, `CREATE`) é valor de
contrato ou enum.

### Severidade e por que fica fora do laço

`HIGH` — bloqueia o apply e aparece no relatório. Mas **deliberadamente fora da
lista de erros acionáveis**: regerar não ajuda quando o contrato não define a
mensagem, o modelo apenas chutaria outra. A decisão é humana — afrouxar a
asserção ou acrescentar a mensagem ao contrato.

### Verificação contra o arquivo real

```
Teste afirma mensagem que o contrato não define:
  E-mail deve ser um endereço de e-mail válido | JSON parse error
```

---

## Parte 2 — Log do veredito BLOCKED

`ReviewAgent` passa a registrar os achados também quando o veredito é `BLOCKED`.
Antes, descobrir qual regra bloqueou exigia reproduzir a execução inteira.

---

## Arquivos alterados

| arquivo | alteração |
|---|---|
| `model/review/ReviewRule.java` | novo código `MENSAGEM_NAO_ESPECIFICADA` |
| `review/StaticReviewRuleEngine.java` | regra e sobrecarga com o texto do cenário |
| `review/CodeReviewService.java` | repassa o texto do cenário ao engine |
| `agent/ReviewAgent.java` | log de achados em `BLOCKED` |
| `test/.../review/RegrasDeTesteFragilTest.java` | 3 testes novos |

## Verificação

Suíte completa: **2162 testes, verde**.

## Limitação conhecida

A regra lê duas posições sintáticas. Uma mensagem inventada fora delas — montada
por concatenação, por exemplo — passa. Cobrir todo literal do arquivo traria
falsos positivos (mensagens de `throw` legítimas), e a troca não compensa.
