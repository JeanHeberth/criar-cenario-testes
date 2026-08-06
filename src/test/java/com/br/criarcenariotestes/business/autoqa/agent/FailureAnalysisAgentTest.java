package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.failure.FailureAnalysisService;
import com.br.criarcenariotestes.business.autoqa.failure.exception.FailureAnalysisException;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("FailureAnalysisAgent - Testes Unitários")
class FailureAnalysisAgentTest {

    private FailureAnalysisService service;
    private FailureAnalysisAgent agent;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(FailureAnalysisService.class);
        agent = new FailureAnalysisAgent(service);
    }

    @Test
    @DisplayName("Deve possuir nome 'failure-analysis'")
    void devePossuirNome() {
        assertThat(agent.getName()).isEqualTo("failure-analysis");
    }

    @Test
    @DisplayName("Deve possuir @Component")
    void devePossuirComponent() {
        assertThat(FailureAnalysisAgent.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    @DisplayName("Deve possuir @Order(80)")
    void devePossuirOrderOitenta() {
        Order order = FailureAnalysisAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(80);
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        assertThatThrownBy(() -> agent.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir ExecutionResult no contexto")
    void deveExigirExecutionResult() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThatThrownBy(() -> agent.execute(context)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve chamar o service com os nove resultados do contexto")
    void deveChamarServiceComTodosResultados() {
        AutoQaContext context = contextoComExecutionResult(ExecutionStatus.PASSED);
        when(service.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(noFailureResult(context.getExecutionId()));

        agent.execute(context);

        Mockito.verify(service).analyze(
                context.getExecutionId(), context.getProjectDiscoveryResult(), context.getScenarioAnalysisResult(),
                context.getProjectKnowledgeResult(), context.getTechnicalPlanResult(), context.getGenerationResult(),
                context.getCodeReviewResult(), context.getApplyResult(), context.getExecutionResult());
    }

    @Test
    @DisplayName("Deve registrar FailureAnalysisResult no contexto")
    void deveRegistrarResultado() {
        AutoQaContext context = contextoComExecutionResult(ExecutionStatus.PASSED);
        FailureAnalysisResult result = noFailureResult(context.getExecutionId());
        when(service.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(result);

        agent.execute(context);

        assertThat(context.getFailureAnalysisResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve retornar success=true quando o service concluir normalmente")
    void deveRetornarSuccess() {
        AutoQaContext context = contextoComExecutionResult(ExecutionStatus.PASSED);
        when(service.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(noFailureResult(context.getExecutionId()));

        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar failure quando o service lançar FailureAnalysisException")
    void deveRetornarFailureQuandoServiceFalhar() {
        AutoQaContext context = contextoComExecutionResult(ExecutionStatus.FAILED);
        when(service.analyze(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new FailureAnalysisException("falha técnica"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(context.getFailureAnalysisResult()).isNull();
    }

    @Test
    @DisplayName("Spring deve incluir o agente na lista ordenada de AutoQaAgent")
    void deveSerIncluidoPeloSpringNaListaOrdenada() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfig.class)) {
            List<AutoQaAgent> agents = ctx.getBeanProvider(AutoQaAgent.class).orderedStream().toList();
            assertThat(agents).hasSize(2);
            assertThat(agents.get(0)).isInstanceOf(ExecuteAgent.class);
            assertThat(agents.get(1)).isInstanceOf(FailureAnalysisAgent.class);
        }
    }

    @org.springframework.context.annotation.Configuration
    static class SpringConfig {
        @Bean
        FailureAnalysisService failureAnalysisService() {
            return Mockito.mock(FailureAnalysisService.class);
        }

        @Bean
        FailureAnalysisAgent failureAnalysisAgent(FailureAnalysisService service) {
            return new FailureAnalysisAgent(service);
        }

        @Bean
        com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService testExecutionService() {
            return Mockito.mock(com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService.class);
        }

        @Bean
        ExecuteAgent executeAgent(com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService service) {
            return new ExecuteAgent(service);
        }
    }

    // --- helpers ---

    private AutoQaContext contextoComExecutionResult(ExecutionStatus status) {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts",
                        com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST)));
        context.registerGeneration(generationResult());
        context.registerCodeReview(reviewResult());
        ApplyApproval applyApproval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true);
        context.registerApplyApproval(applyApproval);
        ApplyResult applyResult = new ApplyResult(context.getExecutionId(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/" + context.getExecutionId(), ApplyStatus.COMPLETED, false, true);
        context.registerApplyResult(applyResult);
        ExecutionApproval executionApproval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
        context.registerExecutionApproval(executionApproval);
        context.registerExecutionResult(sampleResult(status));
        return context;
    }

    private GenerationResult generationResult() {
        UUID executionId = UUID.randomUUID();
        var file = GenerationTestData.generatedFile("tests/login.spec.ts",
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.CREATE,
                com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT,
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.GENERATED, false);
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);
    }

    private CodeReviewResult reviewResult() {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }

    private CommandSpecification command() {
        return new CommandSpecification(ExecutionCommandId.PLAYWRIGHT_TEST, "npx", List.of("playwright", "test"),
                "projeto", Duration.ofMinutes(10), Map.of(), ExecutionCommandType.TEST);
    }

    private ExecutionResult sampleResult(ExecutionStatus status) {
        Integer exitCode = switch (status) {
            case PASSED -> 0;
            case FAILED -> 1;
            default -> null;
        };
        return new ExecutionResult(UUID.randomUUID(), command(), status, exitCode,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "saida", "", false, false, List.of(), List.of(),
                true);
    }

    private FailureAnalysisResult noFailureResult(UUID executionId) {
        return new FailureAnalysisResult(executionId, List.of(), List.of(), List.of(), List.of(),
                FailureAnalysisStatus.NO_FAILURE, FailureConfidence.HIGH, false, false, false, true);
    }
}
