package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.CommandNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessStartException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTerminationException;
import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTimeoutException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionWarning;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestrador stateless da Fase 9. Executa os testes automatizados do
 * projeto analisado após uma aplicação válida (ApplyResult COMPLETED ou
 * COMPLETED_WITH_WARNINGS). Não usa IA, não altera arquivos, não corrige
 * testes, não instala dependências.
 */
@Service
public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);

    private final ExecutionPreconditionValidator preconditionValidator;
    private final CommandResolver commandResolver;
    private final CommandPolicyService commandPolicyService;
    private final ProcessExecutionService processExecutionService;
    private final ExecutionResultParser resultParser;
    private final ExecutionSummaryBuilder summaryBuilder;

    public TestExecutionService(ExecutionPreconditionValidator preconditionValidator,
                                 CommandResolver commandResolver,
                                 CommandPolicyService commandPolicyService,
                                 ProcessExecutionService processExecutionService,
                                 ExecutionResultParser resultParser,
                                 ExecutionSummaryBuilder summaryBuilder) {
        this.preconditionValidator = Objects.requireNonNull(preconditionValidator);
        this.commandResolver = Objects.requireNonNull(commandResolver);
        this.commandPolicyService = Objects.requireNonNull(commandPolicyService);
        this.processExecutionService = Objects.requireNonNull(processExecutionService);
        this.resultParser = Objects.requireNonNull(resultParser);
        this.summaryBuilder = Objects.requireNonNull(summaryBuilder);
    }

    public ExecutionResult execute(UUID executionId,
                                    ProjectDiscoveryResult discovery,
                                    ProjectKnowledgeResult knowledge,
                                    ApplyResult applyResult,
                                    ExecutionApproval approval) {
        if (executionId == null) throw new IllegalArgumentException("executionId must not be null");
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (knowledge == null) throw new IllegalArgumentException("knowledge must not be null");
        if (applyResult == null) throw new IllegalArgumentException("applyResult must not be null");

        preconditionValidator.validate(applyResult, approval);

        log.info("Execute started. executionId={}", executionId);

        Optional<CommandSpecification> resolved = commandResolver.resolve(discovery, knowledge, approval);
        if (resolved.isEmpty()) {
            log.warn("Execute blocked: no deterministic command resolved. executionId={}", executionId);
            return summaryBuilder.buildBlocked(executionId, null, new ExecutionWarning(
                    "MISSING_TEST_COMMAND", "Nenhum comando de teste pôde ser resolvido de forma determinística", true));
        }
        CommandSpecification command = resolved.get();

        try {
            commandPolicyService.validate(command, discovery, approval);
        } catch (CommandNotAllowedException e) {
            log.warn("Execute blocked by policy. executionId={}, commandId={}", executionId, command.commandId());
            return summaryBuilder.buildBlocked(executionId, command,
                    new ExecutionWarning("COMMAND_NOT_ALLOWED", e.getMessage(), true));
        }

        Instant attemptStartedAt = Instant.now();
        ProcessExecutionService.ProcessOutcome outcome;
        try {
            outcome = processExecutionService.execute(command, discovery.getNormalizedProjectPath());
        } catch (ProcessStartException e) {
            log.error("Execute failed to start process. executionId={}, commandId={}", executionId, command.commandId());
            return summaryBuilder.buildError(executionId, command, "PROCESS_START_FAILED", e.getMessage());
        } catch (ProcessTimeoutException e) {
            log.warn("Execute timed out. executionId={}, commandId={}", executionId, command.commandId());
            return summaryBuilder.buildTimedOut(executionId, command, attemptStartedAt, Instant.now(),
                    e.stdout(), e.stderr(), e.stdoutTruncated(), e.stderrTruncated());
        } catch (ProcessTerminationException e) {
            log.error("Execute failed to confirm process termination. executionId={}, commandId={}", executionId, command.commandId());
            return summaryBuilder.buildError(executionId, command, "PROCESS_TERMINATION_FAILED", e.getMessage());
        }

        List<TestExecutionSummary> summaries = resultParser.parse(command.commandId(), outcome.stdout());
        ExecutionResult result = summaryBuilder.buildCompleted(executionId, command, outcome, summaries);

        log.info("Execute finished. executionId={}, status={}, exitCode={}, duration={}",
                executionId, result.status(), result.exitCode(), result.duration());
        return result;
    }
}
