# Arquitetura BMAD - Business Multi-Agent Design

## 📐 Visão Geral

O projeto **criar-cenario-testes** implementa a arquitetura BMAD para geração inteligente de casos de teste.

Antes era um **serviço monolítico**. Agora é um **sistema orquestrado** com agentes especializados.

---

## 🏗️ Estrutura de Pastas

```
business/
├── agent/                           [NOVO - Agentes BMAD]
│   ├── BaseAgent.java               Interface base
│   ├── RequirementAnalysisAgent.java
│   ├── TranscriptAnalysisAgent.java
│   ├── TestPlanAgent.java
│   ├── TestScenarioAgent.java
│   ├── RedundancyReviewAgent.java
│   └── ZephyrFormatterAgent.java
│
├── workflow/                        [NOVO - Orquestração]
│   ├── WorkflowContext.java         Estado compartilhado
│   ├── WorkflowType.java            Enum de workflows
│   └── QaWorkflowService.java       Orquestrador principal
│
├── ai/                              [EXISTENTE]
│   ├── AiProvider.java
│   ├── AiProviderResolver.java
│   ├── OpenAiProvider.java
│   └── GeminiProvider.java
│
├── parser/                          [EXISTENTE]
├── prompt/                          [EXISTENTE]
├── fallback/                        [EXISTENTE]
└── service/                         [REFATORADO]
    └── CenarioService.java          Agora delega para QaWorkflowService
```

---

## 🎯 Fluxo de Execução

### Antes (Monolítico)

```
CenarioService
├── buildSystemPrompt()
├── buildUserPrompt()
├── chamar IA
├── parsear resposta
├── salvar no MongoDB
└── retornar response
```

**Problema:** Tudo em um único lugar, difícil de manter e evoluir.

---

### Depois (BMAD)

```
CenarioService.gerarCenarioCompleto()
    ↓
QaWorkflowService.executarWorkflow()
    ↓
1. RequirementAnalysisAgent.executar(context)
    → context.setRequisitos(...)
    ↓
2. TranscriptAnalysisAgent.executar(context)
    → context.setDecisoesReuniao(...)
    ↓
3. TestPlanAgent.executar(context)
    → context.setPlanoMacro(...)
    ↓
4. TestScenarioAgent.executar(context)
    → context.setCenarios(...)
    ↓
5. RedundancyReviewAgent.executar(context)
    → context.setCenariosRevisados(...)
    ↓
6. ZephyrFormatterAgent.executar(context)
    → context.setFormatoFinal(...)
    ↓
QaWorkflowService.salvarResultado(context)
    ↓
retorna CenarioResponse
```

**Vantagem:** 
- Cada agente tem uma responsabilidade clara
- Fácil testar individualmente
- Fácil adicionar novos agentes
- Workflows customizáveis

---

## 🔧 Conceitos-Chave

### 1. BaseAgent
Interface que todos os agentes implementam:

```java
public interface BaseAgent {
    void executar(WorkflowContext context);
    String getNome();
    boolean isEnabled(WorkflowContext context); // opcional
}
```

---

### 2. WorkflowContext
Objeto que carrega estado entre agentes:

```java
@Data
public class WorkflowContext {
    private CenarioRequest request;
    private String requisitos;           // do RequirementAnalyst
    private String decisoesReuniao;      // do TranscriptAnalyst
    private String planoMacro;           // do TestPlanAgent
    private List<CenarioItem> cenarios;  // do ScenarioGenerator
    private List<CenarioItem> cenariosRevisados; // do RedundancyReviewer
    private Map<String, Object> metadados;
}
```

---

### 3. WorkflowType
Enum que define qual pipeline de agentes executar:

```java
public enum WorkflowType {
    COMPLETO,    // Todos os 6 agentes
    RAPIDO,      // 4 agentes (pula Transcript e Redundancy)
    REVISAO,     // 2 agentes (apenas revisão)
    REGRESSAO    // 4 agentes (análise de impacto)
}
```

---

### 4. QaWorkflowService
Orquestrador que:
- Monta o pipeline de agentes baseado no `WorkflowType`
- Executa os agentes em sequência
- Passa o `WorkflowContext` entre eles
- Salva o resultado final no MongoDB

---

## 📚 Workflows Disponíveis

| Workflow | Agentes | Tempo | Uso |
|----------|---------|-------|-----|
| [**COMPLETO**](./workflow-completo.md) | 6 agentes | 3-5 min | Máxima qualidade |
| [**RAPIDO**](./workflow-rapido.md) | 4 agentes | 1-2 min | Cards simples/urgentes |
| [**REVISAO**](./workflow-revisao.md) | 2 agentes | 30-60s | Otimizar CTs existentes |
| **REGRESSAO** | 4 agentes | 2-3 min | Análise de impacto |

---

## 🔄 Backward Compatibility

O **CenarioService original** foi mantido:
- `gerarCenarioCompleto()` agora chama `QaWorkflowService`
- `gerarCenarioCompletoLegacy()` mantém lógica antiga (deprecated)
- Métodos públicos não mudaram (APIs compatíveis)

✅ **Frontend Angular não precisa mudar nada!**

---

## 🧪 Como Testar

### Teste de Workflow Completo
```bash
curl -X POST http://localhost:8080/api/cenarios \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Login OAuth",
    "regraDeNegocio": "Usuário deve fazer login...",
    "agent": "gerador_cenarios_testes"
  }'
```

### Logs Esperados
```
INFO - Iniciando workflow BMAD. tipo=COMPLETO
INFO - Executando agente: Requirement Analyst
INFO - Requisitos extraídos com sucesso
INFO - Executando agente: Transcript Analyst
INFO - Decisões extraídas com sucesso
INFO - Executando agente: Test Planning Agent
...
INFO - Workflow concluído. cenarios=12
```

---

## 🚀 Próximos Passos

1. ✅ **Nível 1-4:** Implementados
2. 🔲 **Nível 5:** Adicionar seleção de workflow no frontend
3. 🔲 **Testes unitários** para cada agente
4. 🔲 **Métricas:** Tempo de execução, token usage, sucesso/falha por agente
5. 🔲 **Dashboard:** Visualizar execução do workflow em tempo real

---

## 📖 Referências

- [Workflow Completo](./workflow-completo.md)
- [Workflow Rápido](./workflow-rapido.md)
- [Workflow Revisão](./workflow-revisao.md)

---

**Desenvolvido com Arquitetura BMAD** 🏆
