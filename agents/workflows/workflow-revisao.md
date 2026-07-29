# Workflow Revisão - BMAD QA Orchestrator

## Visão Geral
Workflow especializado para **revisar cenários existentes**, removendo redundâncias e aplicando formatação Zephyr.

## Pipeline de Execução

```
[Cenários Existentes]
   ↓
1. Redundancy Reviewer
   ↓
2. Zephyr Formatter
```

## Agentes Executados

### 1️⃣ Redundancy Reviewer
**Responsabilidade:** Otimizar cenários existentes

**Entrada:**
- Cenários pré-existentes (fornecidos no contexto)

**Saída:**
- Cenários otimizados
- Redundâncias removidas
- Sugestões de parametrização

---

### 2️⃣ Zephyr Formatter
**Responsabilidade:** Formatar cenários revisados

**Entrada:**
- Cenários otimizados (etapa 1)

**Saída:**
- Formato final Zephyr

---

## Quando Usar Este Workflow?

✅ **Use quando:**
- Já tem cenários prontos mas desorganizados
- Identificou muitas redundâncias manualmente
- Precisa apenas formatar para Zephyr
- Quer consolidar cenários de múltiplas fontes

❌ **Não use quando:**
- Precisa criar cenários do zero
- Não tem cenários pré-existentes

---

## Tempo Estimado
⏱️ **30-60 segundos** (workflow mais rápido)

---

## Exemplo de Uso

```java
// 1. Carregar cenários existentes
Cenario existente = cenarioRepository.findById("123");

// 2. Criar request com cenários para revisão
CenarioRequest request = new CenarioRequest(
    existente.getTitulo(),
    existente.getRegraDeNegocio(),
    "gerador_cenarios_testes"
);

// 3. Executar workflow de revisão
CenarioResponse response = qaWorkflowService.executarWorkflow(
    request, 
    WorkflowType.REVISAO
);
```

---

## Casos de Uso Comuns

### 1. Consolidar Múltiplos CTs
Você tem 20 cenários manualmente criados com redundâncias.

**Antes:**
- CT001: Login com email válido
- CT002: Login com e-mail válido
- CT003: Fazer login com email correto

**Depois:**
- CT001: Login com credenciais válidas [email, senha]

---

### 2. Parametrizar Cenários Similares
Você tem cenários quase idênticos.

**Antes:**
- CT001: CPF com 11 dígitos válidos
- CT002: CPF com 11 dígitos inválidos
- CT003: CPF com formato correto
- CT004: CPF com formato incorreto

**Depois:**
- CT001: Validar CPF [válido/inválido, formato correto/incorreto]

---

## Limitações

⚠️ Este workflow **não gera novos cenários**, apenas otimiza os existentes.

Se precisa criar cenários do zero, use:
- `WorkflowType.COMPLETO`
- `WorkflowType.RAPIDO`
