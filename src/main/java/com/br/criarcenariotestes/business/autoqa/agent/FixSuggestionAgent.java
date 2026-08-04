package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysis;
import com.br.criarcenariotestes.business.autoqa.model.context.FixSuggestion;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agente que gera sugestões de correção para falhas de teste.
 * Usa IA para analisar padrão de erro e sugerir fix.
 */
@Component
@RequiredArgsConstructor
public class FixSuggestionAgent {

    private static final Logger log = LoggerFactory.getLogger(FixSuggestionAgent.class);
    private final AiProviderResolver aiProviderResolver;

    private static final Map<String, Integer> DEFAULT_PRIORITIES = Map.of(
            "MissingImport", 3,
            "AssertionFailed", 2,
            "TimeoutError", 5
    );

    private static final Map<String, String> DEFAULT_CODE_EXAMPLES = Map.of(
            "MissingImport", "import { requiredModule } from 'module-name';",
            "AssertionFailed", "expect(actual).toBe(expected);",
            "TimeoutError", "jest.setTimeout(10000);"
    );

    public void execute(AutoQaContext context) {
        List<FailureAnalysis> analyses = context.getFailureAnalyses();
        
        for (FailureAnalysis analysis : analyses) {
            FixSuggestion suggestion = generateSuggestion(analysis);
            context.addFixSuggestion(suggestion);
        }

        log.info("Generated {} fix suggestions", analyses.size());
    }

    private FixSuggestion generateSuggestion(FailureAnalysis analysis) {
        String failureType = analysis.failureType();
        int priority = DEFAULT_PRIORITIES.getOrDefault(failureType, 2);
        String codeExample = DEFAULT_CODE_EXAMPLES.getOrDefault(failureType, "// Fix me");

        String suggestion = generateSuggestionText(failureType, analysis);

        return new FixSuggestion(
                failureType,
                suggestion,
                codeExample,
                priority
        );
    }

    private String generateSuggestionText(String failureType, FailureAnalysis analysis) {
        return switch (failureType) {
            case "MissingImport" -> 
                "Add the missing import for module '" + analysis.errorMessage() + "'";
            case "AssertionFailed" ->
                "Review the assertion in " + analysis.sourceFile() + " at line " + 
                analysis.lineNumber() + ". The actual value does not match expected.";
            case "TimeoutError" ->
                "Increase the timeout or optimize the async operation in " + 
                analysis.sourceFile() + " at line " + analysis.lineNumber();
            default ->
                "Fix the error in " + analysis.sourceFile() + " at line " + analysis.lineNumber();
        };
    }
}
