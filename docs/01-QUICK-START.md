# 🎯 QUICK START - BMAD

## ⚡ Início Rápido (5 minutos)

---

## 1️⃣ **SUBIR O BACKEND**

```bash
cd /Users/jeanheberth/Development/api/criar-cenario-testes
./gradlew bootRun
```

✅ **Aguarde:** "Started CriarCenarioTestesApplication"

---

## 2️⃣ **SUBIR O FRONTEND**

```bash
cd /Users/jeanheberth/Development/front/gerar-cenario-teste-app
npm start
```

✅ **Abra:** http://localhost:4200

---

## 3️⃣ **CRIAR SEU PRIMEIRO CENÁRIO**

**No navegador:**

1. **Título:** `Login de Usuário`

2. **Regra de Negócio:**
   ```
   Sistema deve permitir login com email e senha.
   Após 3 tentativas incorretas, bloquear por 15 min.
   ```

3. **Tipo de Workflow:** `Geração Rápida` (RAPIDO)

4. Clique: **"🚀 Gerar Cenário"**

5. ⏳ Aguarde ~1 minuto

6. ✅ Sucesso: "Cenario gerado com sucesso!"

---

## 4️⃣ **VISUALIZAR RESULTADOS**

Clique: **"👀 Visualizar Cenários"**

**Você verá:**
- Cenário criado
- Casos de teste detalhados
- Opções de exportar

---

## 🎨 **WORKFLOWS DISPONÍVEIS**

| Workflow | Uso | Tempo | Agentes |
|----------|-----|-------|---------|
| **COMPLETO** | Análise detalhada | ~2 min | 6 |
| **RAPIDO** | Desenvolvimento rápido | ~1 min | 4 |
| **REVISAO** | Apenas revisar | ~30s | 2 |
| **REGRESSAO** | Testes de regressão | ~1 min | 4 |

---

## 📖 **DOCUMENTAÇÃO COMPLETA**

```
GUIA-DE-USO-BMAD.md
```

---

## 🆘 **PROBLEMAS?**

### Dropdown vazio?
```bash
# Verificar backend
curl http://localhost:8080/cenario/workflows
```

### Erro ao gerar?
```bash
# Ver logs do backend (terminal onde rodou bootRun)
```

### Build falhou?
```bash
cd /Users/jeanheberth/Development/api/criar-cenario-testes
./gradlew clean build
```

---

## ✅ **PRONTO!**

Agora você pode:
- Gerar cenários com 4 workflows diferentes
- Anexar PDFs de documentação
- Exportar para Excel/PDF/Zephyr
- Integrar com Jira

**Explore e divirta-se! 🚀**
