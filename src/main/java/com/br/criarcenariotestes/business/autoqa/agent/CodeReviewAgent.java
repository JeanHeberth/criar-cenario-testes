package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agente simples de revisão de código gerado.
 * Detecta padrões básicos (console.log, ausência de asserções) e adiciona WorkflowIssue ao contexto.
 */
@Component
@RequiredArgsConstructor
public class CodeReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewAgent.class);

    public void review(AutoQaContext context) {
        GeneratedCodeResponse response = context.getGeneratedCodeResponse();
        if (response == null) return;
        if (response.generationFailed()) return;
        if (response.files() == null || response.files().isEmpty()) return;

        for (GeneratedFile f : response.files()) {
            if (f == null) continue;
            String content = f.content() != null ? f.content() : "";
            String rel = f.relativePath();

            // detect debug logs
            if (content.toLowerCase().contains("console.log")) {
                context.addIssue(WorkflowIssue.warning(
                        "CODE_REVIEW", "CONSOLE_LOG",
                        "console.log encontrado em " + rel,
                        "Remover logs de depuração; use asserts ou logger configurado"));
            }

            // detect missing assertions (basic heuristic)
            String lower = content.toLowerCase();
            boolean hasAssertion = lower.contains("expect(") || lower.contains("assert") || lower.contains("should(");
            if (!hasAssertion) {
                context.addIssue(WorkflowIssue.warning(
                        "CODE_REVIEW", "NO_ASSERTION",
                        "Arquivo sem asserções detectado: " + rel,
                        "Adicionar 'expect' ou 'assert' para validar comportamento"));
            }
        }
    }
}
