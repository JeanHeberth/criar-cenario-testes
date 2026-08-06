package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningParseException;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningScope;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LearningResponseParser {

    private static final int MAX_LENGTH = 50_000;

    public LearningAiResponse parse(String json) {
        if (json == null) throw new LearningParseException("response is null");
        String trimmed = json.trim();
        if (trimmed.isEmpty()) throw new LearningParseException("response is blank");
        if (trimmed.length() > MAX_LENGTH) throw new LearningParseException("response is too large");

        String cleaned = trimmed.replaceAll("(?s)```(?:json)?\\n", "").replaceAll("(?s)```", "");

        ObjectMapper mapper = new ObjectMapper().copy();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        LearningAiResponse response;
        try {
            response = mapper.readValue(cleaned, LearningAiResponse.class);
        } catch (LearningParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LearningParseException("failed to parse AI response", ex);
        }
        if (response == null) {
            throw new LearningParseException("parsed response is null");
        }
        validateEnums(response);
        return response;
    }

    private void validateEnums(LearningAiResponse response) {
        if (response.items() == null) {
            return;
        }
        for (LearningAiResponse.Item item : response.items()) {
            if (item == null) {
                continue;
            }
            requireKnownEnum(item.type(), LearningType.class, "type");
            requireKnownEnum(item.scope(), LearningScope.class, "scope");
        }
    }

    private <E extends Enum<E>> void requireKnownEnum(String value, Class<E> enumType, String fieldName) {
        if (value == null) {
            return;
        }
        try {
            Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new LearningParseException("unknown " + fieldName + " value: " + value);
        }
    }
}
