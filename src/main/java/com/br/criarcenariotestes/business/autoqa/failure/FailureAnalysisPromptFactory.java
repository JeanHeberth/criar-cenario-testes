package com.br.criarcenariotestes.business.autoqa.failure;

import java.util.List;
import java.util.UUID;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding;

public class FailureAnalysisPromptFactory {

    public String systemPrompt() {
        return "Atenção: responda APENAS com JSON válido. \n" +
                "Não utilize Markdown, não inclua explicações em texto, NÃO inclua código corrigido, diffs, patches ou comandos.\n" +
                "Não inclua caminhos absolutos, segredos, tokens ou credenciais.\n" +
                "Não invente arquivos, nomes de testes, linhas ou evidências.\n" +
                "Não remova findings determinísticos nem reduza sua severidade, confiança ou blocking.\n" +
                "Responda em português do Brasil e retorne apenas o JSON com o schema solicitado.";
    }

    public String userPrompt(UUID executionId, List<FailureEvidence> evidence, List<FailureFinding> deterministicFindings) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"executionId\": \"").append(executionId).append("\",\n");
        sb.append("  \"evidence\": [\n");
        for (int i = 0; i < evidence.size(); i++) {
            FailureEvidence e = evidence.get(i);
            sb.append("    {");
            sb.append("\"source\":\"").append(escape(e.source())).append("\"");
            if (e.relativePath() != null) sb.append(",\"relativePath\":\"").append(escape(e.relativePath())).append("\"");
            if (e.line() != null) sb.append(",\"line\":").append(e.line());
            if (e.testName() != null) sb.append(",\"testName\":\"").append(escape(e.testName())).append("\"");
            if (e.message() != null) sb.append(",\"message\":\"").append(escape(e.message())).append("\"");
            sb.append("}");
            if (i < evidence.size() - 1) sb.append(",\n"); else sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"deterministicFindings\": [\n");
        for (int i = 0; i < deterministicFindings.size(); i++) {
            FailureFinding f = deterministicFindings.get(i);
            sb.append("    {\"code\":\"").append(escape(f.code())).append("\",\"category\":\"").append(escape(f.category().name())).append("\"}");
            if (i < deterministicFindings.size() - 1) sb.append(",\n"); else sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}