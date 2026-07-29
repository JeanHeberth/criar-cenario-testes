# 🔄 02 - Fluxo de Execução

**Para quem:** Desenvolvedores, QAs, qualquer um querendo entender como uma requisição é processada.

**Tempo de leitura:** 5 minutos

---

## Fluxo Passo a Passo

```mermaid
sequenceDiagram
    participant User as 👤 Usuário
    participant Frontend as 🎨 Frontend<br/>Angular
    participant Controller as 🎯 Controller
    participant Service as 💼 Service
    participant WorkflowService as 🔄 WorkflowService
    participant Agents as 🤖 Agentes
    participant Repository as 📦 Repository
    participant MongoDB as 💾 MongoDB
    
    User->>Frontend: 1. Preenche formulário<br/>(requirements, workflow)
    Frontend->>Controller: 2. POST /api/cenarios
    Controller->>Service: 3. Valida & parseia request
    Service->>WorkflowService: 4. Cria contexto de execução
    WorkflowService->>Agents: 5. Orquestra agentes<br/>sequencialmente
    
    loop Para cada agente no workflow
        Agents->>Agents: ├─ RequirementAnalysis
        Agents->>Agents: ├─ TranscriptAnalysis
        Agents->>Agents: ├─ TestPlan
        Agents->>Agents: ├─ TestScenario
        Agents->>Agents: ├─ RedundancyReview
        Agents->>Agents: └─ ZephyrFormatter
    end
    
    Agents->>Repository: 6. Persiste resultado
    Repository->>MongoDB: 7. Insere/Atualiza documento
    MongoDB-->>Repository: 8. Retorna ID
    Repository-->>WorkflowService: 9. Retorna entidade salva
    WorkflowService-->>Service: 10. Retorna contexto completo
    Service-->>Controller: 11. Prepara response DTO
    Controller-->>Frontend: 12. Status 201 + JSON
    Frontend-->>User: 13. Exibe resultados na tela
```

---

## Estados da Execução

```mermaid
stateDiagram-v2
    [*] --> PENDING: User submit
    
    PENDING --> VALIDATING: Request chega
    VALIDATING --> INVALID: Erro de validação
    INVALID --> [*]: Retorna 400
    
    VALIDATING --> PROCESSING: Cria contexto
    
    PROCESSING --> REQUIREMENT_ANALYSIS: Inicia workflow
    REQUIREMENT_ANALYSIS --> TRANSCRIPT_ANALYSIS: Sucesso
    TRANSCRIPT_ANALYSIS --> TEST_PLAN: Sucesso
    TEST_PLAN --> TEST_SCENARIO: Sucesso
    TEST_SCENARIO --> REVIEW: Sucesso
    REVIEW --> FORMATTER: Sucesso
    
    REQUIREMENT_ANALYSIS --> FAILED: Erro
    TRANSCRIPT_ANALYSIS --> FAILED: Erro
    TEST_PLAN --> FAILED: Erro
    TEST_SCENARIO --> FAILED: Erro
    REVIEW --> FAILED: Erro
    FORMATTER --> FAILED: Erro
    
    FORMATTER --> PERSISTING: Sucesso
    PERSISTING --> COMPLETED: Salvo com sucesso
    FAILED --> [*]: Retorna 500
    COMPLETED --> [*]: Retorna 201
    
    style COMPLETED fill:#51cf66
    style FAILED fill:#ff6b6b
    style PROCESSING fill:#748ffc
```

---

## Exemplo: Workflow COMPLETO

### 1️⃣ Request

```bash
curl -X POST http://localhost:8080/api/cenarios \
  -H "Content-Type: application/json" \
  -d '{
    "requirements": "Validar login com email ou CPF",
    "transcript": "Sistema de autenticação com dois caminhos: email ou CPF",
    "workflowType": "COMPLETO"
  }'
```

### 2️⃣ Response

```json
{
  "id": "67890abc...",
  "status": "COMPLETED",
  "duration": "45s",
  "workflow": "COMPLETO",
  "agentsExecuted": [
    {
      "name": "RequirementAnalysis",
      "status": "COMPLETED",
      "duration": 8,
      "output": {...}
    },
    {
      "name": "TranscriptAnalysis",
      "status": "COMPLETED",
      "duration": 6,
      "output": {...}
    },
    {
      "name": "TestPlan",
      "status": "COMPLETED",
      "duration": 12,
      "output": {...}
    },
    {
      "name": "TestScenario",
      "status": "COMPLETED",
      "duration": 14,
      "output": {...}
    },
    {
      "name": "RedundancyReview",
      "status": "COMPLETED",
      "duration": 3,
      "output": {...}
    },
    {
      "name": "ZephyrFormatter",
      "status": "COMPLETED",
      "duration": 2,
      "output": {...}
    }
  ],
  "finalOutput": {
    "testCases": [...],
    "formattedForZephyr": {...}
  }
}
```

---

## Tempo de Execução por Workflow

```mermaid
gantt
    title Tempo de Execução por Workflow
    dateFormat YYYY-MM-DD HH:mm:ss
    
    section COMPLETO (45s)
    RequirementAnalysis :a1, 2024-01-01 00:00:00, 8s
    TranscriptAnalysis :a2, after a1, 6s
    TestPlan :a3, after a2, 12s
    TestScenario :a4, after a3, 14s
    RedundancyReview :a5, after a4, 3s
    ZephyrFormatter :a6, after a5, 2s
    
    section RAPIDO (22s)
    RequirementAnalysis :b1, 2024-01-01 00:01:00, 8s
    TranscriptAnalysis :b2, after b1, 6s
    TestPlan :b3, after b2, 5s
    ZephyrFormatter :b4, after b3, 3s
    
    section REVISAO (5s)
    RedundancyReview :c1, 2024-01-01 00:02:00, 3s
    ZephyrFormatter :c2, after c1, 2s
    
    section REGRESSAO (30s)
    RequirementAnalysis :d1, 2024-01-01 00:03:00, 8s
    TranscriptAnalysis :d2, after d1, 6s
    TestScenario :d3, after d2, 12s
    ZephyrFormatter :d4, after d3, 4s
```

---

## Fluxo de Dados por Camada

```mermaid
graph TB
    subgraph CLIENT["CLIENT SIDE"]
        F1["Form Input"]
        F2["Validation"]
        F3["HTTP Request"]
    end
    
    subgraph CONTROLLER["CONTROLLER"]
        C1["@PostMapping"]
        C2["Parse DTO"]
        C3["Basic Validation"]
    end
    
    subgraph SERVICE["SERVICE"]
        S1["Advanced Validation"]
        S2["Business Logic"]
        S3["Prepare Context"]
    end
    
    subgraph WORKFLOW["WORKFLOW ENGINE"]
        W1["Create WorkflowContext"]
        W2["Select Pipeline"]
        W3["Initialize Agents"]
    end
    
    subgraph AGENTS["AGENTS PIPELINE"]
        A1["Agent 1: Analysis"]
        A2["Agent 2: Processing"]
        A3["Agent 3: Generation"]
        A4["Agent N: Output"]
    end
    
    subgraph REPOSITORY["REPOSITORY"]
        R1["Format Entity"]
        R2["Pre-save Hook"]
    end
    
    subgraph DATA["DATABASE"]
        D1["MongoDB"]
    end
    
    F1 --> F2 --> F3 --> C1
    C1 --> C2 --> C3 --> S1
    S1 --> S2 --> S3 --> W1
    W1 --> W2 --> W3 --> A1
    A1 --> A2 --> A3 --> A4
    A4 --> R1 --> R2 --> D1
    
    style CLIENT fill:#ff6b6b
    style CONTROLLER fill:#f59f00
    style SERVICE fill:#748ffc
    style WORKFLOW fill:#339af0
    style AGENTS fill:#0ea5e9
    style REPOSITORY fill:#10b981
    style DATA fill:#228b22
```

---

## Tratamento de Erros

```mermaid
graph TD
    Request["📨 Requisição"]
    
    Request -->|Inválida| E1["❌ ValidationException"]
    E1 --> E1R["Response 400<br/>Bad Request"]
    
    Request -->|Válida| E2{"Agente<br/>Sucesso?"}
    E2 -->|❌ Não| E3["❌ AgentExecutionException"]
    E3 --> E3R["Response 500<br/>Agent Failed"]
    
    E2 -->|✅ Sim| E4{"BD<br/>Sucesso?"}
    E4 -->|❌ Não| E5["❌ DataException"]
    E5 --> E5R["Response 500<br/>Database Error"]
    
    E4 -->|✅ Sim| E6["✅ Success"]
    E6 --> E6R["Response 201<br/>Created"]
    
    E1R --> L1["Log Error"]
    E3R --> L2["Log Stack Trace"]
    E5R --> L3["Log DB Error"]
    E6R --> L4["Log Success"]
    
    style E1R fill:#ff6b6b
    style E3R fill:#ff6b6b
    style E5R fill:#ff6b6b
    style E6R fill:#51cf66
```

---

## Comparação de Workflows

| Workflow | Agentes | Tempo | Uso |
|----------|---------|-------|-----|
| **COMPLETO** | 6 | ~45s | Análise completa de requisitos com revisão |
| **RAPIDO** | 4 | ~22s | Geração rápida focada em testes |
| **REVISAO** | 2 | ~5s | Revisão de testes existentes |
| **REGRESSAO** | 4 | ~30s | Testes de regressão |

---

## Monitoramento

```mermaid
graph LR
    WF["Workflow<br/>Executa"]
    
    WF --> M1["📊 Métricas"]
    WF --> M2["📝 Logs"]
    WF --> M3["⚠️ Alertas"]
    
    M1 --> D1["Duration"]
    M1 --> D2["Agent Count"]
    M1 --> D3["Success Rate"]
    
    M2 --> L1["INFO: Workflow started"]
    M2 --> L2["DEBUG: Agent output"]
    M2 --> L3["ERROR: Exceptions"]
    
    M3 --> A1["Timeout Alert"]
    M3 --> A2["Failure Alert"]
    M3 --> A3["Performance Alert"]
    
    style M1 fill:#51cf66
    style M2 fill:#748ffc
    style M3 fill:#ff6b6b
```

---

## Checklist de Validação

- ✅ Requisição tem campos obrigatórios
- ✅ Workflow type é válido
- ✅ Agentes executam em sequência
- ✅ Contexto é passado entre agentes
- ✅ Resultado é persistido
- ✅ Response tem status correto
- ✅ Erros são loguados

---

**[⬅️ Voltar](01-arquitetura-geral.md)** | **[Próximo → Pipeline BMAD →](03-pipeline-bmad.md)**
