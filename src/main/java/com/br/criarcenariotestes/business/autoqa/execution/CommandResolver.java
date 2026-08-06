package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.execution.CommandSpecification;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resolve um CommandSpecification permitido a partir de evidências
 * determinísticas (framework/build tool detectados, presença de wrapper no
 * disco, testDirectories já conhecidos). Nunca infere comando a partir de
 * IA, usuário ou leitura de package.json — scripts npm (NPM_TEST,
 * NPM_TEST_E2E, CYPRESS_SCRIPT_RUN) ficam adiados por falta de evidência
 * tipada nesta fase. Framework/build tool UNKNOWN ou sem wrapper comprovado
 * resultam em Optional.empty() (BLOCKED).
 */
@Component
public class CommandResolver {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private static final Set<String> ENVIRONMENT_ALLOWLIST = Set.of(
            "PATH", "HOME", "USERPROFILE", "TMPDIR", "TMP", "TEMP", "JAVA_HOME", "SYSTEMROOT");

    private static final Set<AutomationFramework> JVM_FRAMEWORKS = EnumSet.of(
            AutomationFramework.SELENIDE, AutomationFramework.SELENIUM,
            AutomationFramework.REST_ASSURED, AutomationFramework.KARATE, AutomationFramework.APPIUM);

    private java.util.function.Supplier<String> osNameSupplier = () -> System.getProperty("os.name", "");
    private Map<String, String> hostEnvironment = System.getenv();

    /** Visível apenas para testes: permite simular Windows/macOS/Linux sem depender do SO real. */
    void setOsNameSupplier(java.util.function.Supplier<String> osNameSupplier) {
        this.osNameSupplier = Objects.requireNonNull(osNameSupplier, "osNameSupplier must not be null");
    }

    /** Visível apenas para testes: permite controlar quais variáveis "existem" no ambiente hospedeiro. */
    void setHostEnvironment(Map<String, String> hostEnvironment) {
        this.hostEnvironment = Objects.requireNonNull(hostEnvironment, "hostEnvironment must not be null");
    }

    public Optional<CommandSpecification> resolve(ProjectDiscoveryResult discovery,
                                                    ProjectKnowledgeResult knowledge,
                                                    ExecutionApproval approval) {
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(approval, "approval must not be null");

        Path projectRoot = discovery.getNormalizedProjectPath();
        if (!isValidProjectRoot(projectRoot)) {
            return Optional.empty();
        }

        List<Candidate> candidates = buildCandidates(discovery, knowledge, projectRoot);

        for (Candidate candidate : candidates) {
            if (!candidate.structurallyResolvable()) {
                continue;
            }
            if (candidate.requiresAllowBuildCommand() && !approval.allowBuildCommand()) {
                continue;
            }
            if (!approval.permits(candidate.commandId())) {
                continue;
            }
            return Optional.of(new CommandSpecification(
                    candidate.commandId(), candidate.executable(), candidate.arguments(),
                    sanitizeWorkingDirectory(projectRoot), DEFAULT_TIMEOUT, buildEnvironment(), candidate.type()
            ));
        }
        return Optional.empty();
    }

    private List<Candidate> buildCandidates(ProjectDiscoveryResult discovery, ProjectKnowledgeResult knowledge, Path projectRoot) {
        List<Candidate> candidates = new ArrayList<>();
        AutomationFramework framework = discovery.getAutomationFramework();
        BuildTool buildTool = discovery.getBuildTool();

        if (framework == AutomationFramework.PLAYWRIGHT) {
            candidates.add(new Candidate(ExecutionCommandId.PLAYWRIGHT_TEST, npxExecutable(),
                    List.of("playwright", "test"), ExecutionCommandType.TEST, true, false));
        } else if (framework == AutomationFramework.CYPRESS) {
            candidates.add(new Candidate(ExecutionCommandId.CYPRESS_RUN, npxExecutable(),
                    List.of("cypress", "run"), ExecutionCommandType.TEST, true, false));
        } else if (JVM_FRAMEWORKS.contains(framework)) {
            candidates.addAll(buildJvmCandidates(buildTool, projectRoot));
        } else if (framework == AutomationFramework.ROBOT_FRAMEWORK) {
            buildRobotCandidate(knowledge).ifPresent(candidates::add);
        }

        if (candidates.isEmpty() && discovery.getTestingFrameworks().contains(TestingFramework.PYTEST)) {
            candidates.add(new Candidate(ExecutionCommandId.PYTEST, pythonExecutable(),
                    List.of("-m", "pytest"), ExecutionCommandType.TEST, true, false));
        }

        return candidates;
    }

    private List<Candidate> buildJvmCandidates(BuildTool buildTool, Path projectRoot) {
        List<Candidate> candidates = new ArrayList<>();
        if (buildTool == BuildTool.GRADLE) {
            String gradlew = isWindows() ? "gradlew.bat" : "./gradlew";
            boolean wrapperExists = Files.exists(projectRoot.resolve(isWindows() ? "gradlew.bat" : "gradlew"));
            candidates.add(new Candidate(ExecutionCommandId.GRADLE_WRAPPER_TEST, gradlew,
                    List.of("test"), ExecutionCommandType.TEST, wrapperExists, false));
            candidates.add(new Candidate(ExecutionCommandId.GRADLE_WRAPPER_CLEAN_TEST, gradlew,
                    List.of("clean", "test"), ExecutionCommandType.BUILD_AND_TEST, wrapperExists, true));
        } else if (buildTool == BuildTool.MAVEN) {
            String mvnw = isWindows() ? "mvnw.cmd" : "./mvnw";
            boolean wrapperExists = Files.exists(projectRoot.resolve(isWindows() ? "mvnw.cmd" : "mvnw"));
            candidates.add(new Candidate(ExecutionCommandId.MAVEN_WRAPPER_TEST, mvnw,
                    List.of("test"), ExecutionCommandType.TEST, wrapperExists, false));
            candidates.add(new Candidate(ExecutionCommandId.MAVEN_TEST, "mvn",
                    List.of("test"), ExecutionCommandType.TEST, true, false));
        }
        return candidates;
    }

    private Optional<Candidate> buildRobotCandidate(ProjectKnowledgeResult knowledge) {
        if (knowledge.testDirectories().isEmpty()) {
            return Optional.empty();
        }
        String testDirectory = knowledge.testDirectories().get(0);
        return Optional.of(new Candidate(ExecutionCommandId.ROBOT_TEST, pythonExecutable(),
                List.of("-m", "robot", testDirectory), ExecutionCommandType.TEST, true, false));
    }

    private boolean isValidProjectRoot(Path projectRoot) {
        return projectRoot != null
                && Files.exists(projectRoot)
                && Files.isDirectory(projectRoot)
                && !Files.isSymbolicLink(projectRoot);
    }

    private String sanitizeWorkingDirectory(Path projectRoot) {
        Path fileName = projectRoot.getFileName();
        return fileName != null ? fileName.toString() : "project";
    }

    private Map<String, String> buildEnvironment() {
        Map<String, String> environment = new LinkedHashMap<>();
        for (String key : ENVIRONMENT_ALLOWLIST) {
            String value = hostEnvironment.get(key);
            if (value != null) {
                environment.put(key, value);
            }
        }
        return environment;
    }

    private boolean isWindows() {
        return osNameSupplier.get().toLowerCase().contains("win");
    }

    private String npxExecutable() {
        return isWindows() ? "npx.cmd" : "npx";
    }

    private String pythonExecutable() {
        return isWindows() ? "py" : "python3";
    }

    private record Candidate(
            ExecutionCommandId commandId,
            String executable,
            List<String> arguments,
            ExecutionCommandType type,
            boolean structurallyResolvable,
            boolean requiresAllowBuildCommand
    ) {
    }
}
