package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.discovery.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class PlanningTestData {
    private PlanningTestData() {}

    public static TechnicalPlanResult readyPlan() {
        return new TechnicalPlanResult(
            "Plano de login",
            "Criar teste de login",
            List.of(createAction("tests/login.spec.ts")),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            PlanningStatus.READY,
            PlanningConfidence.HIGH,
            true
        );
    }

    public static TechnicalPlanResult readyWithWarningsPlan() {
        return new TechnicalPlanResult(
            "Plano com warnings",
            "Criar teste com ressalvas",
            List.of(createAction("tests/login.spec.ts")),
            List.of(),
            List.of(),
            List.of(),
            List.of(new PlanningWarning("W001", "Aviso de cobertura", false)),
            List.of(),
            List.of(),
            List.of(),
            PlanningStatus.READY_WITH_WARNINGS,
            PlanningConfidence.MEDIUM,
            true
        );
    }

    public static TechnicalPlanResult blockedPlan() {
        return new TechnicalPlanResult(
            "Plano bloqueado",
            "Decisão necessária",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(new PlanningWarning("W001", "Decisão humana necessária", true)),
            List.of(),
            List.of(),
            List.of(),
            PlanningStatus.BLOCKED,
            PlanningConfidence.LOW,
            false
        );
    }

    public static TechnicalPlanResult invalidPlan() {
        return new TechnicalPlanResult(
            "Plano inválido",
            "Cenário não pode ser planejado",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            PlanningStatus.INVALID,
            PlanningConfidence.UNKNOWN,
            false
        );
    }

    public static PlannedFileAction createAction(String path) {
        return new PlannedFileAction(
            path,
            FileOperation.CREATE,
            PlanComponentType.TEST,
            "Criar arquivo de teste",
            false,
            true,
            ApprovalRequirement.NONE,
            List.of(),
            List.of()
        );
    }

    public static PlannedFileAction reuseAction(String path) {
        return new PlannedFileAction(
            path,
            FileOperation.REUSE,
            PlanComponentType.PAGE_OBJECT,
            "Reutilizar componente existente",
            true,
            true,
            ApprovalRequirement.NONE,
            List.of(),
            List.of()
        );
    }

    public static PlannedFileAction updateAction(String path) {
        return new PlannedFileAction(
            path,
            FileOperation.UPDATE,
            PlanComponentType.PAGE_OBJECT,
            "Atualizar componente existente",
            true,
            true,
            ApprovalRequirement.FILE_UPDATE_REQUIRED,
            List.of(),
            List.of()
        );
    }

    public static ReuseDecision reuseDecision(String path, boolean reuse) {
        return new ReuseDecision(
            path,
            PlanComponentType.PAGE_OBJECT,
            reuse,
            "Componente compatível",
            PlanningConfidence.HIGH,
            List.of("login"),
            List.of()
        );
    }

    public static ProjectDiscoveryResult discovery() {
        return new ProjectDiscoveryResult(
            Path.of("/project"),
            AutomationFramework.PLAYWRIGHT,
            AutomationLanguage.TYPESCRIPT,
            PackageManager.NPM,
            BuildTool.NPM,
            Set.of(TestingFramework.PLAYWRIGHT_TEST),
            Set.of(AutomationFramework.PLAYWRIGHT),
            List.of("PLAYWRIGHT"),
            "playwright.config.ts",
            List.of("playwright.config.ts"),
            List.of(),
            DiscoveryConfidence.HIGH,
            true
        );
    }

    public static ScenarioAnalysisResult validScenario() {
        return new ScenarioAnalysisResult(
            "Login válido",
            "Validar acesso ao sistema",
            List.of("Usuário cadastrado"),
            List.of(new ScenarioStep(1, "Acessar login", "Tela exibida", List.of())),
            List.of(),
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

    public static ScenarioAnalysisResult invalidScenario() {
        return new ScenarioAnalysisResult(
            "Cenário inválido",
            "Não pode ser automatizado",
            List.of(),
            List.of(new ScenarioStep(1, "Ação", "Resultado", List.of())),
            List.of(),
            List.of(),
            List.of(),
            List.of(new ScenarioAmbiguity("Ambiguidade bloqueante", "Questão?", true)),
            List.of(),
            List.of(),
            AutomationType.WEB_UI,
            ScenarioAnalysisStatus.INVALID,
            List.of(),
            false
        );
    }

    public static ProjectKnowledgeResult completeKnowledge(String... existingPaths) {
        List<ProjectComponent> components = Arrays.stream(existingPaths)
            .map(p -> new ProjectComponent(p, nameFromPath(p), ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), false, true, List.of()))
            .toList();
        return buildKnowledge(components, KnowledgeStatus.COMPLETE);
    }

    public static ProjectKnowledgeResult partialKnowledge(String... existingPaths) {
        List<ProjectComponent> components = Arrays.stream(existingPaths)
            .map(p -> new ProjectComponent(p, nameFromPath(p), ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), false, true, List.of()))
            .toList();
        return buildKnowledge(components, KnowledgeStatus.PARTIAL);
    }

    public static ProjectKnowledgeResult emptyKnowledge() {
        return buildKnowledge(List.of(), KnowledgeStatus.EMPTY);
    }

    public static ProjectKnowledgeResult failedKnowledge() {
        return buildKnowledge(List.of(), KnowledgeStatus.FAILED);
    }

    private static ProjectKnowledgeResult buildKnowledge(List<ProjectComponent> components, KnowledgeStatus status) {
        return new ProjectKnowledgeResult(
            Path.of("/project"),
            components,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            new NamingConvention("*.ts", "*.spec.ts", "camelCase", "camelCase", "tests", List.of(), ReuseConfidence.HIGH),
            List.of("tests"),
            List.of("src"),
            List.of("node_modules"),
            List.of(),
            status,
            status != KnowledgeStatus.FAILED
        );
    }

    static String nameFromPath(String path) {
        String[] parts = path.split("[/\\\\]");
        String file = parts[parts.length - 1];
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }
}
