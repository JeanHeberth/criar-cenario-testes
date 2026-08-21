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

## 🎯 Para onde os casos de teste vão (roteamento)

O destino da publicação é **dado do pedido**, não configuração de ambiente —
é o que permite vários times na mesma instância, cada um publicando no seu
projeto.

| Campo (JSON) | O que é | Quando ausente |
| --- | --- | --- |
| `taskRef` | URL da tarefa (ou chave do Jira) a que os casos serão vinculados | Publica sem vínculo |
| `projectKey` | Projeto de destino no Zephyr | Derivado da `taskRef`; senão `ZEPHYR_PROJECT_KEY` |
| `pastaDestino` | Pasta raiz, tipicamente a stack (`Java`, `Robot`) | `ZEPHYR_ROOT_FOLDER` |

```jsonc
{
  "titulo": "Adicionar produto ao carrinho",
  "regraDeNegocio": "...",
  "agent": "gerador_cenarios_testes",
  // Cole a URL do navegador — é a entrada canônica
  "taskRef": "https://empresa.atlassian.net/browse/PAY-77"
}
```

**Por que a URL, e não a chave.** No Jira a chave carrega o projeto
(`PAY-77` → `PAY`). No Azure DevOps o work item é um id numérico global
(`1234`) que não diz organização nem projeto — só a URL carrega isso. A URL é
a única representação autossuficiente nos dois, e o **provedor é detectado
dela**, não configurado por ambiente. Ver `ReferenciaTarefaParser`.

Formatos aceitos:

- `https://empresa.atlassian.net/browse/SCRUM-28`
- `https://empresa.atlassian.net/jira/software/projects/SCRUM/boards/1?selectedIssue=SCRUM-28`
- `https://dev.azure.com/{org}/{projeto}/_workitems/edit/1234`
- `https://{org}.visualstudio.com/{projeto}/_workitems/edit/1234`
- `SCRUM-28` (chave pura, formato aceito antes)

**Estado do suporte a Azure DevOps:** o roteamento entende a URL, mas o
*vínculo* ainda é só Jira — a API do Zephyr Scale é addon do Jira e não aceita
work item. Uma referência do Azure publica os casos normalmente e registra um
aviso no log. Vincular do lado do Azure exigiria um adaptador de Azure Test
Plans, ainda não implementado.

**Derivar `projectKey` da chave é heurística** e vale só para Jira. Times com
um projeto Jira guarda-chuva e o Zephyr em outro lugar devem informar
`projectKey` explicitamente — ele tem precedência.

### Preview do destino (`GET /cenario/destino`)

Resolve para onde a publicação vai **sem gerar nada**:

```bash
curl -G http://localhost:8089/cenario/destino \
  --data-urlencode "taskRef=https://empresa.atlassian.net/browse/SCRUM-28"
```

```json
{ "provedor": "JIRA", "identificador": "SCRUM-28",
  "projectKey": "SCRUM", "pastaRaiz": "Postman", "valido": true, "motivo": null }
```

A tela usa isto ao sair do campo da tarefa, por dois motivos: descobrir que a
referência está errada só depois da geração custa uma rodada inteira de
chamadas de IA, e caso publicado no lugar errado não tem desfazer barato.

Também devolve o `identificador` já normalizado — é o que evita o front
reimplementar o parsing de URL que vive aqui. Referência inválida vem como
`valido: false` com o `motivo`, não como erro HTTP: é resposta legítima de um
preview.

O preview aplica **exatamente a mesma precedência** da publicação real. Um
preview que divergisse do comportamento seria pior que não ter preview.

---

### Derivar a pasta da tarefa (`folder-strategy`)

Em vez de configurar a pasta por ambiente ou informá-la em cada pedido, ela
pode sair de um campo que o time **já mantém** no rastreador:

```yaml
zephyr:
  folder-strategy:
    enabled: true
    sources: [components, labels]   # ordem de precedência
    mapping:                        # mapa FECHADO termo -> pasta
      java: Java
      postman: Postman
      robot: Robot
```

Precedência final da pasta raiz: `pastaDestino` do pedido → derivada da tarefa
→ `ZEPHYR_ROOT_FOLDER`.

**Por que regra declarada e não inferência da IA.** A stack não está no
requisito — é decisão do time sobre a automação, não propriedade da regra de
negócio. Pedir para o modelo adivinhar produziria pastas variando entre
gerações (`Login`, `Autenticação`, `Auth`), o que quebra a deduplicação
(escopada por `folderId`) e cria lixo permanente, já que pasta no Zephyr não
tem `DELETE`. Aqui a mesma tarefa sempre resolve para a mesma pasta, e a
resposta para "por que este caso foi parar aqui" é esta configuração.

**O mapa é fechado de propósito:** um valor que não está nele não vira pasta
nova — cai no destino padrão. É essa salvaguarda que limita o estrago de um
campo preenchido fora do padrão.

**Sobre `summary`:** funciona (`Automacao POSTMAN do POST Usuario` → `Postman`,
casando palavra inteira, então `javascript` não vira `Java`), mas é frágil —
depende de convenção de escrita. Trate como degrau de migração para projetos
que ainda não preenchem campo estruturado, não como destino. Num Jira
corporativo bem mantido, use `components`.

Hoje só lê do Jira: ler campos de work item do Azure DevOps exigiria o
adaptador ainda não implementado, e uma referência do Azure simplesmente não
deriva pasta.

---

### Governança: quem pode criar pasta

```yaml
zephyr:
  allow-folder-creation: ${ZEPHYR_ALLOW_FOLDER_CREATION:true}
```

Com `false`, a publicação só deposita em pastas que **já existem**; um caminho
inexistente falha aquele cenário dizendo qual era o esperado, e os demais
seguem. Quem define a taxonomia volta a ser o dono do board — o gerador só
deposita.

Isso existe porque o estrago de errar é assimétrico e **permanente**: a API do
Zephyr não expõe remoção de pasta (`DELETE /folders` responde **405**), então
cada pasta criada por engano vira limpeza manual pela interface. Em time
grande, onde cada squad traz sua convenção, criação livre multiplica variações
da mesma pasta (`Login`, `Autenticação`, `Auth`) e quebra a deduplicação, que
é escopada por `folderId`.

O default é `true` para não mudar o comportamento de quem já usa. **Times com
taxonomia governada devem ligar em `false`.**

Note a distinção deliberada: pasta inexistente com criação desligada **falha o
item**, enquanto instabilidade de rede ao resolver pasta continua caindo para
publicação sem pasta. Perder o caso por causa de rede seria pior que criá-lo
solto; criá-lo solto quando a política diz o contrário é exatamente o que se
quer evitar.

**Retrocompatibilidade:** `jiraIssueKey` e `pastaRaiz` continuam aceitos como
alias de `taskRef` e `pastaDestino`. Os nomes novos são neutros porque este é
contrato público consumido por front, Jenkins e testes: renomear depois, com
um time já usando Azure, custaria coordenar todos eles.

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
