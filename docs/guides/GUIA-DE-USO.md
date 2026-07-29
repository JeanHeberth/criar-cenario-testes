# 🚀 GUIA DE USO - IMPLEMENTAÇÃO BMAD

## 📋 Como Usar a Nova Arquitetura Multi-Agente

---

## 🎯 **Visão Geral**

Seu projeto agora possui uma arquitetura BMAD (Business Multi-Agent Design) com:

- **6 agentes especializados** que trabalham em pipeline
- **4 tipos de workflows** para diferentes necessidades
- **Orquestração automática** via QaWorkflowService
- **Backward compatibility** total (código antigo continua funcionando)

---

## 🏃 **1. INICIANDO O PROJETO**

### **Backend (Spring Boot)**

```bash
cd /Users/jeanheberth/Development/api/criar-cenario-testes

# Opção 1: Gradle
./gradlew bootRun

# Opção 2: IntelliJ IDEA
# Run → Run 'CriarCenarioTestesApplication'
```

**Verificar se subiu:**
```bash
curl http://localhost:8080/cenario/workflows
```

**Resultado esperado:**
```json
[
  {
    "tipo": "COMPLETO",
    "nome": "Pipeline Completo",
    "descricao": "Executa todos os 6 agentes (análise completa)",
    "agentes": [...]
  },
  ...
]
```

---

### **Frontend (Angular)**

```bash
cd /Users/jeanheberth/Development/front/gerar-cenario-teste-app

# Instalar dependências (primeira vez)
npm install

# Subir aplicação
npm start
```

**Abrir no navegador:**
```
http://localhost:4200
```

---

## 📱 **2. USANDO VIA INTERFACE ANGULAR**

### **Passo a Passo:**

1. **Acesse:** http://localhost:4200

2. **Preencha o formulário:**
   - **Título:** `Login de Usuário`
   - **Regra de Negócio:**
     ```
     O sistema deve permitir login com email e senha.
     Após 3 tentativas incorretas, bloquear conta por 15 minutos.
     Suportar autenticação OAuth (Google e Microsoft).
     ```
   - **Agente:** `Gerador de Cenario de Testes` (padrão)
   - **Tipo de Workflow:** Escolha um dos 4:

---

### **🐌 COMPLETO - Pipeline Completo**
**Quando usar:**
- Primeira vez criando cenários de uma funcionalidade
- Necessita análise detalhada
- Tem ata de reunião ou requisitos complexos

**Agentes executados:**
1. RequirementAnalysisAgent
2. TranscriptAnalysisAgent
3. TestPlanAgent
4. TestScenarioAgent
5. RedundancyReviewAgent
6. ZephyrFormatterAgent

**Tempo estimado:** ~2-3 minutos
**Resultado:** Cenários muito detalhados e completos

---

### **🐇 RAPIDO - Geração Rápida**
**Quando usar:**
- Necessita cenários rapidamente
- Não tem ata de reunião
- Requisitos são claros e simples

**Agentes executados:**
1. RequirementAnalysisAgent
2. ~~TranscriptAnalysisAgent~~ (PULADO)
3. TestPlanAgent
4. TestScenarioAgent
5. RedundancyReviewAgent
6. ZephyrFormatterAgent

**Tempo estimado:** ~1-2 minutos
**Resultado:** Cenários bons, menos detalhados

---

### **🔍 REVISAO - Revisão de Cenários**
**Quando usar:**
- Já tem cenários criados
- Quer apenas revisar e remover redundâncias
- Quer formatar para Zephyr

**Agentes executados:**
1. ~~RequirementAnalysisAgent~~
2. ~~TranscriptAnalysisAgent~~
3. ~~TestPlanAgent~~
4. ~~TestScenarioAgent~~
5. RedundancyReviewAgent
6. ZephyrFormatterAgent

**Tempo estimado:** ~30 segundos
**Resultado:** Cenários revisados e formatados

---

### **🔄 REGRESSAO - Análise de Regressão**
**Quando usar:**
- Alteração em funcionalidade existente
- Necessita testar regressão
- Foco em impactos colaterais

**Agentes executados:**
1. RequirementAnalysisAgent
2. TranscriptAnalysisAgent
3. ~~TestPlanAgent~~
4. TestScenarioAgent (modo regressão)
5. RedundancyReviewAgent
6. ZephyrFormatterAgent

**Tempo estimado:** ~1-2 minutos
**Resultado:** Cenários focados em regressão

---

### **3. Anexar PDFs (Opcional)**

Se tiver documentação:

```
📎 Anexar PDFs:
- Devbox.pdf
- Estimativas.pdf
- Especificacao.pdf
```

**Ou arraste e solte** na área indicada.

---

### **4. Gerar Cenário**

Clique em **"🚀 Gerar Cenário"**

**O que acontece:**
1. ✅ Loading aparece: "⏳ Gerando..."
2. ✅ Backend processa com workflow escolhido
3. ✅ Sucesso: "✅ Cenario gerado com sucesso!"
4. ✅ Cenário salvo no MongoDB

---

### **5. Visualizar Cenários**

Clique em **"👀 Visualizar Cenários"**

**Você verá:**
- Lista de todos os cenários criados
- Exportar para Excel, PDF, Zephyr
- Detalhes de cada cenário

---

## 🔧 **3. USANDO VIA API (cURL/Postman)**

### **3.1. Listar Workflows Disponíveis**

```bash
curl http://localhost:8080/cenario/workflows
```

**Resposta:**
```json
[
  {
    "tipo": "COMPLETO",
    "nome": "Pipeline Completo",
    "descricao": "Executa todos os 6 agentes (análise completa)",
    "agentes": [
      "Análise de Requisitos",
      "Análise de Ata de Reunião",
      "Planejamento de Testes",
      "Geração de Cenários",
      "Revisão de Redundâncias",
      "Formatação Zephyr"
    ]
  },
  {
    "tipo": "RAPIDO",
    "nome": "Geração Rápida",
    "descricao": "Executa 4 agentes (pula análise de ata)",
    "agentes": [...]
  },
  ...
]
```

---

### **3.2. Criar Cenário - Workflow COMPLETO**

```bash
curl -X POST http://localhost:8080/cenario \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Login de Usuário",
    "regraDeNegocio": "O sistema deve permitir login com email e senha. Após 3 tentativas incorretas, bloquear conta por 15 minutos.",
    "agent": "gpt-4o",
    "workflowType": "COMPLETO"
  }'
```

---

### **3.3. Criar Cenário - Workflow RAPIDO**

```bash
curl -X POST http://localhost:8080/cenario \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Cadastro de Produto",
    "regraDeNegocio": "Permitir cadastro de produtos com nome, preço e categoria.",
    "agent": "gpt-4o",
    "workflowType": "RAPIDO"
  }'
```

---

### **3.4. Criar Cenário - Workflow REVISAO**

```bash
curl -X POST http://localhost:8080/cenario \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Revisão de Cenários de Login",
    "regraDeNegocio": "Cenários já criados, apenas revisar.",
    "workflowType": "REVISAO"
  }'
```

---

### **3.5. Criar Cenário com PDFs**

```bash
curl -X POST http://localhost:8080/cenario/com-pdf \
  -F "titulo=Login com OAuth" \
  -F "regraDeNegocio=Integrar login com Google e Microsoft" \
  -F "agent=gpt-4o" \
  -F "workflowType=COMPLETO" \
  -F "arquivos=@/caminho/para/devbox.pdf" \
  -F "arquivos=@/caminho/para/specs.pdf"
```

---

### **3.6. Listar Cenários Criados**

```bash
curl http://localhost:8080/cenario
```

---

### **3.7. Buscar Cenário por ID**

```bash
curl http://localhost:8080/cenario/{id}
```

---

### **3.8. Excluir Cenário**

```bash
curl -X DELETE http://localhost:8080/cenario/{id}
```

---

## 📊 **4. MONITORANDO A EXECUÇÃO**

### **Logs do Backend**

Durante a execução, você verá logs como:

```
[QaWorkflowService] Executando workflow COMPLETO para: Login de Usuário
[QaWorkflowService] Pipeline montado com 6 agentes
[RequirementAnalysisAgent] Executando análise de requisitos...
[TranscriptAnalysisAgent] Analisando ata de reunião...
[TestPlanAgent] Criando plano de testes...
[TestScenarioAgent] Gerando cenários de teste...
[RedundancyReviewAgent] Revisando redundâncias...
[ZephyrFormatterAgent] Formatando para Zephyr...
[QaWorkflowService] Workflow executado com sucesso
```

---

### **Console do Angular**

Abra DevTools (F12) → Console:

```
[cenario.component] Carregando workflows...
[cenario.component] 4 workflows carregados
[cenario.component] Gerando cenário com workflow COMPLETO...
[cenario.component] Cenário gerado com sucesso!
```

---

### **Network (DevTools)**

F12 → Network:

```
GET /cenario/workflows → 200 OK (workflows carregados)
POST /cenario → 200 OK (cenário criado)
```

---

## 🐛 **5. DEBUGANDO PROBLEMAS**

### **Problema: Dropdown de workflows vazio**

**Causa:** Backend não está respondendo

**Solução:**
```bash
# Verificar se backend está rodando
curl http://localhost:8080/cenario/workflows

# Se não responder, subir backend:
cd /Users/jeanheberth/Development/api/criar-cenario-testes
./gradlew bootRun
```

---

### **Problema: Erro ao gerar cenário**

**Causa:** IA não está respondendo ou falhou

**Solução:**
1. Ver logs do backend
2. Verificar se API key da IA está configurada
3. Testar com workflow mais simples (REVISAO)

---

### **Problema: Cenário demora muito**

**Causa:** Workflow COMPLETO é mais lento

**Solução:**
- Use workflow **RAPIDO** para resultados mais rápidos
- Use workflow **REVISAO** apenas para revisar

---

### **Problema: Campos do formulário não aparecem**

**Causa:** Angular não compilou ou erro no código

**Solução:**
```bash
cd /Users/jeanheberth/Development/front/gerar-cenario-teste-app

# Recompilar
npm run build

# Subir novamente
npm start
```

---

## 🔌 **6. INTEGRANDO COM OUTROS SISTEMAS**

### **Integração com Jira**

O formulário já tem integração com Jira:

1. Informe a task: `OP-1122`
2. Clique em "Buscar anexos da task"
3. PDFs são importados automaticamente

---

### **Integração via API**

Outros sistemas podem chamar sua API:

```javascript
// Node.js exemplo
const axios = require('axios');

const response = await axios.post('http://localhost:8080/cenario', {
  titulo: 'Teste Integração',
  regraDeNegocio: 'Validar integração com sistema externo',
  agent: 'gpt-4o',
  workflowType: 'RAPIDO'
});

console.log('Cenário criado:', response.data.id);
```

---

### **Integração com CI/CD**

No Jenkins, adicione step:

```groovy
stage('Gerar Cenários de Teste') {
    steps {
        sh '''
            curl -X POST http://qa-api.empresa.com/cenario \
              -H "Content-Type: application/json" \
              -d '{"titulo":"Testes Automatizados","regraDeNegocio":"...","workflowType":"RAPIDO"}'
        '''
    }
}
```

---

## 🎨 **7. CUSTOMIZANDO WORKFLOWS**

### **Criar Novo Workflow**

**1. Adicionar no Enum:**

```java
// WorkflowType.java
public enum WorkflowType {
    COMPLETO,
    RAPIDO,
    REVISAO,
    REGRESSAO,
    CUSTOMIZADO  // ← NOVO
}
```

**2. Configurar agentes:**

```java
// QaWorkflowService.java
private List<BaseAgent> montarPipelineAgentes(WorkflowType workflowType) {
    return switch (workflowType) {
        case CUSTOMIZADO -> List.of(
            requirementAnalysisAgent,
            testScenarioAgent,
            zephyrFormatterAgent
        );
        // ...
    };
}
```

**3. Documentar:**

Criar arquivo `agents/workflows/workflow-customizado.md`

---

### **Criar Novo Agente**

**1. Implementar BaseAgent:**

```java
@Service
@Slf4j
public class MeuNovoAgent implements BaseAgent {
    
    @Override
    public void executar(WorkflowContext context) {
        log.info("Executando MeuNovoAgent...");
        // Sua lógica aqui
    }
    
    @Override
    public String getNome() {
        return "Meu Novo Agente";
    }
    
    @Override
    public boolean isEnabled(WorkflowContext context) {
        return true;
    }
}
```

**2. Adicionar ao workflow:**

```java
// QaWorkflowService.java
@Autowired
private MeuNovoAgent meuNovoAgent;

private List<BaseAgent> montarPipelineAgentes(WorkflowType workflowType) {
    return switch (workflowType) {
        case COMPLETO -> List.of(
            requirementAnalysisAgent,
            meuNovoAgent,  // ← NOVO
            testPlanAgent,
            // ...
        );
    };
}
```

---

## 📈 **8. BOAS PRÁTICAS**

### ✅ **DOs (Faça)**

- Use **COMPLETO** para funcionalidades críticas
- Use **RAPIDO** para desenvolvimento rápido
- Use **REVISAO** para melhorar cenários existentes
- Anexe PDFs quando tiver documentação
- Revise cenários gerados antes de usar
- Escolha o agente IA apropriado (GPT-4, Claude, etc)

### ❌ **DON'Ts (Não faça)**

- Não use COMPLETO se está com pressa
- Não use REVISAO para criar novos cenários
- Não ignore erros no console
- Não altere diretamente classes de agente sem necessidade
- Não quebre backward compatibility

---

## 📚 **9. DOCUMENTAÇÃO ADICIONAL**

| Arquivo | Descrição |
|---------|-----------|
| `IMPLEMENTACAO-BMAD.md` | Arquitetura completa |
| `TESTES-UNITARIOS-BMAD.md` | Testes implementados |
| `RESULTADO-TESTES-BMAD.md` | Resultado dos testes |
| `IMPACTO-FRONTEND-BMAD.md` | Impacto no Angular |
| `agents/workflows/README.md` | Visão geral dos workflows |
| `agents/workflows/workflow-completo.md` | Detalhes do workflow COMPLETO |
| `agents/workflows/workflow-rapido.md` | Detalhes do workflow RAPIDO |
| `agents/workflows/workflow-revisao.md` | Detalhes do workflow REVISAO |

---

## 🎯 **10. CHECKLIST DE USO**

Antes de usar em produção:

- [ ] Backend rodando sem erros
- [ ] Frontend abrindo normalmente
- [ ] Dropdown de workflows carregando
- [ ] Teste manual com cada workflow
- [ ] Logs do backend funcionando
- [ ] MongoDB conectado
- [ ] API keys das IAs configuradas
- [ ] Testes unitários passando (52/52)
- [ ] Documentação lida

---

## 🆘 **11. SUPORTE**

### **Problemas Comuns:**

1. **"Nenhum workflow disponivel"**
   → Backend offline, suba com `./gradlew bootRun`

2. **"Erro ao gerar cenário"**
   → Veja logs do backend, pode ser problema com IA

3. **"Build failed"**
   → Limpe: `./gradlew clean build`

4. **"Testes falhando"**
   → Rode: `./gradlew test` e veja relatório

---

## 🎉 **RESUMO**

**Você agora tem:**
- ✅ Arquitetura multi-agente funcional
- ✅ 4 workflows diferentes
- ✅ Interface Angular moderna
- ✅ API REST completa
- ✅ Testes unitários (52 testes)
- ✅ Documentação completa
- ✅ Backward compatibility

**Comece usando:**
1. Suba backend e frontend
2. Acesse http://localhost:4200
3. Crie seu primeiro cenário com workflow RAPIDO
4. Explore os outros workflows

**Divirta-se gerando cenários de teste! 🚀**

---

**Autor:** Jean Heberth  
**Data:** 29/07/2026  
**Versão:** 1.0  
**Status:** 📗 Pronto para Uso
