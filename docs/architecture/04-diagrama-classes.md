# 📦 04 - Diagrama de Classes (UML Simplificado)

**Para quem:** Desenvolvedores, arquitetos que precisam entender a estrutura de classes.

**Tempo de leitura:** 10 minutos

---

## Estrutura Geral

```mermaid
graph TB
    subgraph CONTROLLER["CONTROLLER LAYER"]
        CECON["CenarioController<br/>━━━━━━━━━━━━━<br/>+ POST /cenarios<br/>+ GET /cenarios"]
    end
    
    subgraph SERVICE["SERVICE LAYER"]
        CESVC["CenarioService<br/>━━━━━━━━━━━━━<br/>+ createCenario()<br/>+ validateInput()"]
        
        QASVC["QaWorkflowService<br/>━━━━━━━━━━━━━<br/>+ executeWorkflow()<br/>+ selectPipeline()"]
    end
    
    subgraph WORKFLOW["WORKFLOW ENGINE"]
        WF["WorkflowFactory<br/>━━━━━━━━━━━━━<br/>+ createWorkflow()"]
        
        WFX["WorkflowExecutor<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        CTX["WorkflowContext<br/>━━━━━━━━━━━━━<br/>+ requirements<br/>+ transcript<br/>+ agentResults"]
    end
    
    subgraph AGENTS["AGENTS LAYER"]
        BA["«interface»<br/>BaseAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        RA["RequirementAnalysisAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        TA["TranscriptAnalysisAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        TP["TestPlanAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        TS["TestScenarioAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        RR["RedundancyReviewAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
        
        ZF["ZephyrFormatterAgent<br/>━━━━━━━━━━━━━<br/>+ execute()"]
    end
    
    subgraph REPOSITORY["REPOSITORY LAYER"]
        CERES["CenarioRepository<br/>━━━━━━━━━━━━━<br/>+ save()<br/>+ findById()"]
    end
    
    CECON --> CESVC
    CESVC --> QASVC
    QASVC --> WF
    WF --> WFX
    WFX --> CTX
    WFX --> BA
    BA <|-- RA
    BA <|-- TA
    BA <|-- TP
    BA <|-- TS
    BA <|-- RR
    BA <|-- ZF
    WFX --> CERES
    
    style CONTROLLER fill:#ff6b6b
    style SERVICE fill:#f59f00
    style WORKFLOW fill:#748ffc
    style AGENTS fill:#339af0
    style REPOSITORY fill:#10b981
```

---

## Classes Detalhadas

### Controller Layer

```mermaid
classDiagram
    class CenarioController {
        -cenarioService: CenarioService
        -workflowService: QaWorkflowService
        
        +createCenario(request: CreateCenarioRequest): ResponseEntity
        +getCenarios(): ResponseEntity
        +getCenarioById(id: String): ResponseEntity
        +deleteCenario(id: String): ResponseEntity
    }
    
    class CreateCenarioRequest {
        +requirements: String
        +transcript: String
        +workflowType: WorkflowType
    }
    
    class CenarioResponse {
        +id: String
        +status: String
        +duration: Long
        +agentsExecuted: List
        +finalOutput: Object
    }
    
    CenarioController --> CreateCenarioRequest
    CenarioController --> CenarioResponse
```

### Service Layer

```mermaid
classDiagram
    class CenarioService {
        -repository: CenarioRepository
        -workflowService: QaWorkflowService
        
        +createCenario(request: CreateCenarioRequest): Cenario
        +validateInput(request: CreateCenarioRequest): void
        +getAllCenarios(): List~Cenario~
        +findById(id: String): Cenario
        +deleteCenario(id: String): void
    }
    
    class QaWorkflowService {
        -workflowFactory: WorkflowFactory
        -agentExecutor: AgentExecutor
        
        +executeWorkflow(context: WorkflowContext): WorkflowContext
        +selectPipeline(workflowType: WorkflowType): List~BaseAgent~
        +validateWorkflow(workflowType: WorkflowType): void
    }
    
    CenarioService --> QaWorkflowService
```

### Workflow Engine

```mermaid
classDiagram
    class WorkflowContext {
        -requirements: String
        -transcript: String
        -workflowType: WorkflowType
        -agentResults: Map~String, Object~
        -executionStats: ExecutionStats
        -errors: List~Exception~
        
        +addAgentResult(agentName: String, result: Object): WorkflowContext
        +getAgentResult(agentName: String): Object
        +toImmutable(): WorkflowContext
    }
    
    class WorkflowFactory {
        +createWorkflow(type: WorkflowType): List~BaseAgent~
        +createCompletoWorkflow(): List~BaseAgent~
        +createRapidoWorkflow(): List~BaseAgent~
        +createRevisaoWorkflow(): List~BaseAgent~
        +createRegressaoWorkflow(): List~BaseAgent~
    }
    
    class WorkflowExecutor {
        -agents: List~BaseAgent~
        
        +execute(context: WorkflowContext): WorkflowContext
        -executeAgent(agent: BaseAgent, context: WorkflowContext): WorkflowContext
        -handleError(exception: Exception): void
    }
    
    class ExecutionStats {
        -startTime: Long
        -endTime: Long
        -totalDuration: Long
        -agentDurations: Map
    }
    
    WorkflowContext --> ExecutionStats
    WorkflowFactory --> WorkflowContext
    WorkflowExecutor --> WorkflowContext
```

### Agents Layer

```mermaid
classDiagram
    class BaseAgent {
        <<interface>>
        +getName(): String
        +execute(context: WorkflowContext): WorkflowContext
        +validate(context: WorkflowContext): void
    }
    
    class AbstractAgent {
        <<abstract>>
        -name: String
        
        #getName(): String
        #extractInput(context: WorkflowContext): Object
        #enrichContext(context: WorkflowContext, result: Object): WorkflowContext
    }
    
    class RequirementAnalysisAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -parseRequirements(text: String): Requirements
        -extractFeatures(requirements: Requirements): List
    }
    
    class TranscriptAnalysisAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -parseTranscript(text: String): Transcript
        -extractFlows(transcript: Transcript): List~Flow~
    }
    
    class TestPlanAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -analyzeRequirements(req: Requirements): TestPlan
        -strategyDefinition(flows: List): TestPlan
    }
    
    class TestScenarioAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -generateScenarios(plan: TestPlan): List~TestCase~
        -applyGWT(scenario: Scenario): GivenWhenThen
    }
    
    class RedundancyReviewAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -detectDuplicates(cases: List): List~Duplicate~
        -mergeTestCases(cases: List): List~TestCase~
    }
    
    class ZephyrFormatterAgent {
        +execute(context: WorkflowContext): WorkflowContext
        -formatForZephyr(cases: List): ZephyrJSON
        -validateStructure(json: Object): boolean
    }
    
    BaseAgent <|-- AbstractAgent
    AbstractAgent <|-- RequirementAnalysisAgent
    AbstractAgent <|-- TranscriptAnalysisAgent
    AbstractAgent <|-- TestPlanAgent
    AbstractAgent <|-- TestScenarioAgent
    AbstractAgent <|-- RedundancyReviewAgent
    AbstractAgent <|-- ZephyrFormatterAgent
```

### Data Layer

```mermaid
classDiagram
    class CenarioRepository {
        <<interface>>
        +save(cenario: Cenario): Cenario
        +findById(id: String): Cenario
        +findAll(): List~Cenario~
        +delete(id: String): void
    }
    
    class CenarioRepositoryImpl {
        -mongoTemplate: MongoTemplate
        
        +save(cenario: Cenario): Cenario
        +findById(id: String): Cenario
        +findAll(): List~Cenario~
        +delete(id: String): void
    }
    
    class Cenario {
        -id: String
        -requirements: String
        -transcript: String
        -workflowType: WorkflowType
        -status: String
        -createdAt: LocalDateTime
        -result: CenarioResult
    }
    
    class CenarioResult {
        -testCases: List~TestCase~
        -formattedOutput: String
        -executionStats: ExecutionStats
    }
    
    class TestCase {
        -id: String
        -title: String
        -given: String
        -when: String
        -then: String
        -testData: Object
    }
    
    CenarioRepository <|-- CenarioRepositoryImpl
    CenarioRepositoryImpl --> Cenario
    Cenario --> CenarioResult
    CenarioResult --> TestCase
```

---

## Padrões Usados

### 1. Factory Pattern (WorkflowFactory)

```mermaid
sequenceDiagram
    participant Client
    participant Factory as WorkflowFactory
    participant Pipeline as List~BaseAgent~
    
    Client->>Factory: createWorkflow("COMPLETO")
    Factory->>Factory: selectType("COMPLETO")
    Factory->>Pipeline: create agents list
    Pipeline-->>Factory: [RA, TA, TP, TS, RR, ZF]
    Factory-->>Client: agents
```

### 2. Strategy Pattern (Workflows)

```
Cada workflow é uma "strategy" diferente:
- COMPLETO: Full analysis (6 agents)
- RAPIDO: Quick analysis (4 agents)
- REVISAO: Review only (2 agents)
- REGRESSAO: Regression focus (4 agents)
```

### 3. Template Method (BaseAgent)

```java
// Padrão em BaseAgent e AbstractAgent
abstract class AbstractAgent implements BaseAgent {
    final WorkflowContext execute(WorkflowContext ctx) {
        Object input = extractInput(ctx);        // Template method
        Object result = doExecute(input);        // Implementado por subclasses
        return enrichContext(ctx, result);       // Template method
    }
    
    abstract Object doExecute(Object input);
}
```

### 4. Immutable Value Object (WorkflowContext)

```
WorkflowContext é imutável:
- Cada operação retorna uma nova instância
- Preserva histórico de execução
- Thread-safe naturalmente
```

---

## Hierarquia de Classe

```mermaid
graph TB
    Object["Object (Java)"]
    
    Object --> BA["BaseAgent (Interface)"]
    Object --> AA["AbstractAgent"]
    
    AA --> RA["RequirementAnalysisAgent"]
    AA --> TA["TranscriptAnalysisAgent"]
    AA --> TP["TestPlanAgent"]
    AA --> TS["TestScenarioAgent"]
    AA --> RR["RedundancyReviewAgent"]
    AA --> ZF["ZephyrFormatterAgent"]
    
    Object --> WC["WorkflowContext"]
    Object --> Cenario["Cenario (Entity)"]
    Object --> TestCase["TestCase"]
    
    style BA fill:#0ea5e9
    style AA fill:#0ea5e9
    style RA fill:#339af0
    style TA fill:#339af0
    style TP fill:#339af0
    style TS fill:#339af0
    style RR fill:#339af0
    style ZF fill:#339af0
```

---

## Fluxo de Execução com Detalhes de Classe

```mermaid
sequenceDiagram
    participant Controller as CenarioController
    participant Service as CenarioService
    participant QA as QaWorkflowService
    participant Factory as WorkflowFactory
    participant Executor as WorkflowExecutor
    participant Agent1 as BaseAgent 1
    participant Agent2 as BaseAgent N
    participant Repo as CenarioRepository
    
    Controller->>Service: createCenario(request)
    Service->>Service: validateInput()
    Service->>QA: executeWorkflow()
    QA->>Factory: createWorkflow(COMPLETO)
    Factory-->>QA: [Agent1, Agent2, ...]
    QA->>Executor: execute(context, agents)
    
    loop Para cada agente
        Executor->>Agent1: execute(context)
        Agent1-->>Executor: enrichedContext
        Executor->>Agent2: execute(enrichedContext)
        Agent2-->>Executor: enrichedContext
    end
    
    Executor-->>QA: finalContext
    QA-->>Service: result
    Service->>Repo: save(cenario)
    Repo-->>Service: saved
    Service-->>Controller: response
    Controller-->>Client: 201 Created
```

---

## Dependências Entre Componentes

```mermaid
graph LR
    Controller["CenarioController"]
    Service["CenarioService"]
    WFService["QaWorkflowService"]
    Factory["WorkflowFactory"]
    Executor["WorkflowExecutor"]
    BaseAgent["BaseAgent"]
    Repository["CenarioRepository"]
    
    Controller -->|depends on| Service
    Service -->|depends on| WFService
    WFService -->|depends on| Factory
    WFService -->|depends on| Executor
    Factory -->|creates| BaseAgent
    Executor -->|executes| BaseAgent
    Service -->|uses| Repository
    
    style Controller fill:#ff6b6b
    style Service fill:#f59f00
    style WFService fill:#748ffc
    style Factory fill:#339af0
    style Executor fill:#339af0
    style BaseAgent fill:#339af0
    style Repository fill:#10b981
```

---

## Checklist de Implementação de Novo Agente

- [ ] Criar classe que estende `AbstractAgent`
- [ ] Implementar método `doExecute()`
- [ ] Implementar validação no `validate()`
- [ ] Adicionar testes unitários
- [ ] Registrar no `WorkflowFactory`
- [ ] Adicionar ao workflow desejado
- [ ] Testar integração
- [ ] Documentar comportamento

---

**[⬅️ Voltar](03-pipeline-bmad.md)** | **[Próximo → Sequência de Requisição →](05-sequencia-requisicao.md)**
