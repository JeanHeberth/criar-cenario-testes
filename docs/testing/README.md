# 🧪 Documentação de Testes

Testes unitários, estratégia de testes e resultados.

---

## 📚 Documentos

### [TESTES-UNITARIOS.md](TESTES-UNITARIOS.md)
Estratégia completa de testes unitários.

- Padrão AAA (Arrange, Act, Assert)
- 9 classes de teste criadas
- 52 testes unitários
- Cobertura por componente
- Mocks e stubs utilizados
- Como adicionar novos testes

**Tempo:** ~15 KB | **Para:** QAs, Devs

---

### [RESULTADO-TESTES.md](RESULTADO-TESTES.md)
Resultados e estatísticas dos testes.

- Execução: 52/52 testes passando ✅
- Estatísticas detalhadas
- Tempo de execução
- Problemas encontrados e corrigidos
- Relatório HTML gerado
- Comandos úteis

**Tempo:** ~5 KB | **Para:** QAs, Gestores, Devs

---

## 🔗 Links Rápidos

| Documento | Objetivo | Tempo |
|-----------|----------|-------|
| [TESTES-UNITARIOS.md](TESTES-UNITARIOS.md) | Estratégia | 20 min |
| [RESULTADO-TESTES.md](RESULTADO-TESTES.md) | Resultados | 5 min |

---

## 🎯 Por Persona

### 🧪 QA / Tester
→ Leia [TESTES-UNITARIOS.md](TESTES-UNITARIOS.md)  
→ Veja [RESULTADO-TESTES.md](RESULTADO-TESTES.md)

### 👨‍💻 Desenvolvedor
→ Leia [TESTES-UNITARIOS.md](TESTES-UNITARIOS.md)

### 👔 Gestor / PO
→ Veja [RESULTADO-TESTES.md](RESULTADO-TESTES.md)

---

## 🚀 Rodar Testes

```bash
# Rodar todos os testes
mvn clean test

# Rodar teste específico
mvn clean test -Dtest=CenarioServiceTest

# Com relatório
mvn clean test
# Resultado em: target/surefire-reports/
```

---

**[⬅️ Voltar para Documentação Principal](../README.md)**
