package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ScenarioAnalysisResponseParser {

    private static final int MAX_RESPONSE_LENGTH = 80_000;

    private final ObjectMapper objectMapper;

    public ScenarioAnalysisResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
    }

    public ScenarioAnalysisResult parse(String response) {
        if (response == null) {
            throw new ScenarioAnalysisParseException("Resposta nula");
        }
        String normalized = response.trim();
        if (normalized.isEmpty()) {
            throw new ScenarioAnalysisParseException("Resposta vazia");
        }
        if (normalized.length() > MAX_RESPONSE_LENGTH) {
            throw new ScenarioAnalysisParseException("Resposta acima do limite permitido");
        }

        String json = removeKnownWrappers(normalized);
        try {
            return objectMapper.readValue(json, ScenarioAnalysisResult.class);
        } catch (JsonProcessingException exception) {
            throw new ScenarioAnalysisParseException("JSON inválido", exception);
        }
    }

    private String removeKnownWrappers(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring("```json".length()).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }
}
