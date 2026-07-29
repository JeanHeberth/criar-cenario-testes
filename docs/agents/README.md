# 🤖 Documentação de Agentes

Documentação de todos os agentes BMAD e workflows.

---

## 📚 Agentes Disponíveis

### [gerador_cenarios_testes.agent.md](gerador_cenarios_testes.agent.md)
Agente que gera cenários de testes.

---

### [robot_framework_web.agent.md](robot_framework_web.agent.md)
Agente para testes web com Robot Framework.

---

### [robot_framework_api.agent.md](robot_framework_api.agent.md)
Agente para testes API com Robot Framework.

---

### [robot_framework_qa_revisor.agent.md](robot_framework_qa_revisor.agent.md)
Agente QA revisor com Robot Framework.

---

## 📂 Workflows

### [workflows/README.md](workflows/README.md)
Documentação de todos os workflows.

Workflows disponíveis:

| Workflow | Agentes | Tempo | Uso |
|----------|---------|-------|-----|
| **COMPLETO** | 6 | 45s | Análise completa com revisão |
| **RAPIDO** | 4 | 22s | Geração rápida |
| **REVISAO** | 2 | 5s | Apenas revisão |
| **REGRESSAO** | 4 | 30s | Testes de regressão |

---

### [workflows/workflow-completo.md](workflows/workflow-completo.md)
Workflow COMPLETO detalhado.

- 6 agentes executados
- Quando usar
- Tempo estimado
- Exemplo de uso

---

### [workflows/workflow-rapido.md](workflows/workflow-rapido.md)
Workflow RÁPIDO detalhado.

- 4 agentes executados
- Diferenças do COMPLETO
- Quando usar

---

### [workflows/workflow-revisao.md](workflows/workflow-revisao.md)
Workflow REVISÃO detalhado.

- 2 agentes executados
- Apenas revisão, sem criação
- Quando usar

---

## 🔗 Links Rápidos

| Tipo | Documento | Tempo |
|------|-----------|-------|
| Agentes | [*.agent.md](.) | 5 min c/u |
| Workflows | [workflows/README.md](workflows/README.md) | 10 min |
| Completo | [workflows/workflow-completo.md](workflows/workflow-completo.md) | 5 min |
| Rápido | [workflows/workflow-rapido.md](workflows/workflow-rapido.md) | 3 min |
| Revisão | [workflows/workflow-revisao.md](workflows/workflow-revisao.md) | 3 min |

---

## 🎯 Por Persona

### 👨‍💻 Desenvolvedor Backend
→ Leia [workflows/README.md](workflows/README.md)  
→ Leia [gerador_cenarios_testes.agent.md](gerador_cenarios_testes.agent.md)

### 🧪 QA / Tester
→ Leia [workflows/README.md](workflows/README.md)  
→ Escolha o workflow apropriado

### 🏗️ Arquiteto
→ Leia [workflows/README.md](workflows/README.md)  
→ Leia todos os documentos de workflow

---

## 🚀 Adicionar Novo Agente

1. Criar classe em `src/main/java/com/br/criarcenariotestes/business/agent/`
2. Implementar interface `BaseAgent`
3. Criar teste em `src/test/java/com/br/criarcenariotestes/business/agent/`
4. Registrar no `WorkflowFactory`
5. Documentar em `docs/agents/{nome}.agent.md`
6. Adicionar ao workflow desejado

---

**[⬅️ Voltar para Documentação Principal](../README.md)**
