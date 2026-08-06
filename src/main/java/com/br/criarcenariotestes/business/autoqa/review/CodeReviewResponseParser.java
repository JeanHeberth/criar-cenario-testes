package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CodeReviewResponseParser {

    static final int MAX_RESPONSE_LENGTH = 120_000;

    private final ObjectMapper objectMapper;

    public CodeReviewResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public CodeReviewAiResponse parse(String response) {
        if (response == null) {
            throw new CodeReviewParseException("Resposta nula");
        }
        String normalized = response.trim();
        if (normalized.isEmpty()) {
            throw new CodeReviewParseException("Resposta vazia");
        }
        if (normalized.length() > MAX_RESPONSE_LENGTH) {
            throw new CodeReviewParseException("Resposta acima do limite permitido");
        }

        String json = removeKnownWrappers(normalized);
        try {
            return objectMapper.readValue(json, CodeReviewAiResponse.class);
        } catch (JsonProcessingException exception) {
            throw new CodeReviewParseException("JSON inválido", exception);
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
