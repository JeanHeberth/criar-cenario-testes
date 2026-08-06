package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.CommandNotAllowedException;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Único ponto que valida um CommandSpecification já montado por
 * CommandResolver. Verifica simultaneamente: commandId aprovado, executable
 * permitido, argumentos permitidos (sem metacaracteres de shell), framework
 * ou build tool compatível, e flags de aprovação compatíveis (ex.:
 * allowBuildCommand para variantes "clean test"). Nunca aceita uma string de
 * comando livre — todo o universo de comandos possíveis é fechado no
 * enum ExecutionCommandId.
 */
@Component
public class CommandPolicyService {

    private static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("[;|&`$<>\\n]|&&|\\|\\||\\$\\(");

    private static final Set<AutomationFramework> JVM_FRAMEWORKS = Set.of(
            AutomationFramework.SELENIDE, AutomationFramework.SELENIUM,
            AutomationFramework.REST_ASSURED, AutomationFramework.KARATE, AutomationFramework.APPIUM);

    private static final Map<ExecutionCommandId, PolicyRule> RULES = Map.ofEntries(
            Map.entry(ExecutionCommandId.NPM_TEST, new PolicyRule(
                    Set.of("npm", "npm.cmd"), List.of("test"),
                    d -> d.getAutomationFramework() == AutomationFramework.PLAYWRIGHT
                            || d.getAutomationFramework() == AutomationFramework.CYPRESS,
                    false)),
            Map.entry(ExecutionCommandId.NPM_TEST_E2E, new PolicyRule(
                    Set.of("npm", "npm.cmd"), List.of("run", "test:e2e"),
                    d -> d.getAutomationFramework() == AutomationFramework.PLAYWRIGHT, false)),
            Map.entry(ExecutionCommandId.PLAYWRIGHT_TEST, new PolicyRule(
                    Set.of("npx", "npx.cmd"), List.of("playwright", "test"),
                    d -> d.getAutomationFramework() == AutomationFramework.PLAYWRIGHT, false)),
            Map.entry(ExecutionCommandId.CYPRESS_RUN, new PolicyRule(
                    Set.of("npx", "npx.cmd"), List.of("cypress", "run"),
                    d -> d.getAutomationFramework() == AutomationFramework.CYPRESS, false)),
            Map.entry(ExecutionCommandId.CYPRESS_SCRIPT_RUN, new PolicyRule(
                    Set.of("npm", "npm.cmd"), List.of("run", "cy:run"),
                    d -> d.getAutomationFramework() == AutomationFramework.CYPRESS, false)),
            Map.entry(ExecutionCommandId.GRADLE_WRAPPER_TEST, new PolicyRule(
                    Set.of("./gradlew", "gradlew.bat"), List.of("test"),
                    d -> d.getBuildTool() == BuildTool.GRADLE, false)),
            Map.entry(ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST, new PolicyRule(
                    Set.of("./gradlew", "gradlew.bat"), List.of("clean", "test"),
                    d -> d.getBuildTool() == BuildTool.GRADLE, true)),
            Map.entry(ExecutionCommandId.MAVEN_WRAPPER_TEST, new PolicyRule(
                    Set.of("./mvnw", "mvnw.cmd"), List.of("test"),
                    d -> d.getBuildTool() == BuildTool.MAVEN, false)),
            Map.entry(ExecutionCommandId.MAVEN_TEST, new PolicyRule(
                    Set.of("mvn"), List.of("test"),
                    d -> d.getBuildTool() == BuildTool.MAVEN, false)),
            Map.entry(ExecutionCommandId.ROBOT_TEST, new PolicyRule(
                    Set.of("robot", "python", "python3", "py"), List.of(),
                    d -> d.getAutomationFramework() == AutomationFramework.ROBOT_FRAMEWORK, false)),
            Map.entry(ExecutionCommandId.PYTEST, new PolicyRule(
                    Set.of("pytest", "python", "python3", "py"), List.of(),
                    d -> d.getTestingFrameworks().contains(TestingFramework.PYTEST), false))
    );

    public void validate(CommandSpecification command, ProjectDiscoveryResult discovery, ExecutionApproval approval) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(approval, "approval must not be null");

        if (!approval.permits(command.commandId())) {
            throw new CommandNotAllowedException(
                    "ExecutionApproval não cobre o comando " + command.commandId());
        }

        PolicyRule rule = RULES.get(command.commandId());
        if (rule == null) {
            throw new CommandNotAllowedException("commandId desconhecido pela política: " + command.commandId());
        }

        if (!rule.allowedExecutables().contains(command.executable())) {
            throw new CommandNotAllowedException(
                    "executable não permitido para " + command.commandId() + ": " + command.executable());
        }

        if (rule.requiresAllowBuildCommand() && !approval.allowBuildCommand()) {
            throw new CommandNotAllowedException(
                    command.commandId() + " exige ExecutionApproval.allowBuildCommand=true");
        }

        if (!startsWithPrefix(command.arguments(), rule.requiredArgumentPrefix())) {
            throw new CommandNotAllowedException(
                    "argumentos não permitidos para " + command.commandId() + ": " + command.arguments());
        }

        validateNoForbiddenCharacters(command.executable());
        for (String argument : command.arguments()) {
            validateNoForbiddenCharacters(argument);
        }

        if (!rule.compatibility().test(discovery)) {
            throw new CommandNotAllowedException(
                    command.commandId() + " incompatível com o framework/build tool detectado");
        }
    }

    private boolean startsWithPrefix(List<String> arguments, List<String> prefix) {
        if (prefix.isEmpty()) {
            return true;
        }
        if (arguments.size() < prefix.size()) {
            return false;
        }
        return arguments.subList(0, prefix.size()).equals(prefix);
    }

    private void validateNoForbiddenCharacters(String value) {
        if (value == null) {
            return;
        }
        if (value.contains("\n") || FORBIDDEN_CHARACTERS.matcher(value).find()) {
            throw new CommandNotAllowedException("Caractere de shell não permitido em: " + value);
        }
    }

    private record PolicyRule(
            Set<String> allowedExecutables,
            List<String> requiredArgumentPrefix,
            Predicate<ProjectDiscoveryResult> compatibility,
            boolean requiresAllowBuildCommand
    ) {
    }
}
