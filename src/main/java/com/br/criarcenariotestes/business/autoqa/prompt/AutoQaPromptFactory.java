package com.br.criarcenariotestes.business.autoqa.prompt;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.ClassInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.MethodInfo;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Factory de prompts para os agentes do Auto QA.
 * Centraliza a montagem de prompts estruturados para a IA.
 */
@Component
public class AutoQaPromptFactory {

    private static final String SYSTEM_PROMPT = """
            Você é um arquiteto sênior de automação de testes.
            Sua função é analisar projetos de automação reais e criar planos técnicos precisos.
            
            REGRAS CRÍTICAS:
            - Nunca invente classes, métodos ou arquivos que não existam no projeto
            - Baseie TODAS as recomendações nas evidências reais fornecidas
            - Se informações essenciais estiverem faltando, marque o plano como blocked=true
            - Retorne APENAS JSON válido, sem markdown, sem explicações fora do JSON
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildPlannerPrompt(AutoQaContext context, String scenarioText) {
        var discovery = context.getDiscoveryResult();
        var analysis = context.getProjectAnalysis();

        String frameworkInfo = discovery != null
                ? "Framework: " + discovery.effectiveFramework().getDescricao()
                + "\nLinguagem: " + discovery.effectiveLanguage().getDescricao()
                + "\nGerenciador de pacotes: " + (discovery.getPackageManager() != null
                        ? discovery.getPackageManager().getDescricao() : "desconhecido")
                : "Framework: não detectado";

        String pageObjectsSummary = "";
        String classCount = "0";
        if (analysis != null) {
            classCount = String.valueOf(analysis.getClasses() != null ? analysis.getClasses().size() : 0);
            pageObjectsSummary = buildPageObjectsSummary(analysis);
        }

        return """
                ## Contexto do Projeto
                %s
                Classes encontradas: %s
                
                ## Page Objects e Métodos Disponíveis
                %s
                
                ## Cenário de Teste
                %s
                
                ## Tarefa
                Crie um plano de automação em JSON com exatamente este formato:
                {
                  "testName": "nome descritivo do teste",
                  "objective": "objetivo do teste em uma frase",
                  "preconditions": ["pré-condição 1", "pré-condição 2"],
                  "requiredData": ["dado necessário 1"],
                  "existingComponentsToReuse": ["NomeDoPageObject ou Helper já existente"],
                  "existingClassesToUse": ["NomeDaClasse existente no projeto"],
                  "existingMethodsToUse": ["NomeDaClasse.nomeDoMetodo()"],
                  "filesToCreate": ["caminho/relativo/do/arquivo.spec.ts"],
                  "filesToUpdate": [],
                  "assertions": ["verificação esperada 1"],
                  "risks": ["risco identificado"],
                  "pendingItems": [],
                  "missingElements": ["elemento não encontrado no projeto"],
                  "requiresNewPageObject": false,
                  "requiresUserIntervention": false,
                  "blocked": false,
                  "blockedReason": null
                }
                 
                Regras adicionais do plano:
                - Se o cenário exigir fluxo de UI e não houver Page Object compatível, marque requiresNewPageObject=true
                - Quando requiresNewPageObject=true, inclua também o arquivo de Page Object em filesToCreate
                - Respeite a arquitetura existente do projeto (diretórios reais encontrados na análise)
                """.formatted(frameworkInfo, classCount, pageObjectsSummary, scenarioText);
    }

    public String buildCodeGeneratorPrompt(
            AutomationPlan plan,
            String frameworkInstructions,
            String scenarioText,
            com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework framework,
            com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage language,
            String preferredTestDir,
            String preferredPageObjectDir
    ) {
        String planComponents = plan.getExistingClassesToUse() != null
                ? String.join(", ", plan.getExistingClassesToUse()) : "(nenhum)";
        String planMethods = plan.getExistingMethodsToUse() != null
                ? String.join(", ", plan.getExistingMethodsToUse()) : "(nenhum)";
        String planFilesCreate = plan.getFilesToCreate() != null
                ? String.join(", ", plan.getFilesToCreate()) : "(nenhum)";
        String planAssertions = plan.getAssertions() != null
                ? String.join("; ", plan.getAssertions()) : "(nenhum)";

        return """
                ## Framework e Linguagem
                Framework: %s
                Linguagem: %s
                
                ## Perfil do Framework
                %s
                
                ## Plano Aprovado
                Teste: %s
                Objetivo: %s
                Classes existentes a usar: %s
                Métodos existentes a usar: %s
                Arquivos a criar: %s
                Assertions esperadas: %s
                
                ## Cenário de Teste
                %s

                ## Arquitetura do Projeto (obrigatório respeitar)
                - Diretório de testes preferencial: %s
                - Diretório de Page Objects preferencial: %s
                 
                ## Restrições de Segurança
                - Use APENAS os métodos listados acima — nunca invente novos
                - relativePath deve ser sempre relativo (sem /, sem C:\\, sem ../)
                - NUNCA use operation DELETE
                - Não inclua credenciais hardcoded — use variáveis de ambiente
                - Crie APENAS os arquivos listados em 'Arquivos a criar'
                - Se o plano indicar `requiresNewPageObject=true`, inclua ao menos 1 Page Object no diretório preferencial
                - Para Playwright, não criar spec fora do diretório de testes preferencial
                 
                ## Formato de Resposta
                Retorne SOMENTE JSON válido (sem markdown, sem explicações fora do JSON):
                {
                  "files": [
                    {
                      "relativePath": "tests/login/login.spec.ts",
                      "operation": "CREATE",
                      "content": "código aqui",
                      "explanation": "explicação do arquivo"
                    }
                  ],
                  "reusedComponents": ["NomeDaClasse"],
                  "missingComponents": [],
                  "warnings": [],
                  "summary": "resumo do que foi gerado"
                }
                """.formatted(
                framework.getDescricao(), language.getDescricao(),
                frameworkInstructions,
                plan.getTestName(), plan.getObjective(),
                planComponents, planMethods, planFilesCreate, planAssertions,
                scenarioText,
                preferredTestDir, preferredPageObjectDir
        );
    }

    private String buildPageObjectsSummary(
            com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult analysis
    ) {
        if (analysis.getPageObjects() == null || analysis.getPageObjects().isEmpty()) {
            return "(Nenhum Page Object encontrado no projeto)";
        }
        StringBuilder sb = new StringBuilder();
        for (ClassInfo po : analysis.getPageObjects()) {
            sb.append("Classe: ").append(po.getName())
              .append(" (").append(po.getSourceFile()).append(")\n");
            if (po.getMethods() != null) {
                for (MethodInfo m : po.getMethods()) {
                    sb.append("  - ").append(m.signature()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
