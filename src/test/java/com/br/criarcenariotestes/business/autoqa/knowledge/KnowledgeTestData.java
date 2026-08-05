package com.br.criarcenariotestes.business.autoqa.knowledge;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataRequirement;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataType;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class KnowledgeTestData {

    private KnowledgeTestData() {
    }

    public static ProjectDiscoveryResult discovery(Path projectPath, AutomationFramework framework, AutomationLanguage language) {
        return new ProjectDiscoveryResult(
                projectPath,
                framework,
                language,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(framework),
                List.of(framework.name()),
                "config",
                List.of("config"),
                List.of(),
                DiscoveryConfidence.HIGH,
                true
        );
    }

    public static ProjectDiscoveryResult unknownDiscovery(Path projectPath) {
        return new ProjectDiscoveryResult(
                projectPath,
                AutomationFramework.UNKNOWN,
                AutomationLanguage.UNKNOWN,
                PackageManager.UNKNOWN,
                BuildTool.UNKNOWN,
                Set.of(),
                Set.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                DiscoveryConfidence.UNKNOWN,
                true
        );
    }

    public static ScenarioAnalysisResult analysis() {
        return new ScenarioAnalysisResult(
                "Login válido",
                "Validar acesso",
                List.of("Usuário cadastrado"),
                List.of(new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", List.of())),
                List.of(new TestDataRequirement("email", TestDataType.ENVIRONMENT_VARIABLE, true, "E-mail", null)),
                List.of(),
                List.of(),
                List.of(),
                List.of("Usuário"),
                List.of(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                List.of(),
                true
        );
    }

    public static ProjectComponent component(String relativePath, String name, ComponentType type, SourceLanguage language) {
        return new ProjectComponent(
                relativePath,
                name,
                type,
                language,
                "com.example",
                List.of(name),
                List.of("action"),
                List.of("import example"),
                List.of("@Tag"),
                List.of(type.name()),
                type == ComponentType.TEST,
                type != ComponentType.UNKNOWN,
                List.of()
        );
    }

    public static ProjectKnowledgeResult knowledge(Path projectPath, ProjectComponent component) {
        return new ProjectKnowledgeResult(
                projectPath,
                List.of(component),
                List.of(component),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ReuseCandidate(component.relativePath(), component.type(), "match", ReuseConfidence.HIGH, List.of("login"))),
                new NamingConvention("*.java", "*Page.java", "PascalCase", "camelCase", "src/test/java", List.of(component.relativePath()), ReuseConfidence.HIGH),
                List.of("src/test/java"),
                List.of("src/main/java"),
                List.of("node_modules"),
                List.of(),
                KnowledgeStatus.COMPLETE,
                true
        );
    }
}
