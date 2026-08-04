package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioAnalysisResult - Contrato")
class ScenarioAnalysisResultContractTest {

    @Test
    @DisplayName("Deve manter contrato JSON após remover métodos with")
    void deveManterContratoJsonAposRemoverMetodosWith() {
        assertThat(Arrays.stream(ScenarioAnalysisResult.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .noneMatch(name -> name.startsWith("with"));
        assertThat(Arrays.stream(ScenarioAnalysisResult.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .containsExactly(
                        "title",
                        "objective",
                        "preconditions",
                        "steps",
                        "testData",
                        "businessRules",
                        "risks",
                        "ambiguities",
                        "entities",
                        "dependencies",
                        "automationType",
                        "status",
                        "warnings",
                        "valid"
                );
    }
}
