# 📚 Documentação - Índice Principal

Bem-vindo à documentação completa do projeto **Criar Cenário de Testes BMAD**.

---

## 🚀 Comece Aqui

### ⚡ [01-QUICK-START.md](01-QUICK-START.md)
**Tempo:** 5 minutos

Início rápido do projeto em 3 comandos:
- Subir backend e frontend
- Criar seu primeiro cenário
- Troubleshooting básico

👉 **Comece por aqui se é a primeira vez!**

---

## 📖 Documentação Estruturada

### 🏛️ [architecture/](architecture/)
**Documentação Arquitetural Completa**

6 documentos detalhados sobre como o projeto foi arquitetado:
- 01 - Arquitetura Geral
- 02 - Fluxo de Execução
- 03 - Pipeline BMAD
- 04 - Diagrama de Classes
- 05 - Sequência de Requisição
- 06 - Estrutura de Pacotes

**Tempo:** 45 minutos | **Complexidade:** ⭐ a ⭐⭐⭐⭐

👉 **Leia se precisa entender como o projeto funciona**

---

### 📖 [guides/](guides/)
**Guias de Uso e Implementação**

- **GUIA-DE-USO.md** - Como usar via interface e API
- **IMPLEMENTACAO.md** - Detalhes técnicos
- **IMPACTO-FRONTEND.md** - Análise de impacto

👉 **Leia se vai usar ou desenvolver o projeto**

---

### 🧪 [testing/](testing/)
**Documentação de Testes**

- **TESTES-UNITARIOS.md** - Estratégia de testes (52 testes)
- **RESULTADO-TESTES.md** - Resultados e estatísticas

👉 **Leia se vai testar ou manter os testes**

---

### 📊 [diagrams/](diagrams/)
**Diagramas e Visualizações**

- **ARQUITETURA.md** - Diagrama visual da arquitetura
- **arquitetura-bmad.mermaid.md** - Código Mermaid editável

👉 **Use como referência visual**

---

### 🤖 [agents/](agents/)
**Documentação de Agentes**

- **gerador_cenarios_testes.agent.md** - Agente de geração
- **robot_framework_*.agent.md** - Agentes de teste
- **workflows/** - Documentação de workflows

👉 **Leia se vai trabalhar com agentes**

---

## 🗺️ Navegação por Persona

### 👔 Gestor / Product Owner (15 min)
```
1. 01-QUICK-START.md
2. architecture/01-arquitetura-geral.md
3. diagrams/ARQUITETURA.md
✓ Você entende o projeto!
```

### 👨‍💻 Desenvolvedor Backend (2 horas)
```
1. 01-QUICK-START.md
2. architecture/06-estrutura-pacotes.md
3. guides/IMPLEMENTACAO.md
4. architecture/04-diagrama-classes.md
5. architecture/02-fluxo-execucao.md
✓ Você está produtivo!
```

### 👨‍💻 Desenvolvedor Frontend (1 hora)
```
1. 01-QUICK-START.md
2. guides/GUIA-DE-USO.md (seção UI)
3. guides/IMPACTO-FRONTEND.md
✓ Você sabe o que mexer!
```

### 🧪 QA / Tester (45 min)
```
1. 01-QUICK-START.md
2. testing/TESTES-UNITARIOS.md
3. guides/GUIA-DE-USO.md
✓ Você consegue testar!
```

### 🏗️ Arquiteto (1 hora)
```
1. architecture/01-arquitetura-geral.md
2. architecture/03-pipeline-bmad.md
3. architecture/04-diagrama-classes.md
✓ Você entende o design!
```

---

## 📊 Estrutura de Pastas

```
docs/
├── README.md                 (Este arquivo)
├── 01-QUICK-START.md         (Comece aqui)
│
├── architecture/             (Documentação arquitetural)
│   ├── 01-arquitetura-geral.md
│   ├── 02-fluxo-execucao.md
│   ├── 03-pipeline-bmad.md
│   ├── 04-diagrama-classes.md
│   ├── 05-sequencia-requisicao.md
│   ├── 06-estrutura-pacotes.md
│   └── QUICK-REFERENCE.md
│
├── guides/                   (Guias práticos)
│   ├── GUIA-DE-USO.md
│   ├── IMPLEMENTACAO.md
│   └── IMPACTO-FRONTEND.md
│
├── testing/                  (Testes)
│   ├── TESTES-UNITARIOS.md
│   └── RESULTADO-TESTES.md
│
├── diagrams/                 (Visualizações)
│   ├── ARQUITETURA.md
│   └── arquitetura-bmad.mermaid.md
│
└── agents/                   (Agentes & Workflows)
    ├── gerador_cenarios_testes.agent.md
    ├── robot_framework_*.agent.md
    └── workflows/
        ├── workflow-completo.md
        ├── workflow-rapido.md
        └── workflow-revisao.md
```

---

## 🔍 Busca Rápida

| Pergunta | Leia |
|----------|------|
| Como começo? | [01-QUICK-START.md](01-QUICK-START.md) |
| Como funciona? | [architecture/02-fluxo-execucao.md](architecture/02-fluxo-execucao.md) |
| O que é BMAD? | [architecture/03-pipeline-bmad.md](architecture/03-pipeline-bmad.md) |
| Onde está classe X? | [architecture/06-estrutura-pacotes.md](architecture/06-estrutura-pacotes.md) |
| Como debugo? | [architecture/05-sequencia-requisicao.md](architecture/05-sequencia-requisicao.md) |
| Como uso via API? | [guides/GUIA-DE-USO.md](guides/GUIA-DE-USO.md) |
| Como implementar? | [guides/IMPLEMENTACAO.md](guides/IMPLEMENTACAO.md) |
| Qual o impacto frontend? | [guides/IMPACTO-FRONTEND.md](guides/IMPACTO-FRONTEND.md) |
| Como são os testes? | [testing/TESTES-UNITARIOS.md](testing/TESTES-UNITARIOS.md) |
| Quais os resultados? | [testing/RESULTADO-TESTES.md](testing/RESULTADO-TESTES.md) |

---

## ⏱️ Tempo de Leitura

| Documento | Tempo | Dificuldade |
|-----------|-------|------------|
| 01-QUICK-START | 5 min | ⭐ |
| architecture/ | 45 min | ⭐ a ⭐⭐⭐⭐ |
| guides/ | 30 min | ⭐⭐ |
| testing/ | 15 min | ⭐⭐ |
| agents/ | 20 min | ⭐⭐⭐ |

**Total:** ~2-3 horas para ler tudo

---

## 📋 Checklist de Onboarding

Quando você entra no projeto:

- [ ] Ler [01-QUICK-START.md](01-QUICK-START.md)
- [ ] Ler [architecture/01-arquitetura-geral.md](architecture/01-arquitetura-geral.md)
- [ ] Explorar código do projeto
- [ ] Ler documentação específica para sua role
- [ ] Rodar o projeto localmente
- [ ] Criar seu primeiro cenário
- [ ] Consultar documentação quando tiver dúvidas

---

## 🆘 Perguntas Frequentes

**P: Por onde começo?**  
R: Leia [01-QUICK-START.md](01-QUICK-START.md) (5 min)

**P: Preciso aprender tudo?**  
R: Não! Leia apenas a documentação relevante para sua função.

**P: Onde está a documentação do frontend?**  
R: Veja a pasta `front/gerar-cenario-teste-app/` no repositório

**P: Como editar a documentação?**  
R: Faça um PR com as alterações nos arquivos `.md`

**P: Há alguma documentação visual?**  
R: Sim! Veja [diagrams/](diagrams/) e [architecture/QUICK-REFERENCE.md](architecture/QUICK-REFERENCE.md)

---

## 🔄 Manutenção da Documentação

Se você alterar o código, atualize a documentação correspondente:

1. **Alterar estrutura de código?** → Atualize [architecture/06-estrutura-pacotes.md](architecture/06-estrutura-pacotes.md)
2. **Alterar fluxo de execução?** → Atualize [architecture/02-fluxo-execucao.md](architecture/02-fluxo-execucao.md)
3. **Adicionar novo agente?** → Crie arquivo em [agents/](agents/)
4. **Alterar testes?** → Atualize [testing/](testing/)

---

## 📞 Precisa de Ajuda?

1. **Procure na documentação** - Use a seção "Busca Rápida" acima
2. **Procure em um documento específico** - Use Ctrl+F
3. **Consulte o código** - O código está bem comentado
4. **Abra uma issue** - Se não encontrar resposta

---

**Bem-vindo ao projeto! Divirta-se! 🚀**

---

**Status:** ✅ Documentação Completa e Organizada  
**Última atualização:** Julho 2024  
**Versão:** 2.0
