package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TestExecutionService {

    private final AutoQaProperties properties;
    private final CommandPolicyService commandPolicyService;

    public TestExecutionResult execute(String executionId, Path projectPath, AutomationFramework framework) {
        return execute(executionId, projectPath, framework, PackageManager.UNKNOWN, null);
    }

    public TestExecutionResult execute(
            String executionId,
            Path projectPath,
            AutomationFramework framework,
            PackageManager packageManager,
            String specFile
    ) {
        if (!properties.isAllowCommandExecution()) {
            throw new IllegalStateException("Command execution is disabled (allowCommandExecution=false)");
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = -1;

        try {
            // 1) validação de compilação (tsc --noEmit)
            List<String> validationCommand = commandPolicyService.validationCommand(framework, packageManager);
            int validationExit = executeCommand(projectPath, validationCommand, stdout, stderr);
            if (validationExit != 0) {
                return new TestExecutionResult(
                        executionId,
                        framework != null ? framework.name() : "UNKNOWN",
                        String.join(" ", validationCommand),
                        validationExit,
                        stdout.toString(),
                        stderr.toString(),
                        LocalDateTime.now()
                );
            }

            // 2) execução de teste
            List<String> command = commandPolicyService.testCommand(framework, packageManager, specFile);
            exitCode = executeCommand(projectPath, command, stdout, stderr);

            return new TestExecutionResult(
                    executionId,
                    framework != null ? framework.name() : "UNKNOWN",
                    "",
                    exitCode,
                    stdout.toString(),
                    stderr.toString(),
                    LocalDateTime.now()
            );

        } catch (Exception ex) {
            stderr.append(ex.getMessage() != null ? ex.getMessage() : "Execution error");
        }

        return new TestExecutionResult(
                executionId,
                framework != null ? framework.name() : "UNKNOWN",
                "",
                exitCode,
                stdout.toString(),
                stderr.toString(),
                LocalDateTime.now()
        );
    }

    private int executeCommand(
            Path projectPath,
            List<String> command,
            StringBuilder stdout,
            StringBuilder stderr
    ) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(projectPath.toFile());

        Process process = processBuilder.start();

        Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdout));
        Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
        outThread.start();
        errThread.start();

        boolean finished = process.waitFor(properties.getMaxExecutionMinutes(), TimeUnit.MINUTES);
        int exitCode;
        if (!finished) {
            process.destroyForcibly();
            stderr.append("Process timed out.");
            exitCode = -1;
        } else {
            exitCode = process.exitValue();
        }

        outThread.join();
        errThread.join();
        return exitCode;
    }

    private void readStream(InputStream stream, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (Exception ignored) {
            // saída de processo: sem impacto crítico no fluxo
        }
    }
}
