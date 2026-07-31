package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysis;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agente que analisa falhas de execução de testes.
 * Detecta tipo de erro, extrai arquivo/linha, e armazena análise.
 */
@Component
@RequiredArgsConstructor
public class FailureAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(FailureAnalysisAgent.class);

    private static final Pattern FILE_LINE_PATTERN = Pattern.compile("(?:at\\s+(?:.*/)?([^/:]+):(\\d+)|at line\\s+(\\d+)\\s+in\\s+([^\\s]+))");
    private static final Pattern MISSING_IMPORT_PATTERN = Pattern.compile("(?:Cannot find|Cannot resolve) (?:module|reference|name|symbol)");
    private static final Pattern ASSERTION_PATTERN = Pattern.compile("(?:AssertionError|Expected|assertion failed|Expected.*but got)");
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?:Timeout|timeout|TIMEOUT|jest.setTimeout)");

    public void execute(AutoQaContext context) {
        TestExecutionResult executionResult = context.getTestExecutionResult();
        if (executionResult == null) {
            log.warn("No test execution result found");
            return;
        }

        String stdout = executionResult.stdout();
        String stderr = executionResult.stderr();
        List<String> output = new ArrayList<>();
        
        if (stdout != null && !stdout.isEmpty()) {
            output.addAll(stdout.lines().toList());
        }
        if (stderr != null && !stderr.isEmpty()) {
            output.addAll(stderr.lines().toList());
        }

        if (output.isEmpty()) {
            return;
        }

        List<FailureAnalysis> failures = extractFailures(output);
        for (FailureAnalysis failure : failures) {
            context.addFailureAnalysis(failure);
        }

        log.info("Analyzed {} test failures", failures.size());
    }

    private List<FailureAnalysis> extractFailures(List<String> output) {
        List<FailureAnalysis> failures = new ArrayList<>();
        StringBuilder currentStackTrace = new StringBuilder();
        String currentFile = null;
        String currentErrorMessage = null;
        String currentFailureType = null;
        int currentLineNumber = 0;

        for (String line : output) {
            // Detecta tipo de erro
            String failureType = detectFailureType(line);
            if (failureType != null) {
                if (currentFailureType != null && currentErrorMessage != null) {
                    FailureAnalysis fa = new FailureAnalysis(
                            currentFailureType,
                            currentErrorMessage,
                            currentFile != null ? currentFile : "unknown",
                            currentLineNumber,
                            currentStackTrace.toString(),
                            List.of()
                    );
                    failures.add(fa);
                    currentStackTrace = new StringBuilder();
                }
                currentFailureType = failureType;
                currentErrorMessage = line.trim();
                currentFile = null;
                currentLineNumber = 0;
            }

            if (currentFailureType != null) {
                currentStackTrace.append(line).append("\n");

                // Extrai arquivo e linha
                Matcher m = FILE_LINE_PATTERN.matcher(line);
                if (m.find()) {
                    // Grupo 1: arquivo (formato1), Grupo 2: linha (formato1)
                    // Grupo 3: linha (formato2), Grupo 4: arquivo (formato2)
                    if (m.group(1) != null) {
                        currentFile = m.group(1);
                        currentLineNumber = Integer.parseInt(m.group(2));
                    } else if (m.group(3) != null) {
                        currentLineNumber = Integer.parseInt(m.group(3));
                        currentFile = m.group(4);
                    }
                }
            }
        }

        // Adiciona última falha
        if (currentFailureType != null && currentErrorMessage != null) {
            FailureAnalysis fa = new FailureAnalysis(
                    currentFailureType,
                    currentErrorMessage,
                    currentFile != null ? currentFile : "unknown",
                    currentLineNumber,
                    currentStackTrace.toString(),
                    List.of()
            );
            failures.add(fa);
        }

        return failures;
    }

    private String detectFailureType(String line) {
        if (MISSING_IMPORT_PATTERN.matcher(line).find()) {
            return "MissingImport";
        }
        if (ASSERTION_PATTERN.matcher(line).find()) {
            return "AssertionFailed";
        }
        if (TIMEOUT_PATTERN.matcher(line).find()) {
            return "TimeoutError";
        }
        return null;
    }
}
