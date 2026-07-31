package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.TestExecutionResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Agente que executa suite de testes no projeto gerado.
 * Detecta framework e constrói comando apropriado.
 */
@Component
@RequiredArgsConstructor
public class TestExecutionAgent {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionAgent.class);

    public void execute(AutoQaContext context) {
        TestExecutionResult result = executeTests(context);
        if (result != null) {
            context.setTestExecutionResult(result);
        }
    }

    public TestExecutionResult executeTests(AutoQaContext context) {
        String projectPath = context.getProjectPath();
        Path projectDir = Path.of(projectPath);

        String framework = detectFramework(projectDir);
        String command = buildCommand(framework, projectDir);
        String executionId = context.executionIdAsString();

        return new TestExecutionResult(
                executionId,
                framework,
                command,
                -1,
                "",
                "",
                LocalDateTime.now()
        );
    }

    private String detectFramework(Path projectDir) {
        if (Files.exists(projectDir.resolve("package.json"))) {
            return "node";
        }
        if (Files.exists(projectDir.resolve("build.gradle")) ||
            Files.exists(projectDir.resolve("build.gradle.kts"))) {
            return "gradle";
        }
        if (Files.exists(projectDir.resolve("pom.xml"))) {
            return "maven";
        }
        return "unknown";
    }

    private String buildCommand(String framework, Path projectDir) {
        return switch (framework) {
            case "node" -> "npm run test";
            case "gradle" -> "./gradlew test";
            case "maven" -> "mvn test";
            default -> "";
        };
    }
}
