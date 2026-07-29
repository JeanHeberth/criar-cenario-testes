# ⚡ Quick Reference - Cheat Sheet

Sua cola para navegação rápida na documentação de arquitetura.

---

## 🎯 Em 30 Segundos

```
Frontend Angular
    ↓ HTTP POST
Backend Spring Boot
    ↓ Orquestra
BMAD Pipeline (6 Agentes)
    ↓ Salva
MongoDB
```

---

## 📖 Os 6 Documentos

| # | Nome | Tempo | Foco | Leia quando |
|---|------|-------|------|------------|
| 1️⃣ | [Arquitetura Geral](01-arquitetura-geral.md) | 2 min | Stack & Camadas | Quer visão geral |
| 2️⃣ | [Fluxo de Execução](02-fluxo-execucao.md) | 5 min | 30 etapas | Quer entender fluxo |
| 3️⃣ | [Pipeline BMAD](03-pipeline-bmad.md) | 8 min | 6 Agentes | Quer aprender BMAD |
| 4️⃣ | [Diagrama de Classes](04-diagrama-classes.md) | 10 min | Código | Vai codificar |
| 5️⃣ | [Sequência de Requisição](05-sequencia-requisicao.md) | 6 min | Debug | Debugar erro |
| 6️⃣ | [Estrutura de Pacotes](06-estrutura-pacotes.md) | 8 min | Organização | Navegar código |

---

## 🚀 Caminhos Rápidos

### Entender em 15 minutos
```
1️⃣ (2 min) → 2️⃣ (5 min) → 3️⃣ (8 min)
```

### Começar a codificar
```
6️⃣ (8 min) → 4️⃣ (10 min) → 2️⃣ (5 min)
```

### Debugar um problema
```
5️⃣ (6 min) → 2️⃣ (5 min) → 4️⃣ (10 min)
```

### Dominar BMAD
```
3️⃣ (8 min) → 4️⃣ (10 min) → 5️⃣ (6 min)
```

---

## 🔗 Links Importantes

### Documentação Principal
- [README.md](README.md) - Índice detalhado

### Documentação do Projeto
- QUICK-START.md - Como começar (5 minutos)
- GUIA-DE-USO-BMAD.md - Uso completo
- IMPLEMENTACAO-BMAD.md - Detalhes técnicos
- TESTES-UNITARIOS-BMAD.md - Testes

---

## 🤖 Os 6 Agentes

```
1. RequirementAnalysis      → Analisa requisitos
2. TranscriptAnalysis       → Processa transcrição
3. TestPlan                 → Cria plano de testes
4. TestScenario             → Gera cenários
5. RedundancyReview         → Otimiza testes
6. ZephyrFormatter          → Formata para exportação
```

---

## 📦 Pacotes Principais

```
controller/              → Recebe requisições HTTP
business/service/        → Lógica de negócio
business/workflow/       → Orquestra agentes
business/agent/          → Implementação de agentes
business/repository/     → Persistência
business/parser/         → Parsing de dados
```

---

## ❓ Perguntas Rápidas

| Pergunta | Leia | Seção |
|----------|------|-------|
| Por onde começo? | 1️⃣ | Stack Tecnológico |
| Como funciona? | 2️⃣ | Fluxo Passo a Passo |
| O que é BMAD? | 3️⃣ | O que é BMAD? |
| Onde está classe X? | 6️⃣ | Detalhamento por Pacote |
| Como debugo? | 5️⃣ | Tracing a Request |
| Como adiciono agente? | 3️⃣ | Adicionando Novo Agente |
| Qual é fluxo de dados? | 2️⃣ | Fluxo de Dados por Camada |
| Como são os testes? | 6️⃣ | Estrutura de Testes |

---

## 🎨 Cores dos Documentos

```
🟥 01 - Arquitetura Geral (Iniciante)
🟠 02 - Fluxo de Execução (Fácil)
🟦 03 - Pipeline BMAD (Médio)
🟩 04 - Diagrama de Classes (Técnico)
🟪 05 - Sequência de Requisição (Técnico)
🟨 06 - Estrutura de Pacotes (Iniciante)
```

---

## ⏱️ Timeline

```
0s   → Request arrives
2s   → Validation
3s   → Service processing
4s   → Workflow creation
4-49s → Agents execution
     (RequirementAnalysis: 8s)
     (TranscriptAnalysis: 6s)
     (TestPlan: 12s)
     (TestScenario: 14s)
     (RedundancyReview: 3s)
     (ZephyrFormatter: 2s)
49s  → Database save
50s  → Response sent
```

---

## 📊 Workflows

| Workflow | Agentes | Tempo | Uso |
|----------|---------|-------|-----|
| COMPLETO | 6 | 45s | Análise completa |
| RAPIDO | 4 | 22s | Rápido |
| REVISAO | 2 | 5s | Apenas revisão |
| REGRESSAO | 4 | 30s | Testes regressão |

---

## 🎓 Nível de Dificuldade

```
⭐         Fácil     (01, 06)
⭐⭐       Médio     (02)
⭐⭐⭐     Técnico   (03, 05)
⭐⭐⭐⭐   Avançado  (04)
```

---

## 🔄 Fluxo Arquitetural

```
Browser
  ↓ (Angular)
Frontend Form
  ↓ HTTP POST
Controller (Spring Boot)
  ↓
CenarioService
  ↓
QaWorkflowService
  ↓
WorkflowFactory
  ↓
WorkflowExecutor
  ↓
BaseAgent Pipeline
  ├─ RequirementAnalysisAgent
  ├─ TranscriptAnalysisAgent
  ├─ TestPlanAgent
  ├─ TestScenarioAgent
  ├─ RedundancyReviewAgent
  └─ ZephyrFormatterAgent
  ↓
CenarioRepository
  ↓
MongoDB
  ↓ (Response)
Browser
```

---

## 🧠 Conceitos-Chave

- **BMAD** = Business Multi-Agent Design (pipeline de agentes)
- **WorkflowContext** = Objeto imutável compartilhado entre agentes
- **Workflow** = Seleção de agentes a executar (COMPLETO, RAPIDO, etc)
- **Agent** = Classe que implementa lógica específica
- **Pipeline** = Execução sequencial de agentes

---

## 📝 Padrões de Design

- **Factory** = WorkflowFactory cria pipelines
- **Strategy** = Cada workflow é uma estratégia
- **Template Method** = AbstractAgent define fluxo comum
- **Repository** = Abstração de persistência

---

## 🎯 Onboarding (3 horas)

```
[ ] Este arquivo (2 min)
[ ] 01-arquitetura-geral.md (2 min)
[ ] 06-estrutura-pacotes.md (8 min)
[ ] Explorar código (15 min)
[ ] 04-diagrama-classes.md (10 min)
[ ] 02-fluxo-execucao.md (5 min)
[ ] Rodar projeto (15 min)
[ ] Criar cenário via UI (10 min)
[ ] Debugar com DevTools (5 min)
[ ] 05-sequencia-requisicao.md (6 min)
[ ] 03-pipeline-bmad.md (8 min)
✅ Você está pronto!
```

---

## 💡 Dicas Úteis

- Abra 2 abas: um documento e o código-fonte
- Use Ctrl+F para procurar tópicos
- Copie URLs dos diagramas para salvar/compartilhar
- Os diagramas Mermaid são renderizados automaticamente no GitHub
- Adicione bookmarks nos documentos mais usados

---

## 🚨 Checklist de Erro Comum

Ao debugar, verifique em ordem:

```
[ ] Validação da requisição (02 - Tratamento de Erros)
[ ] Desserialização do JSON (05 - Stage 2)
[ ] Lógica de negócio (02 - Fluxo Passo a Passo)
[ ] Agente específico falhando (03 - Cada agente)
[ ] Conexão com banco de dados (05 - Stage 6)
[ ] Response serializado corretamente (05 - Stage 6)
```

---

**Última atualização:** Julho 2024  
**Status:** ✅ Completo  
**Criado para:** Navegação Rápida
