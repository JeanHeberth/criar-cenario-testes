# Workflow Rápido - BMAD QA Orchestrator

## Visão Geral
Workflow otimizado para **geração rápida** de cenários de teste, pulando análise de transcrições e revisão de redundâncias.

## Pipeline de Execução

```
1. Requirement Analysis Agent
   ↓
2. Test Plan Agent
   ↓
3. Test Scenario Generator
   ↓
4. Zephyr Formatter
```

## Agentes Executados

### 1️⃣ Requirement Analysis Agent
Extrai requisitos funcionais e não-funcionais da regra de negócio.

### 2️⃣ Test Plan Agent
Cria plano macro de testes baseado nos requisitos.

### 3️⃣ Test Scenario Generator
Gera casos de teste detalhados.

### 4️⃣ Zephyr Formatter
Formata para padrão Zephyr.

---

## Agentes Pulados

❌ **Transcript Analysis Agent** - Economiza tempo quando não há contexto de reuniões
❌ **Redundancy Reviewer** - Aceita possíveis duplicações em troca de velocidade

---

## Quando Usar Este Workflow?

✅ **Use quando:**
- Card simples ou mediano
- Urgência na entrega
- Não há transcrições de reuniões
- Aceita cenários sem otimização de redundâncias

❌ **Não use quando:**
- Projeto crítico
- Card extremamente complexo
- Há contexto importante de reuniões

---

## Tempo Estimado
⏱️ **1-2 minutos** (2-3x mais rápido que workflow completo)

---

## Exemplo de Uso

```java
CenarioRequest request = new CenarioRequest(
    "Validar CPF no cadastro",
    "Sistema deve validar formato e dígitos verificadores do CPF...",
    "gerador_cenarios_testes"
);

CenarioResponse response = qaWorkflowService.executarWorkflow(
    request, 
    WorkflowType.RAPIDO
);
```

---

## Comparação com Workflow Completo

| Aspecto | Completo | Rápido |
|---------|----------|--------|
| Agentes | 6 | 4 |
| Tempo | 3-5 min | 1-2 min |
| Qualidade | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Redundâncias | Removidas | Possíveis |
| Contexto reuniões | Sim | Não |
