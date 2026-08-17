# 🎯 Criar Cenário de Testes - BMAD Architecture

Sistema avançado de geração de cenários de teste usando arquitetura **BMAD** (Business Multi-Agent Design).

> **Documentação centralizada em `docs/`** - Todos os guias, diagramas e especificações estão organizados por categoria.

---

## 🚀 Início Rápido

```bash
# 1. Instalar dependências
npm install              # Frontend (Angular)
./gradlew clean build    # Backend (Spring Boot)

# 2. Subir o projeto
docker-compose up       # MongoDB + Backend
npm start              # Frontend (em outra aba)

# 3. Acessar
http://localhost:4200  # Frontend
http://localhost:8080  # Backend API
```

👉 **Documentação completa:** [docs/01-QUICK-START.md](docs/01-QUICK-START.md)

---

## 📚 Documentação

### 📖 [docs/README.md](docs/README.md) - Índice Principal
Índice completo de toda a documentação com navegação por persona.

### 🏛️ [docs/architecture/](docs/architecture/) - Arquitetura (45 min)
Documentação técnica completa em 6 documentos:
- Arquitetura Geral
- Fluxo de Execução
- Pipeline BMAD (6 agentes)
- Diagrama de Classes
- Sequência de Requisição
- Estrutura de Pacotes

### 📖 [docs/guides/](docs/guides/) - Guias (30 min)
- Como usar o sistema
- Detalhes de implementação
- Impacto no frontend

### 🧪 [docs/testing/](docs/testing/) - Testes (15 min)
- Estratégia de testes
- Resultados (52 testes ✅)

### 📊 [docs/diagrams/](docs/diagrams/) - Diagramas
- Arquitetura visual
- Diagramas Mermaid editáveis

### 🤖 [docs/agents/](docs/agents/) - Agentes (20 min)
- Documentação de agentes
- Workflows (COMPLETO, RÁPIDO, REVISÃO, REGRESSÃO)

---

## 🎯 Por Persona

| Persona | Tempo | Comece por | Objetivo |
|---------|-------|-----------|----------|
| **👔 Gestor/PO** | 15 min | [01-QUICK-START.md](docs/01-QUICK-START.md) | Entender projeto |
| **👨‍💻 Dev Backend** | 2h | [docs/README.md](docs/README.md) → Backend path | Ser produtivo |
| **👨‍💻 Dev Frontend** | 1h | [docs/README.md](docs/README.md) → Frontend path | Integrar com API |
| **🧪 QA/Tester** | 45 min | [docs/testing/](docs/testing/) | Testar sistema |
| **🏗️ Arquiteto** | 1h | [docs/architecture/](docs/architecture/) | Entender design |

---

## 🏗️ Stack Tecnológico

```
Frontend:  Angular 17 (TypeScript)
API:       Spring Boot 3 (Java 21)
Agentes:   BMAD Pipeline (Java)
Database:  MongoDB
Orquestr:  Docker + Kubernetes Ready
CI/CD:     Jenkins + GitHub Actions
```

---

## 🧠 Providers de IA

Os agentes BMAD não falam com um modelo específico: eles resolvem o provider
ativo pelo `AiProviderResolver`. Trocar de modelo é trocar variável de ambiente,
sem alterar código de agente.

| Provider | `AI_ACTIVE_PROVIDER` | Chave                | Modelo padrão      |
| -------- | -------------------- | -------------------- | ------------------ |
| OpenAI   | `openai`             | `OPENAI_API_KEY`     | `gpt-4.1`          |
| Gemini   | `gemini`             | `GEMINI_API_KEY`     | `gemini-2.5-flash` |
| Claude   | `claude`             | `ANTHROPIC_API_KEY`  | `claude-opus-5`    |

```bash
# .env — usar Claude
AI_ACTIVE_PROVIDER=claude
ANTHROPIC_API_KEY=sk-ant-...
```

**Notas sobre o Claude** (`ClaudeProvider`, via SDK oficial `com.anthropic:anthropic-java`):

- **`CLAUDE_EFFORT`** (`low|medium|high|xhigh|max`, padrão `medium`) é o controle
  de custo/profundidade. Geração de cenário é formatação estruturada, não
  raciocínio longo — `medium` entrega a mesma qualidade com bem menos tokens que
  o padrão `high` da API.
- **Thinking fica ligado** (`CLAUDE_THINKING_ENABLED=true`). Desligar seria o
  análogo do que fizemos no Gemini, mas no Claude tem efeito colateral conhecido:
  o modelo pode vazar tags `<thinking>` no texto visível, e o `CenarioTextoParser`
  quebraria com isso.
- **`CLAUDE_THINKING_HEADROOM_TOKENS`** (padrão `8000`): no Claude o raciocínio
  divide o mesmo teto de `max_tokens` com o texto final. Essa folga é somada ao
  limite pedido pelo chamador — sem ela, o override de 8000 tokens do
  `TestScenarioAgent` pode ser consumido pelo raciocínio e devolver cenários
  truncados no meio, reprovando na validação BDD.
- Recusa por política chega como HTTP 200 com `stop_reason=refusal`; o provider
  trata isso explicitamente para não mascarar como "resposta vazia".

---

## 📊 Estrutura de Pastas

```
criar-cenario-testes/
├── README.md                        (Este arquivo)
├── docs/                            (📚 TODA A DOCUMENTAÇÃO)
│   ├── README.md                    (Índice principal)
│   ├── 01-QUICK-START.md           (Comece aqui!)
│   ├── architecture/                (6 docs técnicos)
│   ├── guides/                      (Guias práticos)
│   ├── testing/                     (Testes)
│   ├── diagrams/                    (Diagramas)
│   └── agents/                      (Agentes)
│
├── src/
│   ├── main/java/                  (Backend - Spring Boot)
│   │   └── com/br/criarcenariotestes/
│   │       ├── controller/
│   │       ├── business/
│   │       │   ├── service/
│   │       │   ├── workflow/
│   │       │   ├── agent/          (6 agentes BMAD)
│   │       │   └── repository/
│   │       └── parser/
│   │
│   └── test/java/                  (Testes - 52 testes ✅)
│
├── agents/                         (Agentes de teste)
│   ├── *.agent.md
│   └── workflows/
│       ├── workflow-completo.md
│       ├── workflow-rapido.md
│       └── workflow-revisao.md
│
└── front/                          (Frontend - Angular 17)
    └── gerar-cenario-teste-app/
```

---

## 🤖 Agentes BMAD (6 Agentes em Pipeline)

```
RequirementAnalysis
    ↓ (Analisa requisitos)
TranscriptAnalysis
    ↓ (Processa transcrição)
TestPlan
    ↓ (Cria plano)
TestScenario
    ↓ (Gera cenários)
RedundancyReview
    ↓ (Otimiza)
ZephyrFormatter
    ↓ (Formata saída)
Test Cases Ready!
```

---

## 🧪 Testes

```
✅ 52 Testes Unitários
✅ 9 Classes de Teste
✅ Padrão AAA (Arrange, Act, Assert)
✅ Mocks e Stubs completos
```

Rodar:
```bash
mvn clean test
```

---

## 🔧 Desenvolvimento

### Setup Local
```bash
# Clone
git clone <repo>
cd criar-cenario-testes

# Backend
./gradlew clean build
docker-compose up

# Frontend
cd front/gerar-cenario-teste-app
npm install
npm start
```

### Adicionar Novo Agente
```
1. Criar classe em src/main/java/.../business/agent/
2. Estender AbstractAgent
3. Implementar execute()
4. Criar teste unitário
5. Registrar no WorkflowFactory
6. Documentar em docs/agents/
```

---

## 🚀 Deployment

### Docker
```bash
docker-compose up -d
```

### Kubernetes
```bash
kubectl apply -f k8s/
```

### CI/CD
- Jenkins pipeline configurado (Jenkinsfile)
- Testes rodam automaticamente
- Build gerado como Docker image

---

## 📈 Status

| Componente | Status | Cobertura |
|-----------|--------|----------|
| Backend | ✅ Production Ready | 85%+ |
| Frontend | ✅ Production Ready | 70%+ |
| Testes | ✅ 52/52 Passing | 85%+ |
| Docs | ✅ Completa | 100% |
| Docker | ✅ Ready | - |
| CI/CD | ✅ Configurado | - |

---

## 📞 Links Importantes

- **📖 Documentação:** [docs/README.md](docs/README.md)
- **⚡ Quick Start:** [docs/01-QUICK-START.md](docs/01-QUICK-START.md)
- **🏛️ Arquitetura:** [docs/architecture/](docs/architecture/)
- **🧪 Testes:** [docs/testing/](docs/testing/)
- **🤖 Agentes:** [docs/agents/](docs/agents/)

---

## 🤝 Contribuindo

1. **Fork** o projeto
2. **Crie uma branch** para sua feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. **Push** para a branch (`git push origin feature/AmazingFeature`)
5. **Abra um Pull Request**

---

## 📝 Licença

Proprietary - Jean Heberth

---

## 📧 Contato

📧 Email: jean.heberth@email.com  
💼 LinkedIn: linkedin.com/in/jeanheberth

---

**Última atualização:** Julho 2024  
**Versão:** 2.0  
**Status:** ✅ Production Ready

---

**👉 [Comece pela documentação →](docs/README.md)**
