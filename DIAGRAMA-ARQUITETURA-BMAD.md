# 📊 ARQUITETURA BMAD - DIAGRAMA VISUAL

```
╔═══════════════════════════════════════════════════════════════════════════════╗
║                            🖥️  FRONTEND ANGULAR                                ║
║                        http://localhost:4200                                  ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │                    📝 Formulário de Criação                          │    ║
║  │                                                                       │    ║
║  │  Título: _______________________________________________              │    ║
║  │                                                                       │    ║
║  │  Regra de Negócio: ______________________________________            │    ║
║  │  ___________________________________________________________          │    ║
║  │                                                                       │    ║
║  │  Agente: [ Gerador de Cenario de Testes ▼ ]                         │    ║
║  │                                                                       │    ║
║  │  Tipo de Workflow: [ Geração Rápida ▼ ]  ← NOVO!                    │    ║
║  │    ├─ Pipeline Completo (6 agentes)                                  │    ║
║  │    ├─ Geração Rápida (4 agentes)                                     │    ║
║  │    ├─ Revisão de Cenários (2 agentes)                                │    ║
║  │    └─ Análise de Regressão (4 agentes)                               │    ║
║  │                                                                       │    ║
║  │  📎 Anexar PDFs: [Selecionar arquivos]                               │    ║
║  │                                                                       │    ║
║  │              [ 🚀 Gerar Cenário ]                                     │    ║
║  └─────────────────────────────────────────────────────────────────────┘    ║
║                                  │                                            ║
║                                  │ HTTP POST /cenario                         ║
║                                  ▼                                            ║
╚═══════════════════════════════════════════════════════════════════════════════╝
                                   │
                                   │
                                   ▼
╔═══════════════════════════════════════════════════════════════════════════════╗
║                          ⚙️  BACKEND SPRING BOOT                               ║
║                        http://localhost:8080                                  ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │                     🎯 CenarioController                             │    ║
║  │                                                                       │    ║
║  │  POST /cenario                                                        │    ║
║  │  POST /cenario/com-pdf                                                │    ║
║  │  GET  /cenario/workflows  ← NOVO!                                    │    ║
║  │  GET  /cenario                                                        │    ║
║  │  GET  /cenario/{id}                                                   │    ║
║  │  DELETE /cenario/{id}                                                 │    ║
║  └─────────────────────────────────────────────────────────────────────┘    ║
║                                  │                                            ║
║                                  ▼                                            ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │                      🔄 CenarioService                                │    ║
║  │                  (Refatorado para usar BMAD)                          │    ║
║  │                                                                       │    ║
║  │  gerarCenarioCompleto(request) {                                     │    ║
║  │      return qaWorkflowService.executarWorkflow(request);             │    ║
║  │  }                                                                    │    ║
║  └─────────────────────────────────────────────────────────────────────┘    ║
║                                  │                                            ║
║                                  ▼                                            ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │                  🎼 QaWorkflowService (ORQUESTRADOR)                  │    ║
║  │                                                                       │    ║
║  │  executarWorkflow(request) {                                         │    ║
║  │      WorkflowContext context = new WorkflowContext(request);         │    ║
║  │                                                                       │    ║
║  │      List<BaseAgent> pipeline = montarPipelineAgentes(               │    ║
║  │          request.workflowType()                                      │    ║
║  │      );                                                               │    ║
║  │                                                                       │    ║
║  │      for (BaseAgent agent : pipeline) {                              │    ║
║  │          agent.executar(context);                                    │    ║
║  │      }                                                                │    ║
║  │                                                                       │    ║
║  │      return salvarResultado(context);                                │    ║
║  │  }                                                                    │    ║
║  └─────────────────────────────────────────────────────────────────────┘    ║
║                                  │                                            ║
║              ┌───────────────────┼───────────────────┐                       ║
║              │                   │                   │                       ║
║              ▼                   ▼                   ▼                       ║
║  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐           ║
║  │  WORKFLOW TYPE   │ │  WORKFLOW TYPE   │ │  WORKFLOW TYPE   │           ║
║  │    COMPLETO      │ │     RAPIDO       │ │    REVISAO       │           ║
║  │                  │ │                  │ │                  │           ║
║  │   6 agentes      │ │   4 agentes      │ │   2 agentes      │           ║
║  └──────────────────┘ └──────────────────┘ └──────────────────┘           ║
║          │                    │                    │                        ║
║          └────────────────────┴────────────────────┘                        ║
║                               │                                             ║
║                               ▼                                             ║
║  ╔═══════════════════════════════════════════════════════════════════╗    ║
║  ║               🤖 PIPELINE DE AGENTES (Exemplo: COMPLETO)          ║    ║
║  ╠═══════════════════════════════════════════════════════════════════╣    ║
║  ║                                                                    ║    ║
║  ║  1️⃣  RequirementAnalysisAgent                                      ║    ║
║  ║      │ Analisa requisitos e regras de negócio                     ║    ║
║  ║      └─► context.requisitos = "..."                               ║    ║
║  ║                                                                    ║    ║
║  ║  2️⃣  TranscriptAnalysisAgent                                       ║    ║
║  ║      │ Extrai decisões de ata de reunião                          ║    ║
║  ║      └─► context.decisoesReuniao = "..."                          ║    ║
║  ║                                                                    ║    ║
║  ║  3️⃣  TestPlanAgent                                                 ║    ║
║  ║      │ Cria plano macro de testes                                 ║    ║
║  ║      └─► context.planoMacro = "..."                               ║    ║
║  ║                                                                    ║    ║
║  ║  4️⃣  TestScenarioAgent                                             ║    ║
║  ║      │ Gera cenários de teste detalhados                          ║    ║
║  ║      └─► context.cenarios = [...]                                 ║    ║
║  ║                                                                    ║    ║
║  ║  5️⃣  RedundancyReviewAgent                                         ║    ║
║  ║      │ Remove redundâncias e sugere melhorias                     ║    ║
║  ║      └─► context.cenariosRevisados = [...]                        ║    ║
║  ║                                                                    ║    ║
║  ║  6️⃣  ZephyrFormatterAgent                                          ║    ║
║  ║      │ Formata campos para Zephyr                                 ║    ║
║  ║      └─► context.cenariosRevisados = [...] (formatado)            ║    ║
║  ║                                                                    ║    ║
║  ╚═══════════════════════════════════════════════════════════════════╝    ║
║                               │                                             ║
║                               ▼                                             ║
║  ┌─────────────────────────────────────────────────────────────────────┐  ║
║  │                      💾 MongoDB - Salvar Cenário                     │  ║
║  │                                                                       │  ║
║  │  {                                                                    │  ║
║  │    id: "67890...",                                                    │  ║
║  │    titulo: "Login de Usuário",                                       │  ║
║  │    regraDeNegocio: "...",                                            │  ║
║  │    criteriosAceitacao: "...",                                        │  ║
║  │    cenarios: [                                                        │  ║
║  │      {                                                                │  ║
║  │        nome: "CT001 - Login com credenciais válidas",                │  ║
║  │        objetivo: "...",                                              │  ║
║  │        precondicao: "...",                                           │  ║
║  │        scriptTeste: "...",                                           │  ║
║  │        resultadoEsperado: "..."                                      │  ║
║  │      },                                                               │  ║
║  │      ...                                                              │  ║
║  │    ]                                                                  │  ║
║  │  }                                                                    │  ║
║  └─────────────────────────────────────────────────────────────────────┘  ║
║                               │                                             ║
╚═══════════════════════════════│═══════════════════════════════════════════════╝
                                │
                                │ HTTP 200 OK
                                ▼
╔═══════════════════════════════════════════════════════════════════════════════╗
║                           ✅ FRONTEND - SUCESSO                                ║
╠═══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║  ┌─────────────────────────────────────────────────────────────────────┐    ║
║  │         ✅ Cenario gerado com sucesso!                               │    ║
║  │                                                                       │    ║
║  │              [ 👀 Visualizar Cenários ]                              │    ║
║  └─────────────────────────────────────────────────────────────────────┘    ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝
```

---

## 📊 COMPARAÇÃO DE WORKFLOWS

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          WORKFLOW COMPLETO                              │
├─────────────────────────────────────────────────────────────────────────┤
│  Tempo: ~2-3 min    │  Agentes: 6    │  Detalhe: ★★★★★                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ✅ RequirementAnalysisAgent                                            │
│  ✅ TranscriptAnalysisAgent                                             │
│  ✅ TestPlanAgent                                                       │
│  ✅ TestScenarioAgent                                                   │
│  ✅ RedundancyReviewAgent                                               │
│  ✅ ZephyrFormatterAgent                                                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                          WORKFLOW RAPIDO                                │
├─────────────────────────────────────────────────────────────────────────┤
│  Tempo: ~1-2 min    │  Agentes: 4    │  Detalhe: ★★★☆☆                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ✅ RequirementAnalysisAgent                                            │
│  ❌ TranscriptAnalysisAgent (PULADO)                                    │
│  ✅ TestPlanAgent                                                       │
│  ✅ TestScenarioAgent                                                   │
│  ✅ RedundancyReviewAgent                                               │
│  ✅ ZephyrFormatterAgent                                                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                          WORKFLOW REVISAO                               │
├─────────────────────────────────────────────────────────────────────────┤
│  Tempo: ~30 seg     │  Agentes: 2    │  Detalhe: ★★☆☆☆                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ❌ RequirementAnalysisAgent (PULADO)                                   │
│  ❌ TranscriptAnalysisAgent (PULADO)                                    │
│  ❌ TestPlanAgent (PULADO)                                              │
│  ❌ TestScenarioAgent (PULADO)                                          │
│  ✅ RedundancyReviewAgent                                               │
│  ✅ ZephyrFormatterAgent                                                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                          WORKFLOW REGRESSAO                             │
├─────────────────────────────────────────────────────────────────────────┤
│  Tempo: ~1-2 min    │  Agentes: 4    │  Detalhe: ★★★★☆                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ✅ RequirementAnalysisAgent                                            │
│  ✅ TranscriptAnalysisAgent                                             │
│  ❌ TestPlanAgent (PULADO)                                              │
│  ✅ TestScenarioAgent                                                   │
│  ✅ RedundancyReviewAgent                                               │
│  ✅ ZephyrFormatterAgent                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 FLUXO DE DADOS

```
CenarioRequest                 WorkflowContext                CenarioResponse
─────────────                 ───────────────                ────────────────
│ titulo                       │ request                      │ id
│ regraDeNegocio              │ requisitos                   │ titulo
│ agent                       │ decisoesReuniao              │ regraDeNegocio
│ workflowType  ─────────────►│ planoMacro                   │ criteriosAceitacao
                               │ cenarios                     │ cenarios: [
                               │ cenariosRevisados            │   {nome, objetivo,
                               │ metadata                     │    precondicao,
                               └──────────────────────────────►│    scriptTeste,
                                                               │    resultadoEsperado}
                                                               │ ]
```

---

**Criado por:** Jean Heberth  
**Data:** 29/07/2026  
**Versão:** 1.0
