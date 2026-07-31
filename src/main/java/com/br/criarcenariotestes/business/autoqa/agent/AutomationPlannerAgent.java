package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.prompt.AutoQaPromptFactory;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agente de planejamento de automação.
 * Usa IA para criar um plano técnico estruturado a partir do cenário e da análise do projeto.
 * Nunca gera código — apenas o plano de implementação.
 */
@Component
@RequiredArgsConstructor
public class AutomationPlannerAgent {

    private static final Logger log = LoggerFactory.getLogger(AutomationPlannerAgent.class);

    private final AiProviderResolver providerResolver;
    private final AutoQaPromptFactory promptFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutomationPlan plan(AutoQaContext context, String scenarioText) {
        log.info("Gerando plano de automação. executionId=\'{}\'", context.executionIdAsString());
        String systemPrompt = promptFactory.buildSystemPrompt();
        String userPrompt = promptFactory.buildPlannerPrompt(context, scenarioText);
        String aiResponse;
        try {
            aiResponse = providerResolver.getActiveProvider().gerarResposta(systemPrompt, userPrompt);
        } catch (Exception ex) {
            log.error("Falha ao chamar IA para planejamento: {}", ex.getMessage(), ex);
            return blockedPlan("Falha ao comunicar com a IA: " + ex.getMessage());
        }
        return parseResponse(aiResponse);
    }

    @SuppressWarnings("unchecked")
    private AutomationPlan parseResponse(String response) {
        String json = extractJson(response);
        if (json == null) {
            log.warn("Resposta da IA não contém JSON válido");
            return blockedPlan("Não foi possível fazer parse do plano retornado pela IA");
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return AutomationPlan.builder()
                    .testName(str(map, "testName"))
                    .objective(str(map, "objective"))
                    .preconditions(strList(map, "preconditions"))
                    .requiredData(strList(map, "requiredData"))
                    .existingComponentsToReuse(strList(map, "existingComponentsToReuse"))
                    .existingClassesToUse(strList(map, "existingClassesToUse"))
                    .existingMethodsToUse(strList(map, "existingMethodsToUse"))
                    .filesToCreate(strList(map, "filesToCreate"))
                    .filesToUpdate(strList(map, "filesToUpdate"))
                    .assertions(strList(map, "assertions"))
                    .risks(strList(map, "risks"))
                    .pendingItems(strList(map, "pendingItems"))
                    .missingElements(strList(map, "missingElements"))
                    .requiresNewPageObject(bool(map, "requiresNewPageObject"))
                    .requiresUserIntervention(bool(map, "requiresUserIntervention"))
                    .blocked(bool(map, "blocked"))
                    .blockedReason(str(map, "blockedReason"))
                    .build();
        } catch (Exception ex) {
            log.warn("Erro ao parsear JSON do plano: {}", ex.getMessage());
            return blockedPlan("Não foi possível fazer parse do plano retornado pela IA: " + ex.getMessage());
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

    private AutomationPlan blockedPlan(String reason) {
        return AutomationPlan.builder().blocked(true).blockedReason(reason)
                .filesToCreate(List.of()).filesToUpdate(List.of()).build();
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

    private boolean bool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}
