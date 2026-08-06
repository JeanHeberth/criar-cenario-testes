package com.br.criarcenariotestes.business.autoqa.failure;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureAnalysisPromptFactoryTest {

    @Test
    void systemPromptContainsJsonOnlyAndPortuguese() {
        FailureAnalysisPromptFactory f = new FailureAnalysisPromptFactory();
        String sys = f.systemPrompt();
        assertThat(sys.toLowerCase()).contains("json");
        assertThat(sys.toLowerCase()).contains("português");
    }

    @Test
    void userPromptIncludesEvidenceAndFindings() {
        FailureAnalysisPromptFactory f = new FailureAnalysisPromptFactory();
        UUID id = UUID.randomUUID();
        String up = f.userPrompt(id, List.of(new FailureEvidence("stderr", "src/test", 12, "t1", "msg", "ex", true)), List.of());
        assertThat(up).contains("executionId");
        assertThat(up).contains("evidence");
    }
}