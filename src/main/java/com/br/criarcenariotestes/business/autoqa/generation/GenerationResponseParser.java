package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationParseException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class GenerationResponseParser {

    private static final Logger log = LoggerFactory.getLogger(GenerationResponseParser.class);
    static final int MAX_RESPONSE_LENGTH = 120_000;

    private final ObjectMapper objectMapper;

    public GenerationResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
    }

    public GenerationResult parse(String response) {
        if (response == null) {
            throw new GenerationParseException("Resposta nula");
        }
        String normalized = response.trim();
        if (normalized.isEmpty()) {
            throw new GenerationParseException("Resposta vazia");
        }
        if (normalized.length() > MAX_RESPONSE_LENGTH) {
            throw new GenerationParseException("Resposta acima do limite permitido");
        }

        String json = removeKnownWrappers(normalized);
        try {
            return objectMapper.readValue(json, GenerationResult.class);
        } catch (JsonProcessingException exception) {
            log.warn("Generation parse failed. jacksonMessage='{}', jsonPreview='{}'",
                    exception.getOriginalMessage(),
                    json.length() > 500 ? json.substring(0, 500) + "..." : json);
            throw new GenerationParseException("JSON inválido", exception);
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
