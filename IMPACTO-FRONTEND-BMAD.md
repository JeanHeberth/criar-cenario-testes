# 🔍 ANÁLISE DE IMPACTO NO FRONTEND

## ✅ **RESPOSTA RÁPIDA: NÃO PRECISA MEXER NO FRONTEND!**

---

## 🎯 **Backward Compatibility 100% Mantida**

### **Endpoint Principal** - `POST /cenario`

**ANTES:**
```json
{
  "titulo": "string",
  "regraDeNegocio": "string",
  "agent": "string"
}
```

**DEPOIS:**
```json
{
  "titulo": "string",
  "regraDeNegocio": "string",
  "agent": "string",
  "workflowType": "COMPLETO"  // ← OPCIONAL!
}
```

✅ **O campo `workflowType` é OPCIONAL**
✅ **Se não enviado, assume valor DEFAULT = `COMPLETO`**
✅ **Seu Angular continua funcionando exatamente como antes**

---

## 📋 **Resumo das Alterações**

| Item | Antes | Depois | Quebra? |
|------|-------|--------|---------|
| **CenarioRequest** | 3 campos | 4 campos | ❌ NÃO |
| **CenarioResponse** | 5 campos | 5 campos | ❌ NÃO |
| **POST /cenario** | Funciona | Funciona | ❌ NÃO |
| **GET /cenario** | Funciona | Funciona | ❌ NÃO |
| **GET /cenario/{id}** | Funciona | Funciona | ❌ NÃO |
| **DELETE /cenario/{id}** | Funciona | Funciona | ❌ NÃO |
| **POST /cenario/com-pdf** | Funciona | Funciona | ❌ NÃO |

---

## 🔧 **Como Funciona a Backward Compatibility**

### **No Backend (Java)**

```java
public record CenarioRequest(
    String titulo,
    String regraDeNegocio,
    String agent,
    WorkflowType workflowType  // ← Novo campo
) {
    // Construtor de compatibilidade
    public CenarioRequest(String titulo, String regraDeNegocio, String agent) {
        this(titulo, regraDeNegocio, agent, WorkflowType.COMPLETO);
        //                                    ↑ Valor DEFAULT
    }
}
```

### **No Frontend (Angular)**

✅ **Seu código atual continua funcionando:**
```typescript
// Seu código Angular ATUAL (não precisa mexer!)
const request = {
  titulo: this.form.value.titulo,
  regraDeNegocio: this.form.value.regraDeNegocio,
  agent: this.form.value.agent
  // workflowType NÃO precisa ser enviado!
};

this.http.post<CenarioResponse>('/cenario', request).subscribe(...);
```

---

## 🚀 **Funcionalidades NOVAS (Opcionais)**

Se você quiser **aproveitar** os novos workflows, pode fazer:

### **1. Novo Endpoint para listar workflows**

```typescript
// GET /cenario/workflows
interface WorkflowInfo {
  tipo: string;
  nome: string;
  descricao: string;
  agentes: string[];
}

this.http.get<WorkflowInfo[]>('/cenario/workflows').subscribe(workflows => {
  // ["COMPLETO", "RAPIDO", "REVISAO", "REGRESSAO"]
});
```

### **2. Enviar workflowType no request (opcional)**

```typescript
// Adicionar dropdown no formulário
const request = {
  titulo: this.form.value.titulo,
  regraDeNegocio: this.form.value.regraDeNegocio,
  agent: this.form.value.agent,
  workflowType: this.form.value.workflowType || 'COMPLETO'  // Novo campo
};
```

---

## 📊 **Tipos de Workflow Disponíveis**

| WorkflowType | Descrição | Agentes | Velocidade |
|--------------|-----------|---------|------------|
| **COMPLETO** | Pipeline completo com todos os agentes | 6 | 🐌 Lento |
| **RAPIDO** | Pula análise de ata de reunião | 4 | 🐇 Rápido |
| **REVISAO** | Apenas revisa cenários existentes | 2 | 🚀 Muito Rápido |
| **REGRESSAO** | Análise + revisão de regressão | 4 | 🏃 Médio |

---

## ✅ **Checklist de Compatibilidade**

- [x] Contratos de API não mudaram
- [x] CenarioResponse é idêntico
- [x] Novos campos são opcionais
- [x] Valores default definidos
- [x] Endpoints antigos funcionam
- [x] Payload antigo aceito
- [x] Sem breaking changes

---

## 🎯 **Cenários de Teste no Frontend**

### ✅ **Cenário 1: Sem alterar nada**
```typescript
// Seu código Angular atual
const request = {
  titulo: "Teste",
  regraDeNegocio: "Regra",
  agent: "gpt-4o"
};

// ✅ Vai funcionar normalmente!
// ✅ Vai usar WorkflowType.COMPLETO por padrão
```

### ✅ **Cenário 2: Com novo campo (futuro)**
```typescript
const request = {
  titulo: "Teste",
  regraDeNegocio: "Regra",
  agent: "gpt-4o",
  workflowType: "RAPIDO"  // ← Novo campo opcional
};

// ✅ Vai usar o workflow rápido
// ✅ Mais rápido, menos completo
```

---

## 🎨 **Sugestão de UI (Opcional)**

Se quiser adicionar seleção de workflow no futuro:

```html
<mat-form-field>
  <mat-label>Tipo de Workflow</mat-label>
  <mat-select formControlName="workflowType">
    <mat-option value="COMPLETO">Completo (6 agentes)</mat-option>
    <mat-option value="RAPIDO">Rápido (4 agentes)</mat-option>
    <mat-option value="REVISAO">Revisão (2 agentes)</mat-option>
    <mat-option value="REGRESSAO">Regressão (4 agentes)</mat-option>
  </mat-select>
</mat-form-field>
```

---

## 🚫 **O que NÃO MUDOU**

### ❌ Estrutura de resposta
```json
{
  "id": "string",
  "titulo": "string",
  "regraDeNegocio": "string",
  "criteriosAceitacao": "string",
  "cenarios": [...]
}
```
**IDÊNTICO!**

### ❌ Estrutura de CenarioItem
```json
{
  "nome": "string",
  "objetivo": "string",
  "precondicao": "string",
  "scriptTeste": "string",
  "resultadoEsperado": "string"
}
```
**IDÊNTICO!**

---

## 📝 **Resumo Final**

| Pergunta | Resposta |
|----------|----------|
| Preciso alterar o Angular? | ❌ **NÃO** |
| Meu código atual funciona? | ✅ **SIM** |
| Posso usar novos workflows? | ✅ **SIM** (opcional) |
| Preciso testar algo? | ✅ **SIM** (regressão básica) |
| Tem breaking change? | ❌ **NÃO** |

---

## 🧪 **Teste de Regressão Recomendado**

1. Suba o backend com as alterações
2. Rode seu Angular sem alterar nada
3. Crie um cenário de teste
4. Verifique se funciona normalmente

**Resultado esperado:** ✅ Tudo deve funcionar exatamente como antes!

---

## 🎉 **Conclusão**

✅ **Zero impacto no frontend atual**  
✅ **Backward compatibility 100%**  
✅ **Pode fazer deploy do backend sem medo**  
✅ **Novas features disponíveis quando quiser usar**

---

**Autor:** Jean Heberth  
**Data:** 29/07/2026  
**Resumo:** Alterações BMAD são **totalmente transparentes** para o frontend Angular
