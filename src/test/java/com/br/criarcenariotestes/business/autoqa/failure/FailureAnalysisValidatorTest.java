package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisValidationException;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

public class FailureAnalysisValidatorTest {

    @Test
    void executionIdMismatchThrows() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(), List.of(), true);
        var res = new com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), FailureAnalysisStatus.INCONCLUSIVE, FailureConfidence.UNKNOWN, true, false, false, true);
        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void passedMustBeNoFailure() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.PASSED, 0, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(), List.of(), true);
        var res = new com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult(id, List.of(), List.of(), List.of(), List.of(), FailureAnalysisStatus.ANALYZED, FailureConfidence.UNKNOWN, false, false, false, true);
        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void criticalMustBeBlocking() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(new TestExecutionSummary("JUnit",1,0,1,0,0,List.of("t"),List.of())), List.of(), true);
        com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding f = new com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding("C1", com.br.criarcenariotestes.business.autoqa.model.failure.FailureCategory.APPLICATION_DEFECT, com.br.criarcenariotestes.business.autoqa.model.failure.FailureOrigin.APPLICATION, com.br.criarcenariotestes.business.autoqa.model.failure.FailureSeverity.CRITICAL, com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence.HIGH, "t","d","c", List.of("t"), List.of(), List.of(new com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence("stderr", null, null, "t", "m", "e", true)), false, false);
        var res = new com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult(id, List.of(f), List.of(), List.of(), List.of(), FailureAnalysisStatus.ANALYZED, FailureConfidence.HIGH, true, false, false, true);
        assertThatThrownBy(() -> v.validate(res, exec, List.of(f))).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void findingsListContainingNullElementThrows() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(new TestExecutionSummary("JUnit",1,0,1,0,0,List.of("t"),List.of())), List.of(), true);

        FailureAnalysisResult res = org.mockito.Mockito.mock(FailureAnalysisResult.class);
        org.mockito.Mockito.when(res.executionId()).thenReturn(id);
        java.util.List<com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding> findingsWithNull = new java.util.ArrayList<>();
        findingsWithNull.add(null);
        org.mockito.Mockito.when(res.findings()).thenReturn(findingsWithNull);
        org.mockito.Mockito.when(res.suggestions()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.globalEvidence()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.warnings()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.status()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.ANALYZED);
        org.mockito.Mockito.when(res.confidence()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence.HIGH);

        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void suggestionsListContainingNullElementThrows() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(new TestExecutionSummary("JUnit",1,0,1,0,0,List.of("t"),List.of())), List.of(), true);

        FailureAnalysisResult res = org.mockito.Mockito.mock(FailureAnalysisResult.class);
        org.mockito.Mockito.when(res.executionId()).thenReturn(id);
        org.mockito.Mockito.when(res.findings()).thenReturn(java.util.List.of());
        java.util.List<com.br.criarcenariotestes.business.autoqa.model.failure.FailureSuggestion> suggestionsWithNull = new java.util.ArrayList<>();
        suggestionsWithNull.add(null);
        org.mockito.Mockito.when(res.suggestions()).thenReturn(suggestionsWithNull);
        org.mockito.Mockito.when(res.globalEvidence()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.warnings()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.status()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.ANALYZED);
        org.mockito.Mockito.when(res.confidence()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence.HIGH);

        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void globalEvidenceListContainingNullElementThrows() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(new TestExecutionSummary("JUnit",1,0,1,0,0,List.of("t"),List.of())), List.of(), true);

        FailureAnalysisResult res = org.mockito.Mockito.mock(FailureAnalysisResult.class);
        org.mockito.Mockito.when(res.executionId()).thenReturn(id);
        org.mockito.Mockito.when(res.findings()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.suggestions()).thenReturn(java.util.List.of());
        java.util.List<com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence> evidenceWithNull = new java.util.ArrayList<>();
        evidenceWithNull.add(null);
        org.mockito.Mockito.when(res.globalEvidence()).thenReturn(evidenceWithNull);
        org.mockito.Mockito.when(res.warnings()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.status()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.ANALYZED);
        org.mockito.Mockito.when(res.confidence()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence.HIGH);

        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }

    @Test
    void warningsListContainingNullElementThrows() {
        FailureAnalysisValidator v = new FailureAnalysisValidator();
        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(new TestExecutionSummary("JUnit",1,0,1,0,0,List.of("t"),List.of())), List.of(), true);

        FailureAnalysisResult res = org.mockito.Mockito.mock(FailureAnalysisResult.class);
        org.mockito.Mockito.when(res.executionId()).thenReturn(id);
        org.mockito.Mockito.when(res.findings()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.suggestions()).thenReturn(java.util.List.of());
        org.mockito.Mockito.when(res.globalEvidence()).thenReturn(java.util.List.of());
        java.util.List<com.br.criarcenariotestes.business.autoqa.model.failure.FailureWarning> warningsWithNull = new java.util.ArrayList<>();
        warningsWithNull.add(null);
        org.mockito.Mockito.when(res.warnings()).thenReturn(warningsWithNull);
        org.mockito.Mockito.when(res.status()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.ANALYZED);
        org.mockito.Mockito.when(res.confidence()).thenReturn(com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence.HIGH);

        assertThatThrownBy(() -> v.validate(res, exec, List.of())).isInstanceOf(FailureAnalysisValidationException.class);
    }
}