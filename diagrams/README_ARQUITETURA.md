```mermaid
graph TB
    subgraph FRONTEND["🖥️ FRONTEND ANGULAR - http://localhost:4200"]
        FORM["📝 Formulário<br/>Título<br/>Regra de Negócio<br/>Agente<br/>⭐ Workflow Type"]
        DROPDOWN["🔽 Dropdown<br/>• COMPLETO 6️⃣<br/>• RAPIDO 4️⃣<br/>• REVISAO 2️⃣<br/>• REGRESSAO 4️⃣"]
        UPLOAD["📎 Upload PDFs<br/>Drag & Drop<br/>Integração Jira"]
        BUTTON["🚀 Gerar Cenário<br/>POST /cenario"]
        VIEW["👀 Visualizar<br/>Lista de Cenários<br/>Exportar"]
    end
    
    subgraph BACKEND["⚙️ BACKEND SPRING BOOT - http://localhost:8080"]
        CONTROLLER["🎯 CenarioController<br/>POST /cenario<br/>GET /cenario/workflows<br/>GET /cenario"]
        SERVICE["🔄 CenarioService<br/>gerarCenarioCompleto()<br/>delega para ↓"]
        WORKFLOW_SERVICE["🎼 QaWorkflowService<br/>ORQUESTRADOR<br/>executarWorkflow()<br/>montarPipelineAgentes()"]
    end
    
    subgraph WORKFLOWS["🔀 ORQUESTRAÇÃO DE WORKFLOWS"]
        W_COMPLETO["🐌 COMPLETO<br/>6 agentes<br/>~2 min"]
        W_RAPIDO["🐇 RAPIDO<br/>4 agentes<br/>~1 min"]
        W_REVISAO["🔍 REVISAO<br/>2 agentes<br/>~30s"]
        W_REGRESSAO["🔄 REGRESSAO<br/>4 agentes<br/>~1 min"]
    end
    
    subgraph AGENTS["🤖 PIPELINE DE AGENTES (Exemplo: COMPLETO)"]
        AGENT1["1️⃣ RequirementAnalysisAgent<br/>Analisa requisitos"]
        AGENT2["2️⃣ TranscriptAnalysisAgent<br/>Extrai decisões"]
        AGENT3["3️⃣ TestPlanAgent<br/>Cria plano macro"]
        AGENT4["4️⃣ TestScenarioAgent<br/>Gera cenários"]
        AGENT5["5️⃣ RedundancyReviewAgent<br/>Remove duplicatas"]
        AGENT6["6️⃣ ZephyrFormatterAgent<br/>Formata campos"]
        CONTEXT["📦 WorkflowContext<br/>requisitos → decisoes → plano → cenarios"]
    end
    
    DB[("💾 MongoDB<br/>Cenário salvo<br/>id, titulo,<br/>regraDeNegocio,<br/>cenarios[]")]
    
    BUTTON -->|"HTTP POST<br/>{workflowType: ...}"| CONTROLLER
    CONTROLLER --> SERVICE
    SERVICE --> WORKFLOW_SERVICE
    WORKFLOW_SERVICE --> W_COMPLETO
    WORKFLOW_SERVICE --> W_RAPIDO
    WORKFLOW_SERVICE --> W_REVISAO
    WORKFLOW_SERVICE --> W_REGRESSAO
    
    W_COMPLETO --> AGENT1
    AGENT1 --> AGENT2
    AGENT2 --> AGENT3
    AGENT3 --> AGENT4
    AGENT4 --> AGENT5
    AGENT5 --> AGENT6
    
    AGENT1 -.-> CONTEXT
    AGENT2 -.-> CONTEXT
    AGENT3 -.-> CONTEXT
    AGENT4 -.-> CONTEXT
    AGENT5 -.-> CONTEXT
    AGENT6 -.-> CONTEXT
    
    AGENT6 --> DB
    DB -->|"HTTP 200 OK<br/>CenarioResponse"| VIEW
    
    style FORM fill:#fff2cc,stroke:#d6b656
    style DROPDOWN fill:#d5e8d4,stroke:#82b366
    style UPLOAD fill:#e1d5e7,stroke:#9673a6
    style BUTTON fill:#f8cecc,stroke:#b85450,stroke-width:3px
    style VIEW fill:#dae8fc,stroke:#6c8ebf
    
    style CONTROLLER fill:#fff2cc,stroke:#d6b656
    style SERVICE fill:#d5e8d4,stroke:#82b366
    style WORKFLOW_SERVICE fill:#e1d5e7,stroke:#9673a6,stroke-width:3px
    
    style W_COMPLETO fill:#dae8fc,stroke:#6c8ebf
    style W_RAPIDO fill:#d5e8d4,stroke:#82b366
    style W_REVISAO fill:#fff2cc,stroke:#d6b656
    style W_REGRESSAO fill:#e1d5e7,stroke:#9673a6
    
    style AGENT1 fill:#dae8fc,stroke:#6c8ebf
    style AGENT2 fill:#dae8fc,stroke:#6c8ebf
    style AGENT3 fill:#dae8fc,stroke:#6c8ebf
    style AGENT4 fill:#dae8fc,stroke:#6c8ebf
    style AGENT5 fill:#dae8fc,stroke:#6c8ebf
    style AGENT6 fill:#dae8fc,stroke:#6c8ebf
    style CONTEXT fill:#fff2cc,stroke:#d6b656
    
    style DB fill:#f8cecc,stroke:#b85450,stroke-width:3px
```

## 📊 **Como usar este diagrama:**

### **Opção 1: Visualizar online** ⭐
1. Copie todo o código acima (incluindo ```mermaid)
2. Acesse: https://mermaid.live/
3. Cole o código
4. Visualize o diagrama interativo
5. Exporte como PNG/SVG/PDF

### **Opção 2: GitHub/GitLab**
- Este código Mermaid é renderizado automaticamente no GitHub/GitLab
- Basta criar um README.md com este conteúdo

### **Opção 3: VS Code**
1. Instale extensão "Markdown Preview Mermaid Support"
2. Abra este arquivo
3. Pressione Ctrl+Shift+V (preview)

### **Opção 4: Converter para Draw.io**
1. Acesse: https://mermaid.live/
2. Cole o código acima
3. Clique em "Actions" → "Export as SVG"
4. Abra draw.io → File → Import → SVG
