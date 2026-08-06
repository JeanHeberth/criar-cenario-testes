package com.br.criarcenariotestes.business.autoqa.execution.exception;

/**
 * Sinaliza que um CommandSpecification resolvido não passou na política de
 * allowlist (CommandPolicyService). É capturada internamente por
 * TestExecutionService e convertida em ExecutionResult com status BLOCKED —
 * nunca escapa para o ExecuteAgent.
 */
public class CommandNotAllowedException extends ExecutionException {
    public CommandNotAllowedException(String message) { super(message); }
}
