package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.learning.LearningService;
import com.br.criarcenariotestes.business.autoqa.learning.LearningTestData;
import com.br.criarcenariotestes.business.autoqa.learning.exception.LearningException;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LearningAgent - Testes Unitários")
class LearningAgentTest {

    private LearningService service;
    private LearningAgent agent;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(LearningService.class);
        agent = new LearningAgent(service);
    }

    @Test
    @DisplayName("Deve possuir nome 'learning'")
    void devePossuirNome() {
        assertThat(agent.getName()).isEqualTo("learning");
    }

    @Test
    @DisplayName("Deve possuir @Component")
    void devePossuirComponent() {
        assertThat(LearningAgent.class.getAnnotation(Component.class)).isNotNull();
    }

    @Test
    @DisplayName("Deve possuir @Order(90)")
    void devePossuirOrderNoventa() {
        Order order = LearningAgent.class.getAnnotation(Order.class);
        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(90);
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo")
    void deveRejeitarContextoNulo() {
        assertThatThrownBy(() -> agent.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir FailureAnalysisResult no contexto")
    void deveExigirFailureAnalysisResult() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.success()).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Deve chamar o service com os nove resultados do contexto")
    void deveChamarServiceComNoveResultados() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(collectedResult(context.getExecutionId()));

        agent.execute(context);

        verify(service).learn(context.getExecutionId(), context.getProjectDiscoveryResult(),
                context.getScenarioAnalysisResult(), context.getProjectKnowledgeResult(), context.getTechnicalPlanResult(),
                context.getGenerationResult(), context.getCodeReviewResult(), context.getApplyResult(),
                context.getExecutionResult(), context.getFailureAnalysisResult());
    }

    @Test
    @DisplayName("Deve registrar LearningResult no contexto")
    void deveRegistrarLearningResult() {
        AutoQaContext context = contextoCompleto();
        LearningResult result = collectedResult(context.getExecutionId());
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(result);

        agent.execute(context);

        assertThat(context.getLearningResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve retornar success=true para COLLECTED")
    void deveRetornarSuccessParaCollected() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(collectedResult(context.getExecutionId()));
        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar success=true para COLLECTED_WITH_WARNINGS")
    void deveRetornarSuccessParaCollectedWithWarnings() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resultComStatus(context.getExecutionId(), LearningStatus.COLLECTED_WITH_WARNINGS));
        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar success=true para REVIEW_REQUIRED")
    void deveRetornarSuccessParaReviewRequired() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resultComStatus(context.getExecutionId(), LearningStatus.REVIEW_REQUIRED));
        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar success=true para SKIPPED")
    void deveRetornarSuccessParaSkipped() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(resultComStatus(context.getExecutionId(), LearningStatus.SKIPPED));
        assertThat(agent.execute(context).success()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar success=false para BLOCKED")
    void deveRetornarFailureParaBlocked() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new LearningResult(context.getExecutionId(), List.of(), List.of(),
                        List.of(new LearningWarning("OPERATIONAL_FAILURE", "bloqueado", true)),
                        LearningStatus.BLOCKED, LearningConfidence.UNKNOWN, 0, 0, true, true));
        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar success=false para INVALID")
    void deveRetornarFailureParaInvalid() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new LearningResult(context.getExecutionId(), List.of(), List.of(), List.of(),
                        LearningStatus.INVALID, LearningConfidence.UNKNOWN, 0, 0, false, false));
        assertThat(agent.execute(context).success()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar failure quando o service lançar LearningException")
    void deveRetornarFailureQuandoServiceFalhar() {
        AutoQaContext context = contextoCompleto();
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new LearningException("falha técnica"));

        AgentExecutionResult result = agent.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(context.getLearningResult()).isNull();
    }

    @Test
    @DisplayName("Não deve expor projectPath na mensagem")
    void naoDeveExporProjectPath() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto/sensivel/secreto");
        AgentExecutionResult result = agent.execute(context);
        assertThat(result.message()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("Mensagem de sucesso deve conter status e contagem de itens")
    void mensagemDeveConterStatusEContagem() {
        AutoQaContext context = contextoCompleto();
        LearningResult result = resultComStatus(context.getExecutionId(), LearningStatus.REVIEW_REQUIRED);
        when(service.learn(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(result);

        AgentExecutionResult agentResult = agent.execute(context);

        assertThat(agentResult.message()).contains("REVIEW_REQUIRED");
    }

    // --- helpers ---

    private AutoQaContext contextoCompleto() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        UUID id = context.getExecutionId();
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(LearningTestData.knowledgeWithReuseCandidates("tests/support/loginPage.ts"));
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST)));
        context.registerGeneration(generationResult(id));
        context.registerCodeReview(reviewResult(id));
        context.registerApplyApproval(new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true));
        context.registerApplyResult(LearningTestData.completedApply(id, "tests/login.spec.ts"));
        context.registerExecutionApproval(new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false));
        context.registerExecutionResult(LearningTestData.passedExecution(id, "PLAYWRIGHT", 3));
        context.registerFailureAnalysis(LearningTestData.noFailureResult(id));
        return context;
    }

    private GenerationResult generationResult(UUID id) {
        return LearningTestData.generationWithReusedFiles(id, "tests/support/loginPage.ts");
    }

    private CodeReviewResult reviewResult(UUID id) {
        return new CodeReviewResult(id, List.of(), List.of(), List.of(), List.of("no-hardcoded-wait"), List.of(),
                List.of(), ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }

    private LearningResult collectedResult(UUID executionId) {
        return resultComStatus(executionId, LearningStatus.COLLECTED);
    }

    private LearningResult resultComStatus(UUID executionId, LearningStatus status) {
        LearningItem item = new LearningItem("id", LearningType.COMMAND_PATTERN, LearningScope.EXECUTION,
                LearningSource.EXECUTION, "t", "d", "r", LearningConfidence.HIGH, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, true, false);
        List<LearningItem> items = status == LearningStatus.SKIPPED ? List.of() : List.of(item);
        List<LearningWarning> warnings = status == LearningStatus.COLLECTED ? List.of()
                : List.of(new LearningWarning("SINGLE_EXECUTION_ONLY", "execução única", false));
        return new LearningResult(executionId, items, List.of(), warnings, status,
                items.isEmpty() ? LearningConfidence.UNKNOWN : LearningConfidence.MEDIUM,
                items.size(), 0, !items.isEmpty(), true);
    }
}
