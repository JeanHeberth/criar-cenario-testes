# 🎨 DIAGRAMAS BMAD - Múltiplos Formatos

## 📁 Arquivos Disponíveis

Este diretório contém os diagramas da arquitetura BMAD em diferentes formatos para diferentes ferramentas:

---

## 1️⃣ **arquitetura-bmad.mermaid.md** ⭐ (RECOMENDADO)

**Formato:** Mermaid Diagram
**Melhor para:** Visualização rápida online, GitHub, VS Code

**Como usar:**
```bash
# Ver no VS Code (com extensão)
code arquitetura-bmad.mermaid.md
# Pressione Ctrl+Shift+V

# Ver online
https://mermaid.live/
# Cole o conteúdo do arquivo
```

**Vantagens:**
- ✅ Rápido e fácil
- ✅ Renderiza no GitHub/GitLab automaticamente
- ✅ Pode exportar para PNG/SVG/PDF
- ✅ Código legível e editável

---

## 2️⃣ **arquitetura-bmad.drawio**

**Formato:** Draw.io XML
**Melhor para:** Edição visual completa

**Como usar:**
```bash
# Online
1. Acesse: https://app.diagrams.net/
2. File → Open from → Device
3. Selecione: arquitetura-bmad.drawio
4. Edite visualmente
5. File → Export as → PNG/SVG/PDF

# Desktop
1. Baixe: https://www.diagrams.net/
2. Abra: arquitetura-bmad.drawio
3. Edite e exporte
```

**Vantagens:**
- ✅ Editor visual completo
- ✅ Arrastar e soltar
- ✅ Muitos shapes disponíveis
- ✅ Exporta para múltiplos formatos

---

## 3️⃣ **arquitetura-bmad.plantuml.md** (Em breve)

**Formato:** PlantUML
**Melhor para:** Documentação técnica, UML

**Como usar:**
```bash
# Online
https://www.plantuml.com/plantuml/

# VS Code
Instalar extensão PlantUML
```

---

## 🎯 **QUAL FORMATO ESCOLHER?**

| Necessidade | Formato Recomendado |
|-------------|---------------------|
| **Ver rapidamente** | Mermaid (.mermaid.md) + mermaid.live |
| **Editar visualmente** | Draw.io (.drawio) |
| **Incluir no README GitHub** | Mermaid (.mermaid.md) |
| **Apresentar em reunião** | Draw.io → Export PNG |
| **Documentação técnica** | PlantUML (.plantuml.md) |
| **Imprimir** | Draw.io → Export PDF |

---

## 📖 **INSTRUÇÕES DETALHADAS**

### **🌐 Visualizar Mermaid Online (Mais Fácil):**

1. Abra: **arquitetura-bmad.mermaid.md**
2. Copie todo o conteúdo (incluindo \`\`\`mermaid)
3. Acesse: https://mermaid.live/
4. Cole o código na área de edição
5. O diagrama aparece automaticamente!
6. Clique em "Actions" para exportar:
   - PNG (imagem)
   - SVG (vetorial)
   - PDF (documento)

---

### **🎨 Editar no Draw.io:**

1. Acesse: https://app.diagrams.net/
2. Clique em: **File → Open from → Device**
3. Selecione: **arquitetura-bmad.drawio**
4. Edite o diagrama:
   - Arraste elementos
   - Adicione/remova componentes
   - Mude cores e estilos
5. Exporte:
   - **File → Export as → PNG** (compartilhar)
   - **File → Export as → SVG** (alta qualidade)
   - **File → Export as → PDF** (apresentação)

---

### **📝 Editar no VS Code:**

**Pré-requisito:** Instalar extensão

```bash
# Extensão para Mermaid
code --install-extension bierner.markdown-mermaid
```

**Uso:**
1. Abra: **arquitetura-bmad.mermaid.md** no VS Code
2. Pressione: **Ctrl+Shift+V** (Windows/Linux) ou **Cmd+Shift+V** (Mac)
3. Veja o preview renderizado
4. Edite o código e veja mudanças em tempo real

---

## 🚀 **DICA: MELHOR WORKFLOW**

Para **criar apresentações** ou **documentar**:

```bash
# 1. Visualizar rapidamente
Abra arquitetura-bmad.mermaid.md no mermaid.live

# 2. Exportar PNG
Actions → Export PNG

# 3. Adicionar em documentação
Insira PNG no PowerPoint, Word, ou README
```

Para **editar/customizar**:

```bash
# 1. Abrir no Draw.io
https://app.diagrams.net/
File → Open → arquitetura-bmad.drawio

# 2. Editar visualmente
Arraste, adicione, customize

# 3. Salvar e exportar
File → Save
File → Export as PNG/SVG/PDF
```

---

## 📊 **COMPONENTES DO DIAGRAMA**

### **Camadas principais:**

1. **FRONTEND ANGULAR**
   - Formulário de criação
   - Dropdown de workflows ⭐ (NOVO)
   - Upload de PDFs
   - Botão de geração
   - Visualização de resultados

2. **BACKEND SPRING BOOT**
   - CenarioController
   - CenarioService
   - QaWorkflowService (Orquestrador)

3. **WORKFLOWS**
   - COMPLETO (6 agentes, ~2 min)
   - RAPIDO (4 agentes, ~1 min)
   - REVISAO (2 agentes, ~30s)
   - REGRESSAO (4 agentes, ~1 min)

4. **PIPELINE DE AGENTES**
   - RequirementAnalysisAgent
   - TranscriptAnalysisAgent
   - TestPlanAgent
   - TestScenarioAgent
   - RedundancyReviewAgent
   - ZephyrFormatterAgent

5. **PERSISTÊNCIA**
   - MongoDB

---

## 🎨 **CÓDIGO DE CORES**

| Cor | Significado |
|-----|-------------|
| 🟡 Amarelo | Formulários, Entrada de dados |
| 🟢 Verde | Processamento, Workflows rápidos |
| 🟣 Roxo | Orquestração, Workflows especiais |
| 🔵 Azul | Agentes, Componentes principais |
| 🔴 Vermelho | Ações críticas, Persistência |

---

## 💡 **DICAS DE USO**

### **Para Desenvolvedores:**
- Use Mermaid para manter diagrama no controle de versão
- Código é mais fácil de revisar em pull requests
- Auto-renderiza no GitHub

### **Para Apresentações:**
- Exporte PNG de alta resolução do Mermaid
- Ou use Draw.io para slides customizados

### **Para Documentação:**
- Inclua Mermaid diretamente no README.md
- GitHub renderiza automaticamente

---

## 📞 **PRECISA DE AJUDA?**

**Mermaid não renderiza?**
- Verifique se copiou \`\`\`mermaid no início
- Use https://mermaid.live/ online

**Draw.io não abre?**
- Arquivo pode estar corrompido
- Crie novo diagrama e importe componentes

**Quer outro formato?**
- PlantUML, Graphviz, etc.
- Peça para gerar

---

**Criado por:** Jean Heberth  
**Data:** 29/07/2026  
**Versão:** 1.0  
**Última atualização:** Arquivos Mermaid e Draw.io criados
