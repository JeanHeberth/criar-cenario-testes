package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FailureAnalysisResponseParser {

    private static final int MAX_LENGTH = 50_000;

    public FailureAnalysisAiResponse parse(String json) {
        if (json == null) throw new FailureAnalysisParseException("response is null");
        String trimmed = json.trim();
        if (trimmed.isEmpty()) throw new FailureAnalysisParseException("response is blank");
        if (trimmed.length() > MAX_LENGTH) throw new FailureAnalysisParseException("response is too large");

        // remove common markdown wrappers (```json ... ``` or ``` ... ```)
        String cleaned = trimmed.replaceAll("(?s)```(?:json)?\\n", "").replaceAll("(?s)```", "");

        // Mesma política dos demais parsers do pipeline (planning, generation,
        // review, scenario): campo extra na resposta da IA é IGNORADO, não motivo
        // para descartar tudo. Este parser ficou de fora daquela correção e era o
        // único ainda estrito — o efeito era o provider primário falhar sempre no
        // parse, cair no fallback e só então o erro aparecer, mascarando a causa.
        ObjectMapper mapper = new ObjectMapper().copy();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);

        try {
            FailureAnalysisAiResponse resp = mapper.readValue(cleaned, FailureAnalysisAiResponse.class);
            if (resp == null) throw new FailureAnalysisParseException("parsed response is null");
            return resp;
        } catch (FailureAnalysisParseException fae) {
            throw fae;
        } catch (Exception ex) {
            throw new FailureAnalysisParseException("failed to parse AI response", ex);
        }
    }
}