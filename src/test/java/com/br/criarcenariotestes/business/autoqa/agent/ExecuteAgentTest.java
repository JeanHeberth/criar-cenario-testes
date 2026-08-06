package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.execution.TestExecutionService;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ExecutionValidationException;
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
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ExecuteAgent - Testes Unitários")
class ExecuteAgentTest {

    private TestExecutionService testExecutionService;
    private ExecuteAgent agent;

    @BeforeEach
    void setUp() {
        testExecutionService = Mockito.mock(TestExecutionService.class);
        agent = new ExecuteAgent(testExecutionService);
    }

    @Test
    @DisplayName("Deve possuir nome 'execute'")
    void devePossuirNomeExecute() {
        assertThat(agent.getName()).isEqualTo("execute");
    }

    @Test
    @DisplayName("Deve possuir Order(70)")
    void devePossuirOrderSetenta() {
        Order order = ExecuteAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(70);
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        assertThatThrownBy(() -> agent.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir discovery")
    void deveExigirDiscovery() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThat(agent.execute(context).success()).isFalse();
        verify(testExecutionService, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve exigir scenario, knowledge, planning, generation e review (contexto incompleto)")
    void deveExigirFasesAnteriores() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve exigir ApplyResult")
    void deveExigirApplyResult() {
        AutoQaContext context = contextoComCodeReview();
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve exigir ExecutionApproval")
    void deveExigirExecutionApproval() {
        AutoQaContext context = contextoComApplyResult(ApplyStatus.COMPLETED);
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus BLOCKED sem chamar o service")
    void deveRejeitarApplyBlocked() {
        AutoQaContext context = contextoCompleto(ApplyStatus.BLOCKED);
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus ROLLED_BACK sem chamar o service")
    void deveRejeitarApplyRolledBack() {
        AutoQaContext context = contextoCompleto(ApplyStatus.ROLLED_BACK);
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve rejeitar ApplyStatus FAILED sem chamar o service")
    void deveRejeitarApplyFailed() {
        AutoQaContext context = contextoCompleto(ApplyStatus.FAILED);
        assertThat(agent.execute(context).success()).isFalse();
        verifyNoInteractions(testExecutionService);
    }

    @Test
    @DisplayName("Deve chamar TestExecutionService quando pré-condições OK")
    void deveChamarTestExecutionService() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        agent.execute(context);

        verify(testExecutionService).execute(
                context.getExecutionId(), context.getProjectDiscoveryResult(), context.getProjectKnowledgeResult(),
                context.getApplyResult(), context.getExecutionApproval()
        );
    }

    @Test
    @DisplayName("Deve registrar ExecutionResult no contexto")
    void deveRegistrarExecutionResult() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        ExecutionResult result = sampleResult(ExecutionStatus.PASSED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(result);

        agent.execute(context);

        assertThat(context.getExecutionResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve retornar success=true para PASSED")
    void deveRetornarSuccessParaPassed() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar success=true para FAILED (testes falharam, não é erro técnico)")
    void deveRetornarSuccessParaFailed() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.FAILED));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("FAILED");
    }

    @Test
    @DisplayName("Deve retornar failure para ERROR")
    void deveRetornarFailureParaError() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.ERROR));

        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure para BLOCKED")
    void deveRetornarFailureParaBlocked() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.BLOCKED));

        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve tratar TIMED_OUT como success=false, conforme política definida")
    void deveTratarTimedOutConformePolitica() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.TIMED_OUT));

        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure quando o service lançar ExecutionValidationException")
    void deveRetornarFailureQuandoServiceFalhar() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any()))
                .thenThrow(new ExecutionValidationException("inválido"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Falha na execução dos testes do projeto");
    }

    @Test
    @DisplayName("Não deve registrar resultado em caso de falha")
    void deveNaoRegistrarResultadoEmFalha() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        when(testExecutionService.execute(any(), any(), any(), any(), any()))
                .thenThrow(new ExecutionValidationException("inválido"));

        agent.execute(context);

        assertThat(context.getExecutionResult()).isNull();
    }

    @Test
    @DisplayName("Não deve expor projectPath na mensagem")
    void deveNaoExporProjectPath() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto/sensivel/secreto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.message()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("Não deve expor stdout/stderr completos na mensagem")
    void deveNaoExporStdoutCompleto() {
        AutoQaContext context = contextoCompleto(ApplyStatus.COMPLETED);
        ExecutionResult result = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "stdout-secreto-completo-aqui", "", false, false,
                List.of(), List.of(), true);
        when(testExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(result);

        AgentExecutionResult agentResult = agent.execute(context);

        assertThat(agentResult.message()).doesNotContain("stdout-secreto-completo-aqui");
    }

    @Test
    @DisplayName("Não deve chamar o service sem pré-condições atendidas")
    void deveNaoChamarServiceSemPreCondicoes() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        agent.execute(context);
        verifyNoInteractions(testExecutionService);
    }

    // --- helpers ---

    private AutoQaContext contextoComCodeReview() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge());
        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST));
        context.registerTechnicalPlan(plan);
        context.registerGeneration(generationResult());
        context.registerCodeReview(reviewResult());
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

    private AutoQaContext contextoComApplyResult(ApplyStatus status) {
        AutoQaContext context = contextoComCodeReview();
        ApplyApproval applyApproval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.CREATE), true, true);
        context.registerApplyApproval(applyApproval);
        ApplyResult applyResult = new ApplyResult(context.getExecutionId(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/" + context.getExecutionId(), status, false, true);
        context.registerApplyResult(applyResult);
        return context;
    }

    private AutoQaContext contextoCompleto(ApplyStatus applyStatus) {
        AutoQaContext context = contextoComApplyResult(applyStatus);
        ExecutionApproval executionApproval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
        context.registerExecutionApproval(executionApproval);
        return context;
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
                status == ExecutionStatus.PASSED || status == ExecutionStatus.FAILED || status == ExecutionStatus.BLOCKED);
    }
}
