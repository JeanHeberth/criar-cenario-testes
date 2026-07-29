# 🏛️ Documentação de Arquitetura

Bem-vindo à documentação arquitetural do projeto **Criar Cenário de Testes BMAD**!

Esta documentação fornece uma visão completa e estruturada da arquitetura do projeto, similar às usadas em grandes empresas.

---

## 📚 Documentos Principais

### 🟥 [01 - Arquitetura Geral](01-arquitetura-geral.md)
**Para quem:** Executivos, PMs, qualquer pessoa querendo entender tudo em 30 segundos

- Stack tecnológico (Angular + Spring Boot + BMAD + MongoDB)
- Camadas da aplicação
- Componentes principais
- Fluxo de dados resumido
- Infraestrutura

**Tempo:** 2 minutos | **Fácil** ⭐

---

### 🟠 [02 - Fluxo de Execução](02-fluxo-execucao.md)
**Para quem:** Desenvolvedores, QAs querendo entender como uma requisição é processada

- Fluxo passo a passo (30 etapas)
- Estados da execução
- Workflows: COMPLETO, RÁPIDO, REVISÃO, REGRESSÃO
- Tempo de execução por workflow
- Tratamento de erros
- Timeline de requisição

**Tempo:** 5 minutos | **Médio** ⭐⭐

---

### 🟦 [03 - Pipeline BMAD](03-pipeline-bmad.md)
**Para quem:** Arquitetos, desenvolvedores que querem entender os agentes

- O que é BMAD (Business Multi-Agent Design)
- 6 Agentes detalhados:
  - RequirementAnalysis
  - TranscriptAnalysis
  - TestPlan
  - TestScenario
  - RedundancyReview
  - ZephyrFormatter
- WorkflowContext (o coração do pipeline)
- Vantagens da arquitetura
- Como adicionar novo agente

**Tempo:** 8 minutos | **Avançado** ⭐⭐⭐

---

### 🟩 [04 - Diagrama de Classes](04-diagrama-classes.md)
**Para quem:** Desenvolvedores, arquitetos entendendo a estrutura de classes

- Controller Layer
- Service Layer
- Workflow Engine
- Agents Layer
- Data Layer (Repository)
- Padrões de design (Factory, Strategy, Template Method)
- Hierarquia de classes
- Fluxo de execução com classes

**Tempo:** 10 minutos | **Técnico** ⭐⭐⭐⭐

---

### 🟪 [05 - Sequência da Requisição](05-sequencia-requisicao.md)
**Para quem:** Debuggers, alguém que quer traçar exatamente o que acontece

- Caminho completo de uma requisição (30 estágios)
- Dados em cada estágio
- Timeline de execução
- Pontos de erro possíveis
- Como fazer tracing (DevTools, Logs, DB)
- Performance checkpoints

**Tempo:** 6 minutos | **Hands-on** ⭐⭐⭐

---

### 🟨 [06 - Estrutura de Pacotes](06-estrutura-pacotes.md)
**Para quem:** Desenvolvedores novatos, alguém conhecendo a estrutura do projeto

- Visão geral da estrutura de diretórios
- Detalhamento por pacote (Controller, Service, Agent, etc)
- Responsabilidades de cada camada
- Mapa mental da arquitetura
- Convenções de nomenclatura
- Estrutura de testes
- Ciclo de desenvolvimento

**Tempo:** 8 minutos | **Iniciante** ⭐

---

## 🗺️ Navegação por Persona

### 👨‍💼 **Gestor / Product Owner**
1. [01 - Arquitetura Geral](01-arquitetura-geral.md) - Entender o projeto
2. [02 - Fluxo de Execução](02-fluxo-execucao.md) - Ver como funciona

### 👨‍💻 **Desenvolvedor Backend (Iniciante)**
1. [06 - Estrutura de Pacotes](06-estrutura-pacotes.md) - Conhecer estrutura
2. [01 - Arquitetura Geral](01-arquitetura-geral.md) - Visão geral
3. [02 - Fluxo de Execução](02-fluxo-execucao.md) - Entender fluxo
4. [04 - Diagrama de Classes](04-diagrama-classes.md) - Estrutura de classes

### 👨‍💻 **Desenvolvedor Backend (Experiente)**
1. [03 - Pipeline BMAD](03-pipeline-bmad.md) - Entender BMAD
2. [04 - Diagrama de Classes](04-diagrama-classes.md) - Estrutura
3. [05 - Sequência de Requisição](05-sequencia-requisicao.md) - Debug profundo

### 🏗️ **Arquiteto de Solução**
1. [01 - Arquitetura Geral](01-arquitetura-geral.md) - Visão técnica
2. [03 - Pipeline BMAD](03-pipeline-bmad.md) - Padrão BMAD
3. [04 - Diagrama de Classes](04-diagrama-classes.md) - Design patterns
4. [06 - Estrutura de Pacotes](06-estrutura-pacotes.md) - Organização

### 🧪 **QA / Tester**
1. [02 - Fluxo de Execução](02-fluxo-execucao.md) - Entender workflows
2. [05 - Sequência de Requisição](05-sequencia-requisicao.md) - Tracing
3. [01 - Arquitetura Geral](01-arquitetura-geral.md) - Componentes

### 🔧 **DevOps / Infra**
1. [01 - Arquitetura Geral](01-arquitetura-geral.md) - Componentes
2. [06 - Estrutura de Pacotes](06-estrutura-pacotes.md) - Configuração

---

## 🎯 Leitura Rápida (15 minutos)

1. [01 - Arquitetura Geral](01-arquitetura-geral.md) (2 min)
2. [02 - Fluxo de Execução](02-fluxo-execucao.md) (5 min)
3. [03 - Pipeline BMAD](03-pipeline-bmad.md) (8 min)

**Resultado:** Você entenderá como o projeto funciona de ponta a ponta!

---

## 📊 Leitura Completa (45 minutos)

Leia os 6 documentos em sequência:

1. ✅ [01 - Arquitetura Geral](01-arquitetura-geral.md)
2. ✅ [02 - Fluxo de Execução](02-fluxo-execucao.md)
3. ✅ [03 - Pipeline BMAD](03-pipeline-bmad.md)
4. ✅ [04 - Diagrama de Classes](04-diagrama-classes.md)
5. ✅ [05 - Sequência de Requisição](05-sequencia-requisicao.md)
6. ✅ [06 - Estrutura de Pacotes](06-estrutura-pacotes.md)

**Resultado:** Você será um especialista na arquitetura do projeto!

---

## 🔍 Busca Rápida

### "Como funciona a criação de um cenário?"
→ [02 - Fluxo de Execução](02-fluxo-execucao.md)

### "Quais são os agentes e o que fazem?"
→ [03 - Pipeline BMAD](03-pipeline-bmad.md)

### "Como adicionar um novo agente?"
→ [03 - Pipeline BMAD](03-pipeline-bmad.md) + [04 - Diagrama de Classes](04-diagrama-classes.md)

### "Onde está a classe X?"
→ [06 - Estrutura de Pacotes](06-estrutura-pacotes.md)

### "Por que minha requisição falha?"
→ [05 - Sequência de Requisição](05-sequencia-requisicao.md)

### "Como debugar uma requisição?"
→ [05 - Sequência de Requisição](05-sequencia-requisicao.md)

### "Qual é a diferença entre os workflows?"
→ [02 - Fluxo de Execução](02-fluxo-execucao.md) ou [03 - Pipeline BMAD](03-pipeline-bmad.md)

### "Como é a arquitetura geral?"
→ [01 - Arquitetura Geral](01-arquitetura-geral.md)

---

## 📈 Complexidade por Documento

```
Complexidade
     ▲
     │     ╔════════════════════════════════════╗
     │     ║ 04 - Diagrama de Classes (⭐⭐⭐⭐)  ║
     │     ║ 05 - Sequência de Requisição (⭐⭐⭐) ║
     │     ║ 03 - Pipeline BMAD (⭐⭐⭐)         ║
     │     ╠════════════════════════════════════╣
     │     ║ 02 - Fluxo de Execução (⭐⭐)        ║
     │     ║ 06 - Estrutura de Pacotes (⭐)     ║
     │     ╠════════════════════════════════════╣
     │     ║ 01 - Arquitetura Geral (⭐)        ║
     │     ╚════════════════════════════════════╝
     │
     └─────────────────────────────────────► Tipo de Leitor
   Iniciante                        Especialista
```

---

## 🛠️ Ferramentas Úteis

### Para Visualizar Diagramas Mermaid

- **GitHub:** Visualização nativa (recomendado)
- **VS Code:** Extensão "Markdown Preview Mermaid Support"
- **Notion:** Copy-paste e use integração Mermaid
- **Draw.io:** Importe e edite diagramas

### Para Estudar

- **Markdown Viewer:** Para ler offline
- **VS Code:** Abra em split screen com o código
- **GitHub Pages:** Hospede esta doc

---

## ✅ Checklist de Onboarding

Quando uma pessoa nova entra no projeto, siga:

- [ ] Ler [01 - Arquitetura Geral](01-arquitetura-geral.md)
- [ ] Ler [06 - Estrutura de Pacotes](06-estrutura-pacotes.md)
- [ ] Ler [02 - Fluxo de Execução](02-fluxo-execucao.md)
- [ ] Explorar o código seguindo [06 - Estrutura de Pacotes](06-estrutura-pacotes.md)
- [ ] Ler [04 - Diagrama de Classes](04-diagrama-classes.md)
- [ ] Criar seu primeiro cenário na UI
- [ ] Fazer debug seguindo [05 - Sequência de Requisição](05-sequencia-requisicao.md)
- [ ] Ler [03 - Pipeline BMAD](03-pipeline-bmad.md) para entender os agentes
- [ ] Você agora é produtivo! 🎉

**Tempo total:** ~2-3 horas

---

## 📈 Estatísticas da Documentação

| Métrica | Valor |
|---------|-------|
| **Documentos** | 6 |
| **Diagramas Mermaid** | 40+ |
| **Linhas totais** | ~8,000 |
| **Páginas (A4)** | ~35 |
| **Tempo para ler tudo** | ~45 minutos |
| **Tempo para entender** | ~2-3 horas |

---

## 🔄 Versão e Manutenção

- **Versão:** 2.0
- **Data:** Julho 2024
- **Status:** ✅ Completa e Atualizada
- **Próxima revisão:** Quando houver mudanças maiores na arquitetura

### Como Manter Atualizado

Quando houver mudanças na arquitetura:

1. Identifique qual documento é afetado
2. Atualize o documento principal
3. Atualize documentos relacionados
4. Revise links cruzados
5. Atualize diagramas mermaid

---

## 🤝 Contribuindo

Se você:
- ✅ Encontrou um erro
- ✅ Tem uma pergunta frequente
- ✅ Quer melhorar um diagrama
- ✅ Quer adicionar um novo tópico

**Abra uma issue ou pull request!**

---

## 📞 Contato e Suporte

**Dúvidas sobre a documentação?**
1. Consulte o documento relevante
2. Use a seção "Busca Rápida" acima
3. Abra uma issue no repositório

---

## 🎯 Próximas Leituras

Depois de ler esta documentação de arquitetura, explore:

- **IMPLEMENTACAO-BMAD.md** - Detalhes da implementação
- **GUIA-DE-USO-BMAD.md** - Como usar o sistema
- **TESTES-UNITARIOS-BMAD.md** - Estratégia de testes
- **agents/workflows/*.md** - Detalhes de cada workflow

---

**Bem-vindo ao projeto! Você está em boas mãos com esta documentação.** 🚀

---

**Criado por:** Copilot CLI  
**Data:** Julho 2024  
**Projeto:** Criar Cenário de Testes - BMAD Architecture  
**Status:** ✅ Documentação Completa
