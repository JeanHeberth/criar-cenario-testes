# 🏛️ 01 - Arquitetura Geral

**Para quem:** Executivos, PMs, qualquer pessoa que quer entender o projeto em 30 segundos.

**Tempo de leitura:** 2 minutos

---

## Stack Tecnológico

```mermaid
graph TD
    A["👨‍💻 Frontend<br/>Angular 17"]
    B["🌐 REST API<br/>Spring Boot 3"]
    C["🤖 IA & Agentes<br/>BMAD Pipeline"]
    D["💾 Persistência<br/>MongoDB"]
    
    A -->|HTTP/REST| B
    B -->|Orquestra| C
    C -->|Salva| D
    D -->|Retorna| C
    C -->|Resposta| B
    B -->|JSON| A
    
    style A fill:#dd4e4e
    style B fill:#68a063
    style C fill:#4169e1
    style D fill:#228b22
```

---

## Camadas da Aplicação

```mermaid
graph LR
    UI["🎨 Presentation<br/>Angular UI<br/>Interface do usuário"]
    
    REST["📡 API Layer<br/>Spring Boot REST<br/>Controllers & DTOs"]
    
    BIZ["💼 Business Logic<br/>Services & Workflows<br/>Lógica de negócio"]
    
    AGENTS["🤖 AI Agents Layer<br/>BMAD Pipeline<br/>Análise & Geração"]
    
    DATA["💾 Data Layer<br/>MongoDB<br/>Persistência"]
    
    UI <-->|JSON/REST| REST
    REST <-->|Objects| BIZ
    BIZ <-->|Context| AGENTS
    AGENTS <-->|Queries| DATA
    
    style UI fill:#ff6b6b
    style REST fill:#51cf66
    style BIZ fill:#748ffc
    style AGENTS fill:#339af0
    style DATA fill:#15aabf
```

---

## Componentes Principais

| Componente | Tecnologia | Responsabilidade |
|-----------|-----------|-----------------|
| **Frontend** | Angular 17 | Interface gráfica, formulários, visualização de resultados |
| **Backend API** | Spring Boot 3 | REST API, validação, orquestração |
| **Workflow Engine** | Java Service | Orquestra múltiplos agentes em sequência |
| **IA Agents** | BMAD Pipeline | Análise de requisitos, transcrição, planos, cenários |
| **Banco de Dados** | MongoDB | Persistência de cenários, resultados, histórico |
| **Message Queue** | Kafka *(futuro)* | Assincronismo e escalabilidade |

---

## Fluxo de Dados - 30 Segundos

```mermaid
sequenceDiagram
    participant User as 👤 Usuário
    participant UI as 🎨 Angular
    participant API as 📡 Spring Boot
    participant WF as 🔄 Workflow
    participant Agents as 🤖 Agentes
    participant DB as 💾 MongoDB
    
    User->>UI: Preenche formulário
    UI->>API: POST /cenario
    API->>WF: Executa workflow
    WF->>Agents: Orquestra agentes
    Agents->>DB: Salva resultados
    DB-->>Agents: Confirma
    Agents-->>WF: Retorna contexto
    WF-->>API: Response
    API-->>UI: JSON
    UI-->>User: Exibe resultados
```

---

## Infraestrutura

```mermaid
graph TB
    subgraph Client
        Browser["🌐 Browser<br/>Chrome/Firefox/Safari"]
    end
    
    subgraph Cloud
        LB["⚙️ Load Balancer<br/>Port 8080"]
        API["🖥️ Application Server<br/>Spring Boot"]
        CACHE["⚡ Cache<br/>Redis (futuro)"]
        QUEUE["📨 Message Queue<br/>Kafka (futuro)"]
    end
    
    subgraph Database
        MONGO["💾 MongoDB<br/>Primary Replica Set"]
    end
    
    subgraph External
        JIRA["🔗 Jira API<br/>Integração"]
        LLM["🤖 LLM API<br/>IA External"]
    end
    
    Browser -->|HTTPS:8080| LB
    LB -->|forward| API
    API -->|cache| CACHE
    API -->|queue| QUEUE
    API -->|query| MONGO
    API -.->|fetch issues| JIRA
    API -.->|call LLM| LLM
    
    style Client fill:#ff6b6b
    style Cloud fill:#51cf66
    style Database fill:#228b22
    style External fill:#748ffc
```

---

## Padrões Arquiteturais

### 1. **MVC Pattern** (Frontend)
- **Model:** Estado Angular (RxJS)
- **View:** Componentes HTML/CSS
- **Controller:** Services Angular

### 2. **Layered Architecture** (Backend)
- **Presentation Layer:** Controllers
- **Business Layer:** Services
- **Data Layer:** Repositories

### 3. **Pipeline Pattern** (Agentes)
- Múltiplos agentes executam em sequência
- Cada agente recebe contexto e enriquece
- Resultado final é composto de saídas de todos os agentes

### 4. **Repository Pattern** (Dados)
- Abstração de acesso a dados
- Facilita testes e mudança de BD

---

## Características Principais

✅ **Multi-Tenant Ready** - Suporta múltiplos usuários/empresas  
✅ **Escalável** - Design preparado para crescimento  
✅ **Testável** - Separação de responsabilidades  
✅ **Resiliente** - Tratamento de erros em múltiplas camadas  
✅ **Documentado** - Código comentado e documentação externa  
✅ **CI/CD Ready** - Dockerizado, pronto para pipeline  

---

## Próximas Evoluções

🔮 **Message Queue** → Processamento assincronismo  
🔮 **Cache Layer** → Melhor performance  
🔮 **Microserviços** → Escalabilidade horizontal  
🔮 **API Gateway** → Gerenciamento de APIs  
🔮 **Observability** → Logs, métricas, tracing  

---

**[⬅️ Voltar ao Índice](README.md)** | **[Próximo → Fluxo de Execução](02-fluxo-execucao.md)**
