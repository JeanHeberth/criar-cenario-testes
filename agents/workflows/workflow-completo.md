# Workflow Completo - BMAD QA Orchestrator

## Visão Geral
Este é o workflow completo que executa **todos os 6 agentes** especializados para máxima qualidade e cobertura de testes.

## Pipeline de Execução

```
1. Requirement Analysis Agent
   ↓
2. Transcript Analysis Agent
   ↓
3. Test Plan Agent
   ↓
4. Test Scenario Generator
   ↓
5. Redundancy Reviewer
   ↓
6. Zephyr Formatter
```

## Agentes Executados

### 1️⃣ Requirement Analysis Agent
**Responsabilidade:** Extrair requisitos funcionais e não-funcionais

**Entrada:**
- Título do card
- Regra de negócio

**Saída:**
- Requisitos Funcionais (RF)
- Requisitos Não-Funcionais (RNF)
- Regras de Negócio (RN)
- Pontos de Atenção

---

### 2️⃣ Transcript Analysis Agent
**Responsabilidade:** Analisar transcrições de reuniões e identificar decisões/ambiguidades

**Entrada:**
- Requisitos extraídos (etapa 1)
- Regra de negócio original

**Saída:**
- Decisões tomadas pela equipe
- Ambiguidades que precisam esclarecimento
- Premissas assumidas
- Pontos não discutidos

---

### 3️⃣ Test Plan Agent
**Responsabilidade:** Criar plano macro de testes

**Entrada:**
- Requisitos (etapa 1)
- Decisões e ambiguidades (etapa 2)

**Saída:**
- Estratégia de cobertura
- Cenários principais identificados (positivos, negativos, edge cases)
- Tipos de teste sugeridos
- Priorização por risco

---

### 4️⃣ Test Scenario Generator
**Responsabilidade:** Gerar casos de teste detalhados

**Entrada:**
- Requisitos (etapa 1)
- Decisões (etapa 2)
- Plano de testes (etapa 3)
- Instruções do agente customizado (se houver)

**Saída:**
- Lista completa de casos de teste com:
  - Nome
  - Objetivo
  - Pré-condições
  - Passos
  - Resultado esperado

---

### 5️⃣ Redundancy Reviewer
**Responsabilidade:** Revisar e otimizar cenários

**Entrada:**
- Cenários gerados (etapa 4)

**Saída:**
- Cenários otimizados sem redundâncias
- Sugestões de parametrização
- Cenários consolidados

---

### 6️⃣ Zephyr Formatter
**Responsabilidade:** Formatar para padrão Zephyr

**Entrada:**
- Cenários revisados (etapa 5)

**Saída:**
- Casos de teste formatados no padrão Zephyr
- Numeração CT001, CT002, etc.
- Markdown estruturado

---

## Quando Usar Este Workflow?

✅ **Use quando:**
- Projeto crítico que exige máxima cobertura
- Card complexo com múltiplos requisitos
- Há transcrições de reuniões disponíveis
- Tempo não é crítico (workflow mais lento)

❌ **Não use quando:**
- Card simples com poucos cenários
- Urgência na entrega
- Não há contexto adicional de reuniões

---

## Tempo Estimado
⏱️ **3-5 minutos** (depende da complexidade e dos providers de IA)

---

## Exemplo de Uso

```java
CenarioRequest request = new CenarioRequest(
    "Implementar login com OAuth",
    "Usuário deve poder fazer login usando Google, Facebook ou Microsoft...",
    "gerador_cenarios_testes"
);

CenarioResponse response = qaWorkflowService.executarWorkflow(
    request, 
    WorkflowType.COMPLETO
);
```

---

## Fallback
Se qualquer agente falhar criticamente, o sistema:
1. Registra o erro no log
2. Aplica fallback local (cenários pré-definidos)
3. Retorna resposta mesmo com falha parcial
