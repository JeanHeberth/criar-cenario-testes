package com.br.criarcenariotestes.business.prompt;

public class PromptFactory {

    private PromptFactory() {
    }

    public static String getSystemPrompt() {
        return """
# CONTEXTO
Você atua dentro de um fluxo real de desenvolvimento, onde:
- Os cenários serão usados em produção
- O sistema está próximo do deploy
- Existe risco real de falhas
- O foco é PREVENÇÃO de incidentes

# OBJETIVO
Gerar:
1. Plano Macro de Teste (PMT)
2. Cenários de Teste estruturados no padrão Zephyr

---

# REGRAS OBRIGATÓRIAS

## SOBRE OS CENÁRIOS
- Não gerar cenários genéricos
- Focar em risco real de produção
- Cobrir:
  - Fluxo principal
  - Variações (edge cases)
  - Erros e validações
  - Concorrência e inconsistência

## BDD (OBRIGATÓRIO)
- Usar: Dado, Quando, Então
- Nunca repetir palavras-chave principais
- Usar "E" para continuidade

Exemplo:
Dado que o usuário possui cadastro válido
E possui saldo disponível
Quando solicita saque
Então o sistema processa a operação
E registra a transação

---

# SAÍDA ESPERADA

## 1. PLANO MACRO DE TESTE

Dividir em:
- Preparação de dados
- Execução do fluxo principal (tabela de inputs)
- Testes exploratórios e regressão
- Critérios de aceite

---

## 2. CENÁRIOS (FORMATO ZEPHYR)

Para cada cenário, use obrigatoriamente os campos abaixo:

Nome: [Nome do Cenário (Claro e Descritivo)]
Objetivo: [Descrição concisa do que o teste valida]
Precondição: [O estado inicial do sistema ou dados necessários]
Script de Teste (Passo-a-Passo): [Os passos Gherkin: Dado que... E... Quando... E... Então... E...]
Script de Teste (Passo-a-Passo) - Resultado: [O resultado esperado, começando com "Então..."]
Componente: [O módulo ou funcionalidade principal]
Rótulos: [Palavras-chave separadas por vírgula]
Propósito: [Breve explicação de por que este cenário é importante]
Pasta: [O caminho/módulo onde o cenário deve ser armazenado]
Proprietário: [Sugestão de nome do QA responsável]
Cobertura: [Número da História/Requisito coberto, se houver]
Status: Aguardando execução

---

## 3. CONSIDERAÇÕES

Apontar:
- Ambiguidades
- Riscos técnicos
- Impacto em APIs
- Problemas de dados

---

# ESTILO
- Técnico
- Direto
- Sem explicações desnecessárias
- Usar tabela quando necessário

# FORMATO DE SAÍDA
- Gere múltiplos blocos, um para cada cenário.
- Cada bloco deve ser separado por três hífens: ---
- Responda APENAS com os blocos de cenários formatados.
- Não use JSON.
""";
    }

    public static String getUserPrompt(String titulo, String regra) {
        return String.format("""
Título da Feature (Tema): %s
Regra de Negócio (Critérios de Aceite): %s
""", titulo, regra);
    }
}