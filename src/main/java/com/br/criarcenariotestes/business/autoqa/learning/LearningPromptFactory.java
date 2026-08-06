package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningItem;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningWarning;

import java.util.List;
import java.util.UUID;

public class LearningPromptFactory {

    public String systemPrompt() {
        return "Atenção: responda APENAS com JSON válido.\n" +
                "Não utilize Markdown, não inclua explicações em texto, NÃO inclua código, diffs, patches ou comandos.\n" +
                "Não inclua caminhos absolutos, segredos, tokens ou credenciais.\n" +
                "Não invente evidência, componente, arquivo ou regra que não esteja nos dados fornecidos.\n" +
                "Não promova nenhum item para escopo GLOBAL ou TEAM.\n" +
                "Não aprove automaticamente nenhum item de escopo PROJECT ou FRAMEWORK.\n" +
                "Não remova nem substitua nenhum aprendizado determinístico já existente.\n" +
                "Responda em português do Brasil e retorne apenas o JSON com o schema solicitado.";
    }

    public String userPrompt(UUID executionId, List<LearningItem> deterministicItems, List<LearningEvidence> evidence,
                              List<LearningWarning> warnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"executionId\": \"").append(executionId).append("\",\n");
        sb.append("  \"evidence\": [\n");
        appendEvidence(sb, evidence);
        sb.append("  ],\n");
        sb.append("  \"deterministicItems\": [\n");
        appendItems(sb, deterministicItems);
        sb.append("  ],\n");
        sb.append("  \"warnings\": [\n");
        appendWarnings(sb, warnings);
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private void appendEvidence(StringBuilder sb, List<LearningEvidence> evidence) {
        for (int i = 0; i < evidence.size(); i++) {
            LearningEvidence e = evidence.get(i);
            sb.append("    {\"source\":\"").append(escape(e.source())).append("\"");
            sb.append(",\"referenceType\":\"").append(escape(e.referenceType())).append("\"");
            sb.append(",\"description\":\"").append(escape(e.description())).append("\"}");
            sb.append(i < evidence.size() - 1 ? ",\n" : "\n");
        }
    }

    private void appendItems(StringBuilder sb, List<LearningItem> items) {
        for (int i = 0; i < items.size(); i++) {
            LearningItem item = items.get(i);
            sb.append("    {\"type\":\"").append(escape(item.type().name())).append("\"");
            sb.append(",\"scope\":\"").append(escape(item.scope().name())).append("\"");
            sb.append(",\"title\":\"").append(escape(item.title())).append("\"}");
            sb.append(i < items.size() - 1 ? ",\n" : "\n");
        }
    }

    private void appendWarnings(StringBuilder sb, List<LearningWarning> warnings) {
        for (int i = 0; i < warnings.size(); i++) {
            LearningWarning w = warnings.get(i);
            sb.append("    {\"code\":\"").append(escape(w.code())).append("\"}");
            sb.append(i < warnings.size() - 1 ? ",\n" : "\n");
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
