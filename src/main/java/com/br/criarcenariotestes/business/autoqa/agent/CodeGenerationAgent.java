package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.prompt.AutoQaPromptFactory;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agente de geração de código de automação.
 * Usa IA para gerar código com base no plano aprovado.
 * Valida os arquivos gerados para garantir que não contenham paths inválidos ou operações proibidas.
 */
@Component
@RequiredArgsConstructor
public class CodeGenerationAgent {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationAgent.class);

    private final AiProviderResolver providerResolver;
    private final AutoQaPromptFactory promptFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedCodeResponse generate(
            AutomationPlan plan,
            AutomationFramework framework,
            AutomationLanguage language,
            AutomationFrameworkAdapter adapter,
            String scenarioText
    ) {
        return generate(plan, framework, language, adapter, scenarioText, "tests", "tests/pages");
    }

    public GeneratedCodeResponse generate(
            AutomationPlan plan,
            AutomationFramework framework,
            AutomationLanguage language,
            AutomationFrameworkAdapter adapter,
            String scenarioText,
            String preferredTestDir,
            String preferredPageObjectDir
    ) {
        log.info("Gerando código de automação. framework={}, language={}", framework, language);

        String frameworkInstructions = adapter != null
                ? adapter.buildFrameworkInstructions(null)
                : "";

        String systemPrompt = promptFactory.buildSystemPrompt();
        String userPrompt = promptFactory.buildCodeGeneratorPrompt(
                plan, frameworkInstructions, scenarioText, framework, language,
                preferredTestDir, preferredPageObjectDir
        );

        String aiResponse;
        try {
            aiResponse = providerResolver.getActiveProvider().gerarResposta(systemPrompt, userPrompt);
        } catch (Exception ex) {
            log.error("Falha ao chamar IA para geração: {}", ex.getMessage(), ex);
            return GeneratedCodeResponse.failed("Falha ao comunicar com a IA: " + ex.getMessage());
        }

        return parseAndValidate(aiResponse);
    }

    // ─── Parser e validação ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private GeneratedCodeResponse parseAndValidate(String response) {
        String json = extractJson(response);
        if (json == null) {
            log.warn("Resposta da IA não contém JSON válido para geração de código");
            return GeneratedCodeResponse.failed(
                    "Não foi possível fazer parse do código retornado pela IA");
        }

        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            List<GeneratedFile> rawFiles = parseFiles(map);
            List<GeneratedFile> validatedFiles = validateFiles(rawFiles);

            return new GeneratedCodeResponse(
                    validatedFiles,
                    strList(map, "reusedComponents"),
                    strList(map, "missingComponents"),
                    strList(map, "warnings"),
                    str(map, "summary"),
                    false,
                    null
            );

        } catch (Exception ex) {
            log.warn("Erro ao parsear JSON de geração: {}", ex.getMessage());
            return GeneratedCodeResponse.failed(
                    "Erro ao parsear código gerado pela IA: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<GeneratedFile> parseFiles(Map<String, Object> map) {
        List<GeneratedFile> result = new ArrayList<>();
        Object filesObj = map.get("files");
        if (!(filesObj instanceof List<?> filesList)) return result;

        for (Object fileObj : filesList) {
            if (!(fileObj instanceof Map<?, ?> fileMap)) continue;
            Map<String, Object> fm = (Map<String, Object>) fileMap;

            String relativePath = str(fm, "relativePath");
            String operationStr = str(fm, "operation");
            String content = str(fm, "content");
            String explanation = str(fm, "explanation");

            GeneratedFileOperation operation = parseOperation(operationStr);

            result.add(new GeneratedFile(relativePath, operation, content, explanation, null));
        }
        return result;
    }

    private List<GeneratedFile> validateFiles(List<GeneratedFile> files) {
        List<GeneratedFile> valid = new ArrayList<>();
        for (GeneratedFile file : files) {
            if (file.relativePath() == null || file.relativePath().isBlank()) {
                log.warn("Arquivo gerado com path nulo/vazio ignorado");
                continue;
            }
            if (!file.isRelativePath()) {
                log.warn("Arquivo gerado com path inválido ignorado: {}", file.relativePath());
                continue;
            }
            if (file.operation() == null) {
                log.warn("Arquivo gerado com operação nula ignorado: {}", file.relativePath());
                continue;
            }
            if (file.operation() == GeneratedFileOperation.DELETE) {
                log.warn("Arquivo gerado com operação DELETE ignorado: {}", file.relativePath());
                continue;
            }
            valid.add(file);
        }
        return valid;
    }

    private GeneratedFileOperation parseOperation(String operationStr) {
        if (operationStr == null) return null;
        try {
            GeneratedFileOperation op = GeneratedFileOperation.valueOf(operationStr.toUpperCase());
            return op;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractJson(String response) {
        if (response == null) return null;
        String cleaned = response.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) return null;
        return cleaned.substring(start, end + 1);
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> strList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List<?> list) {
            return list.stream().filter(i -> i instanceof String).map(i -> (String) i).toList();
        }
        return List.of();
    }
}
