package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;

import java.util.List;

public record SanitizedGenerationInput(
        AutomationFramework framework,
        /**
         * Canal do teste. O framework sozinho não decide o código: no Playwright,
         * WEB_UI usa page/locators e API usa APIRequestContext — arquivos
         * completamente diferentes. Sem este campo o prompt só via "PLAYWRIGHT" e
         * escrevia UI mesmo quando a análise dizia API.
         */
        AutomationType automationType,
        AutomationLanguage language,
        BuildTool buildTool,
        List<String> testingFrameworks,
        NamingConvention namingConvention,
        String scenarioTitle,
        String scenarioObjective,
        List<SanitizedStep> steps,
        String planTitle,
        String planStrategy,
        List<SanitizedFileAction> fileActions,
        List<SanitizedComponent> reusableComponents,
        List<String> planWarnings,
        /**
         * Erros da tentativa ANTERIOR que esta geração precisa corrigir. Vazio
         * na primeira tentativa. É o que transforma o pipeline de detector em
         * corretor: sem realimentar o erro, a retentativa repete o mesmo
         * defeito e só queima token.
         */
        List<String> correcoesObrigatorias,
        /**
         * Caminho de import do cliente, derivado do plano COMPLETO. Precisa vir
         * de fora porque numa regeração parcial o plano recebido está restrito
         * ao arquivo com erro — e o cliente, que compila bem, não está nele.
         */
        String moduloDoCliente,
        /**
         * Nomes de campo que o CONTRATO define, extraídos do texto original do
         * cenário. Existem porque o modelo tem viés forte para a convenção em
         * inglês: gerou "statusCode"/"message"/"error" repetidamente para uma
         * API que responde "status"/"erro"/"mensagem". Instrução em prosa não
         * segurou; a lista concreta dá o vocabulário fechado.
         */
        List<String> camposDoContrato
) {
    public record SanitizedStep(int order, String action, String expectedResult) {}

    public record SanitizedFileAction(
            String relativePath,
            String operationName,
            String componentTypeName,
            String reason,
            boolean existingFile,
            List<String> dependencies
    ) {}

    public record SanitizedComponent(
            String relativePath,
            String typeName,
            String componentName,
            List<String> declaredMethods,
            List<String> imports
    ) {}
}
