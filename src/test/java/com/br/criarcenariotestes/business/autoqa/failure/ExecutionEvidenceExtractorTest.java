package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionEvidenceExtractorTest {

    @Test
    void shouldExtractFailedTests() {
        TestExecutionSummary s = new TestExecutionSummary("JUnit", 3, 2, 1, 0, 0, List.of("com.example.Test.testA"), List.of());
        ExecutionResult exec = new ExecutionResult(UUID.randomUUID(), null, com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus.FAILED, 1, null, null, null, "", "", false, false, List.of(s), List.of(), true);

        ExecutionEvidenceExtractor extractor = new ExecutionEvidenceExtractor();
        var ev = extractor.extract(exec);

        assertThat(ev).isNotEmpty();
        assertThat(ev.get(0).testName()).isEqualTo("com.example.Test.testA");
    }
}