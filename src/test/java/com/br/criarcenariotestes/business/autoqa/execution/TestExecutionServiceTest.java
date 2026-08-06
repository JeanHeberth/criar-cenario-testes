package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.CommandNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ExecutionValidationException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessStartException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTerminationException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTimeoutException;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionStatus;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionWarning;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TestExecutionService - Testes Unitários")
class TestExecutionServiceTest {

    private ExecutionPreconditionValidator preconditionValidator;
    private CommandResolver commandResolver;
    private CommandPolicyService commandPolicyService;
    private ProcessExecutionService processExecutionService;
    private ExecutionResultParser resultParser;
    private ExecutionSummaryBuilder summaryBuilder;
    private TestExecutionService service;

    @BeforeEach
    void setUp() {
        preconditionValidator = Mockito.mock(ExecutionPreconditionValidator.class);
        commandResolver = Mockito.mock(CommandResolver.class);
        commandPolicyService = Mockito.mock(CommandPolicyService.class);
        processExecutionService = Mockito.mock(ProcessExecutionService.class);
        resultParser = Mockito.mock(ExecutionResultParser.class);
        summaryBuilder = Mockito.mock(ExecutionSummaryBuilder.class);
        service = new TestExecutionService(preconditionValidator, commandResolver, commandPolicyService,
                processExecutionService, resultParser, summaryBuilder);
    }

    private ProjectDiscoveryResult discovery() {
        return GenerationTestData.playwrightDiscovery();
    }

    private ProjectKnowledgeResult knowledge() {
        return new ProjectKnowledgeResult(Path.of("/project"), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                List.of(), List.of(), List.of(), List.of(), KnowledgeStatus.COMPLETE, true);
    }

    private ApplyResult applyResult(ApplyStatus status) {
        return new ApplyResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(),
                "projeto", ".auto-qa/backups/x", status, false, true);
    }

    private ExecutionApproval approval() {
        return new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
    }

    private CommandSpecification command() {
        return new CommandSpecification(ExecutionCommandId.PLAYWRIGHT_TEST, "npx", List.of("playwright", "test"),
                "projeto", Duration.ofMinutes(10), Map.of(), ExecutionCommandType.TEST);
    }

    private ProcessExecutionService.ProcessOutcome outcome(int exitCode) {
        Instant now = Instant.now();
        return new ProcessExecutionService.ProcessOutcome(exitCode, now, now.plusSeconds(1), "saida", "", false, false);
    }

    private ExecutionResult sampleResult(ExecutionStatus status) {
        return new ExecutionResult(UUID.randomUUID(), command(), status, status == ExecutionStatus.PASSED ? 0 : 1,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "saida", "", false, false, List.of(), List.of(),
                status != ExecutionStatus.ERROR && status != ExecutionStatus.TIMED_OUT);
    }

    @Test
    @DisplayName("Deve executar o comando permitido e retornar o resultado do summaryBuilder")
    void deveExecutarComandoPermitido() {
        UUID executionId = UUID.randomUUID();
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        when(resultParser.parse(any(), any())).thenReturn(List.of());
        ExecutionResult expected = sampleResult(ExecutionStatus.PASSED);
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(expected);

        ProjectDiscoveryResult discovery = discovery();
        ExecutionApproval approval = approval();
        ExecutionResult result = service.execute(executionId, discovery, knowledge(), applyResult(ApplyStatus.COMPLETED), approval);

        assertThat(result).isEqualTo(expected);
        verify(commandPolicyService).validate(command(), discovery, approval);
    }

    @Test
    @DisplayName("Deve retornar PASSED quando exitCode=0")
    void deveRetornarPassed() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.PASSED);
    }

    @Test
    @DisplayName("Deve retornar FAILED para exitCode não zero")
    void deveRetornarFailedParaExitCodeNaoZero() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(1));
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.FAILED));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    @DisplayName("Deve retornar TIMED_OUT quando ProcessExecutionService lança ProcessTimeoutException")
    void deveRetornarTimedOut() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any()))
                .thenThrow(new ProcessTimeoutException("timeout", "saida parcial", "", false, false));
        when(summaryBuilder.buildTimedOut(any(), any(), any(), any(), any(), any(), eq(false), eq(false)))
                .thenReturn(sampleResult(ExecutionStatus.TIMED_OUT));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.TIMED_OUT);
    }

    @Test
    @DisplayName("Deve retornar ERROR em falha técnica ao iniciar o processo")
    void deveRetornarErrorEmFalhaTecnica() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenThrow(new ProcessStartException("falhou"));
        when(summaryBuilder.buildError(any(), any(), eq("PROCESS_START_FAILED"), any()))
                .thenReturn(sampleResult(ExecutionStatus.ERROR));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.ERROR);
    }

    @Test
    @DisplayName("Deve retornar ERROR quando não confirma o encerramento do processo")
    void deveRetornarErrorQuandoFalhaTerminacao() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenThrow(new ProcessTerminationException("não confirmado"));
        when(summaryBuilder.buildError(any(), any(), eq("PROCESS_TERMINATION_FAILED"), any()))
                .thenReturn(sampleResult(ExecutionStatus.ERROR));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.ERROR);
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> service.execute(null, discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(commandResolver);
    }

    @Test
    @DisplayName("Deve rejeitar discovery nulo")
    void deveRejeitarDiscoveryNulo() {
        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), null, knowledge(), applyResult(ApplyStatus.COMPLETED), approval()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar knowledge nulo")
    void deveRejeitarKnowledgeNulo() {
        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), null, applyResult(ApplyStatus.COMPLETED), approval()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar applyResult nulo")
    void deveRejeitarApplyResultNulo() {
        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), null, approval()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve propagar rejeição quando aprovação é nula")
    void deveRejeitarApprovalNula() {
        doThrow(new ExecutionValidationException("aprovação obrigatória"))
                .when(preconditionValidator).validate(any(), eq(null));

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), null))
                .isInstanceOf(ExecutionValidationException.class);
        verifyNoInteractions(commandResolver);
    }

    @Test
    @DisplayName("Deve propagar rejeição quando ApplyStatus é BLOCKED")
    void deveRejeitarApplyBlocked() {
        doThrow(new ExecutionValidationException("ApplyResult BLOCKED não pode ser executado"))
                .when(preconditionValidator).validate(any(), any());

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.BLOCKED), approval()))
                .isInstanceOf(ExecutionValidationException.class);
        verifyNoInteractions(commandResolver, processExecutionService);
    }

    @Test
    @DisplayName("Deve propagar rejeição quando ApplyStatus é ROLLED_BACK")
    void deveRejeitarApplyRolledBack() {
        doThrow(new ExecutionValidationException("ApplyResult ROLLED_BACK não pode ser executado"))
                .when(preconditionValidator).validate(any(), any());

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.ROLLED_BACK), approval()))
                .isInstanceOf(ExecutionValidationException.class);
    }

    @Test
    @DisplayName("Deve propagar rejeição quando ApplyStatus é FAILED")
    void deveRejeitarApplyFailed() {
        doThrow(new ExecutionValidationException("ApplyResult FAILED não pode ser executado"))
                .when(preconditionValidator).validate(any(), any());

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.FAILED), approval()))
                .isInstanceOf(ExecutionValidationException.class);
    }

    @Test
    @DisplayName("Deve propagar rejeição quando approval.approved=false")
    void deveRejeitarApprovalFalse() {
        doThrow(new ExecutionValidationException("ExecutionApproval.approved deve ser true"))
                .when(preconditionValidator).validate(any(), any());

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval()))
                .isInstanceOf(ExecutionValidationException.class);
    }

    @Test
    @DisplayName("Deve bloquear (não iniciar processo) quando o comando não é permitido pela política")
    void deveRejeitarComandoNaoPermitido() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        doThrow(new CommandNotAllowedException("não permitido"))
                .when(commandPolicyService).validate(any(), any(), any());
        when(summaryBuilder.buildBlocked(any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.BLOCKED));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.BLOCKED);
        verifyNoInteractions(processExecutionService);
    }

    @Test
    @DisplayName("Deve usar o normalizedProjectPath do discovery como workingDirectory")
    void deveUsarProjectRootComoWorkingDirectory() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        ProjectDiscoveryResult discovery = discovery();
        service.execute(UUID.randomUUID(), discovery, knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        verify(processExecutionService).execute(command(), discovery.getNormalizedProjectPath());
    }

    @Test
    @DisplayName("Deve capturar stdout e stderr através do outcome retornado")
    void deveCapturarStdoutEStderr() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        ProcessExecutionService.ProcessOutcome outcomeComSaida = new ProcessExecutionService.ProcessOutcome(
                0, Instant.now(), Instant.now(), "stdout-x", "stderr-y", false, false);
        when(processExecutionService.execute(any(), any())).thenReturn(outcomeComSaida);
        when(summaryBuilder.buildCompleted(any(), any(), eq(outcomeComSaida), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        verify(summaryBuilder).buildCompleted(any(), any(), eq(outcomeComSaida), any());
    }

    @Test
    @DisplayName("Deve repassar flags de truncamento ao montar o resultado")
    void deveTruncarSaida() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        ProcessExecutionService.ProcessOutcome truncado = new ProcessExecutionService.ProcessOutcome(
                0, Instant.now(), Instant.now(), "x", "y", true, true);
        when(processExecutionService.execute(any(), any())).thenReturn(truncado);
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        verify(summaryBuilder).buildCompleted(any(), any(), eq(truncado), any());
    }

    @Test
    @DisplayName("Deve parsear o resumo de testes usando o commandId resolvido")
    void deveParsearResumo() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        List<TestExecutionSummary> summaries = List.of(new TestExecutionSummary("PLAYWRIGHT", 1, 1, 0, 0, 0, List.of(), List.of()));
        when(resultParser.parse(eq(ExecutionCommandId.PLAYWRIGHT_TEST), any())).thenReturn(summaries);
        when(summaryBuilder.buildCompleted(any(), any(), any(), eq(summaries))).thenReturn(sampleResult(ExecutionStatus.PASSED));

        service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        verify(resultParser).parse(ExecutionCommandId.PLAYWRIGHT_TEST, "saida");
    }

    @Test
    @DisplayName("Deve delegar ao summaryBuilder a decisão de adicionar warning quando o parsing falha")
    void deveAdicionarWarningQuandoNaoParseia() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        when(resultParser.parse(any(), any())).thenReturn(List.of());
        ExecutionResult withWarning = new ExecutionResult(UUID.randomUUID(), command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(1), "saida", "", false, false, List.of(),
                List.of(new ExecutionWarning("RESULT_PARSE_FAILED", "não parseado", false)), true);
        when(summaryBuilder.buildCompleted(any(), any(), any(), eq(List.of()))).thenReturn(withWarning);

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.warnings()).anySatisfy(w -> assertThat(w.code()).isEqualTo("RESULT_PARSE_FAILED"));
    }

    @Test
    @DisplayName("Deve ser stateless: chamadas independentes não compartilham estado")
    void deveSerStateless() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        ExecutionResult resultado1 = sampleResult(ExecutionStatus.PASSED);
        ExecutionResult resultado2 = sampleResult(ExecutionStatus.PASSED);
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(resultado1, resultado2);

        ExecutionResult r1 = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());
        ExecutionResult r2 = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(r1).isEqualTo(resultado1);
        assertThat(r2).isEqualTo(resultado2);
    }

    @Test
    @DisplayName("Não deve chamar processExecutionService quando não há comando resolvido")
    void deveNaoChamarProcessoSemComandoResolvido() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.empty());
        when(summaryBuilder.buildBlocked(any(), eq(null), any())).thenReturn(sampleResult(ExecutionStatus.BLOCKED));

        ExecutionResult result = service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        assertThat(result.status()).isEqualTo(ExecutionStatus.BLOCKED);
        verifyNoInteractions(processExecutionService);
        verifyNoInteractions(commandPolicyService);
    }

    @Test
    @DisplayName("Deve executar o processo exatamente uma vez por chamada, sem retry automático")
    void deveExecutarApenasUmaVez() {
        when(commandResolver.resolve(any(), any(), any())).thenReturn(Optional.of(command()));
        when(processExecutionService.execute(any(), any())).thenReturn(outcome(0));
        when(summaryBuilder.buildCompleted(any(), any(), any(), any())).thenReturn(sampleResult(ExecutionStatus.PASSED));

        service.execute(UUID.randomUUID(), discovery(), knowledge(), applyResult(ApplyStatus.COMPLETED), approval());

        verify(processExecutionService, times(1)).execute(any(), any());
    }
}
