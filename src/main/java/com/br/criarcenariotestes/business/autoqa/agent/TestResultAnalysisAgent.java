package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agente que analisa resultado de execução de testes.
 * Detecta falhas, extrai descrições de erro, adiciona issues ao workflow.
 */
@Component
@RequiredArgsConstructor
public class TestResultAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(TestResultAnalysisAgent.class);

    public void execute(AutoQaContext context) {
        TestExecutionResult result = context.getTestExecutionResult();
        if (result == null) {
            return;
        }

        if (result.success()) {
            log.info("Testes passaram com sucesso");
            return;
        }

        // Se exit code != 0, já indica falha
        if (result.exitCode() != 0) {
            // Analisar falhas em stdout
            if (!result.stdout().isEmpty()) {
                String[] stdoutLines = result.stdout().split("\n");
                for (String line : stdoutLines) {
                    if (line.contains("FAIL")) {
                        WorkflowIssue issue = WorkflowIssue.error(
                                "TEST_EXECUTION",
                                "TEST_FAILURE",
                                line
                        );
                        context.addIssue(issue);
                    }
                }
            }

            // Analisar falhas em stderr
            if (!result.stderr().isEmpty()) {
                String[] stderrLines = result.stderr().split("\n");
                for (String line : stderrLines) {
                    if (!line.trim().isEmpty()) {
                        if (line.trim().startsWith("Error:") || line.contains("FAIL")) {
                            WorkflowIssue issue = WorkflowIssue.error(
                                    "TEST_EXECUTION",
                                    "TEST_FAILURE",
                                    line
                            );
                            context.addIssue(issue);
                        }
                    }
                }
            }

            // Se nenhuma issue foi adicionada mas há falha, criar uma genérica
            if (context.getIssues().isEmpty()) {
                String message = !result.stderr().isEmpty() 
                    ? result.stderr() 
                    : !result.stdout().isEmpty() 
                    ? result.stdout() 
                    : "Tests failed with exit code " + result.exitCode();
                WorkflowIssue issue = WorkflowIssue.error(
                        "TEST_EXECUTION",
                        "TEST_FAILURE",
                        message
                );
                context.addIssue(issue);
            }
        }
    }
}
