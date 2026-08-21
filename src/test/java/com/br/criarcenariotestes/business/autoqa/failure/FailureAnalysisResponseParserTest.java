package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FailureAnalysisResponseParserTest {

    @Test
    void parseValidJson() {
        FailureAnalysisResponseParser p = new FailureAnalysisResponseParser();
        String json = "{\"findings\":[],\"suggestions\":[],\"warnings\":[],\"confidence\":\"MEDIUM\",\"humanReviewRequired\":false,\"retryRecommended\":false,\"regenerationRecommended\":false,\"valid\":true}";
        var r = p.parse(json);
        assertThat(r).isNotNull();
        assertThat(r.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    void parseBlankThrows() {
        FailureAnalysisResponseParser p = new FailureAnalysisResponseParser();
        assertThatThrownBy(() -> p.parse("   ")).isInstanceOf(FailureAnalysisParseException.class);
    }

    @Test
    void parseUnknownPropertyIsIgnored() {
        // Alinhado aos demais parsers do pipeline: campo extra da IA é ignorado.
        // Rejeitar fazia o provider primário falhar sempre no parse e cair no
        // fallback, escondendo a causa real atrás do erro do segundo provider.
        FailureAnalysisResponseParser p = new FailureAnalysisResponseParser();
        String json = "{\"unknown\": 1}";
        assertThat(p.parse(json)).isNotNull();
    }
}