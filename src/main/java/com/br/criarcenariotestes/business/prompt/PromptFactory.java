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
                 
                 ⚠️ AVISO CRÍTICO: Respostas com APENAS 2-3 cenários são INACEITÁVEIS.
                 Qualidade da geração é medida por COBERTURA COMPLETA, não por tamanho.
                 Se retornar poucos cenários, você não completou o trabalho.
                 
                 # OBJETIVO
                 Gerar:
                 1. Plano Macro de Teste (PMT)
                 2. Cenários de Teste estruturados no padrão Zephyr Scale (MÍNIMO 6-10 cenários)
                 3. Cenários parametrizados usando variáveis quando fizer sentido
                
                ---
                
                 # REGRAS OBRIGATÓRIAS
                 
                 ## QUANTIDADE DE CENÁRIOS (CRÍTICO)
                 - Gere a quantidade de cenários necessária para cobrir completamente os riscos da funcionalidade.
                 - MÍNIMO OBRIGATÓRIO: 6-10 cenários por regra de negócio (não parar com 2-3).
                 - Não limite artificialmente a quantidade.
                 - Evite cenários redundantes.
                 - Use variáveis sempre que possível para reduzir repetição.
                 - Priorize qualidade, cobertura e risco ao invés de volume.
                 
                 ### Cobertura Mínima Esperada:
                 - 1-2 cenários positivos (fluxo principal com variações)
                 - 2-3 cenários negativos (validações, erros, rejeições)
                 - 1-2 cenários de borda (limites, campos obrigatórios, tamanho máximo/mínimo)
                 - 1-2 cenários de permissão/integração (segurança, autorização)
                 - 1-2 cenários exploratórios ou contextuais (baseados na regra específica)
                 
                 Se houver menos de 6 cenários após verificação, AUMENTAR cobertura.
                
                ## SOBRE OS CENÁRIOS
                - Não gerar cenários genéricos.
                - Focar em risco real de produção.
                - Cobrir:
                  - Fluxo principal
                  - Variações e edge cases
                  - Erros e validações
                  - Concorrência e inconsistência
                  - Regras implícitas identificadas em reuniões/transcrições
                  - Impactos em integrações, APIs, dados e permissões
                
                ## EVITAR REPETIÇÃO
                - Não gere vários cenários iguais mudando apenas massa de dados.
                - Quando a diferença entre cenários for apenas valor de entrada, gere um único cenário parametrizado.
                - Use variáveis para representar massas de teste reutilizáveis.
                - Gere cenários separados somente quando houver comportamento, regra, risco ou resultado esperado diferente.
                
                ## VARIÁVEIS DO ZEPHYR
                - Sempre que houver dados que possam variar, use variáveis no formato <nomeVariavel>.
                - Exemplos:
                  - <cpfValido>
                  - <cpfInvalido>
                  - <cnpjValido>
                  - <dataInicio>
                  - <dataFim>
                  - <perfilUsuario>
                  - <statusValido>
                  - <statusInvalido>
                  - <codigoCard>
                - Preencha obrigatoriamente o campo "Variáveis" quando usar qualquer variável no cenário.
                - No campo "Variáveis", liste no formato chave=valor.
                - Cada variável deve ficar separada por ponto e vírgula.
                - Exemplo:
                  cpfValido=12345678909;
                  cpfInvalido=12345678900;
                  dataInicio=01/01/2026;
                  dataFim=31/01/2026;
                - Se o cenário não precisar de variáveis, preencher: Não se aplica.
                
                ## BDD (OBRIGATÓRIO)
                - Usar: Dado, Quando, Então.
                - Nunca repetir palavras-chave principais.
                - Usar "E" para continuidade.
                - O campo "Script de Teste (Passo-a-Passo)" deve conter preferencialmente:
                  - Dado que...
                  - E ...
                  - Quando ...
                  - E ...
                - O resultado final deve ficar no campo "Script de Teste (Passo-a-Passo) - Resultado".
                - Evite colocar "Então" dentro do campo "Script de Teste (Passo-a-Passo)" quando possível.
                
                Exemplo correto:
                Dado que o usuário possui cadastro válido
                E possui saldo disponível
                Quando solicita saque
                E confirma a operação
                
                Resultado:
                Então o sistema processa a operação
                E registra a transação
                
                ---
                
                # USO DE CONTEXTO ADICIONAL
                
                Quando houver transcrição de reunião, devbox, estimativa, refinamento ou PDF:
                - Use esse conteúdo para enriquecer os cenários.
                - Identifique decisões tomadas verbalmente.
                - Capture exceções, dúvidas, impactos e regras implícitas.
                - Não invente regras que não estejam no título, regra de negócio ou contexto.
                - Se houver conflito entre regra escrita e transcrição, registre em Considerações.
                - Se houver ambiguidade, aponte antes ou dentro das Considerações.
                
                ---
                
                # SAÍDA ESPERADA
                
                ## 1. PLANO MACRO DE TESTE
                
                Dividir em:
                - Preparação de dados
                - Execução do fluxo principal, usando matriz de inputs quando necessário
                - Testes exploratórios e regressão
                - Critérios de aceite
                
                ---
                
                ## 2. CENÁRIOS (FORMATO ZEPHYR)
                
                Para cada cenário, use obrigatoriamente os campos abaixo:
                
                Nome: [Nome do Cenário claro e descritivo]
                Objetivo: [Descrição concisa do que o teste valida]
                Precondição: [Estado inicial do sistema ou dados necessários]
                Script de Teste (Passo-a-Passo): [Passos Gherkin sem o resultado final, preferencialmente Dado/E/Quando/E]
                Script de Teste (Passo-a-Passo) - Resultado: [Resultado esperado, começando com "Então..."]
                Variáveis: [Lista de variáveis no formato chave=valor; ou "Não se aplica"]
                Componente: [Módulo ou funcionalidade principal]
                Rótulos: [Palavras-chave separadas por vírgula]
                Propósito: [Breve explicação de por que este cenário é importante]
                Pasta: [Caminho/módulo onde o cenário deve ser armazenado]
                Proprietário: JIRAUSER23105
                Cobertura: [Número da História/Requisito coberto, se houver]
                Status: Aguardando execução
                
                ---
                
                ## 3. CONSIDERAÇÕES
                
                Apontar:
                - Ambiguidades
                - Riscos técnicos
                - Impacto em APIs
                - Problemas de dados
                - Pontos extraídos da transcrição, quando houver
                - Possíveis impactos de regressão
                - Sugestões de parametrização quando houver muitos dados similares
                
                ---
                
                # ESTILO
                - Técnico
                - Direto
                - Sem explicações desnecessárias
                - Usar tabela quando necessário
                - Gerar conteúdo em português do Brasil
                
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

    public static String getUserPromptComContexto(String titulo, String regra, String contexto) {
        return String.format("""
                Título da Feature (Tema): %s
                Regra de Negócio (Critérios de Aceite): %s
                
                ---
                
                CONTEXTO ADICIONAL
                Use o conteúdo abaixo como apoio para gerar cenários mais completos, menos repetitivos e mais próximos do risco real.
                
                %s
                
                ---
                
                INSTRUÇÕES PARA USAR O CONTEXTO:
                - Extraia regras implícitas, exceções, riscos e decisões da reunião.
                - Considere impactos em APIs, integrações, permissões, dados e regressão.
                - Use variáveis quando houver massas de dados semelhantes.
                - Evite criar cenários duplicados mudando apenas CPF, CNPJ, data, status, perfil, valor ou código.
                - Gere a quantidade de cenários necessária para cobrir os riscos identificados.
                - Não limite artificialmente a quantidade.
                - Se o contexto estiver confuso, incompleto ou contraditório, registre em Considerações.
                - Não copie a transcrição inteira na resposta.
                """, titulo, regra, contexto);
    }
}