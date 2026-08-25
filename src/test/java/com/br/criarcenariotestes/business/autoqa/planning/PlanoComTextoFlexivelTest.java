package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PlanoComTextoFlexivelTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String planoCom(String assumptions) {
        return """
                {"title":"t","strategy":"s","fileActions":[],"components":[],"reuseDecisions":[],
                 "risks":[],"warnings":[],"assumptions":%s,"constraints":[],"requiredApprovals":[],
                 "status":"READY","confidence":"HIGH","valid":true}
                """.formatted(assumptions);
    }

    @Test
    void deveAceitarStringPromovidaAObjeto() throws Exception {
        // Caso real: o planejamento falhou nos DOIS provedores com
        // "Cannot deserialize value of type String from Object value".
        // O modelo enriquece o item da lista como objeto quando a entrada é
        // longa, e a resposta inteira era descartada por formato — não por
        // conteúdo. O TextoFlexivelDeserializer já existia e resolvia isso na
        // análise de cenário; só não tinha sido aplicado ao plano.
        String json = planoCom("""
                [{"description":"A API responde em português"}, "Segunda premissa"]
                """);

        assertThatCode(() -> mapper.readValue(json, TechnicalPlanResult.class)).doesNotThrowAnyException();

        TechnicalPlanResult plano = mapper.readValue(json, TechnicalPlanResult.class);
        assertThat(plano.assumptions())
                .containsExactly("A API responde em português", "Segunda premissa");
    }

    @Test
    void deveContinuarAceitandoListaDeStringSimples() throws Exception {
        TechnicalPlanResult plano = mapper.readValue(planoCom("[\"uma\",\"outra\"]"), TechnicalPlanResult.class);
        assertThat(plano.assumptions()).containsExactly("uma", "outra");
    }
}
