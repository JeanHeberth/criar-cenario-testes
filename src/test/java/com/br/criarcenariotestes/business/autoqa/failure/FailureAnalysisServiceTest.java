package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FailureAnalysisServiceTest {

    @Test
    void passedShouldReturnNoFailure() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        FailureAnalysisService svc = new FailureAnalysisService(resolver);

        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.PASSED, 0, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(), List.of(), true);

        var res = svc.analyze(id, sampleDiscovery(), sampleScenario(), sampleKnowledge(), samplePlan(), sampleGeneration(id), sampleReview(), sampleApply(id), exec);
        assertThat(res.status()).isEqualTo(FailureAnalysisStatus.NO_FAILURE);
        assertThat(res.confidence()).isEqualTo(FailureConfidence.HIGH);
    }

    @Test
    void failedWithDeterministicFindingShouldBeAnalyzedWithoutAI() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        FailureAnalysisService svc = new FailureAnalysisService(resolver);

        UUID id = UUID.randomUUID();
        TestExecutionSummary s = new TestExecutionSummary("JUnit", 1, 0, 1, 0, 0, List.of("com.Ex.test"), List.of());
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.FAILED, 1, Instant.now(), Instant.now(), Duration.ZERO, "", "java.lang.AssertionError: expected <1> but was <2>", false, false, List.of(s), List.of(), true);

        var res = svc.analyze(id, sampleDiscovery(), sampleScenario(), sampleKnowledge(), samplePlan(), sampleGeneration(id), sampleReview(), sampleApply(id), exec);
        assertThat(res.status()).isEqualTo(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.ANALYZED);
        assertThat(res.findings()).isNotEmpty();
    }

    @Test
    void errorStatusShouldReturnBlocked() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        FailureAnalysisService svc = new FailureAnalysisService(resolver);

        UUID id = UUID.randomUUID();
        ExecutionResult exec = new ExecutionResult(id, null, ExecutionStatus.ERROR, 2, Instant.now(), Instant.now(), Duration.ZERO, "", "", false, false, List.of(), List.of(), true);

        var res = svc.analyze(id, sampleDiscovery(), sampleScenario(), sampleKnowledge(), samplePlan(), sampleGeneration(id), sampleReview(), sampleApply(id), exec);
        assertThat(res.status()).isEqualTo(com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus.BLOCKED);
    }

    private ProjectDiscoveryResult sampleDiscovery() { return mock(ProjectDiscoveryResult.class); }
    private ScenarioAnalysisResult sampleScenario() { return mock(ScenarioAnalysisResult.class); }
    private ProjectKnowledgeResult sampleKnowledge() { return mock(ProjectKnowledgeResult.class); }
    private TechnicalPlanResult samplePlan() { return mock(TechnicalPlanResult.class); }
    private GenerationResult sampleGeneration(UUID id) { return mock(GenerationResult.class); }
    private CodeReviewResult sampleReview() { return mock(CodeReviewResult.class); }
    private ApplyResult sampleApply(UUID id) { return mock(ApplyResult.class); }
}