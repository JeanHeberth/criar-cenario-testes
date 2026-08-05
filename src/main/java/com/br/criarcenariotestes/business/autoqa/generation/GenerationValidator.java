package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GenerationValidator {

    static final int MAX_CONTENT_LENGTH = 20_000;
    static final int MAX_TOTAL_CONTENT_LENGTH = 100_000;

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");
    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```");
    private static final Pattern MULTI_JAVA_PACKAGE = Pattern.compile("(?m)^\\s*package\\s+[\\w.]+;");
    private static final Pattern MULTI_FILE_HEADER = Pattern.compile("(?m)^\\s*(//|#)\\s*(tests?|src|pages)/[\\w\\-./]+\\.(ts|js|java|py|robot|resource)\\s*$");

    private record FrameworkRule(Set<String> extensions, List<String> requiredAny, List<String> forbidden) {}

    private static final Map<AutomationFramework, FrameworkRule> FRAMEWORK_RULES = new EnumMap<>(AutomationFramework.class);

    static {
        FRAMEWORK_RULES.put(AutomationFramework.PLAYWRIGHT, new FrameworkRule(
                Set.of("ts", "js"),
                List.of("@playwright/test", "Page", "Locator", "expect(", "test("),
                List.of("cy.", "Cypress.Commands")
        ));
        FRAMEWORK_RULES.put(AutomationFramework.CYPRESS, new FrameworkRule(
                Set.of("ts", "js", "json"),
                List.of("describe(", "it(", "cy."),
                List.of("@playwright/test")
        ));
        FRAMEWORK_RULES.put(AutomationFramework.SELENIDE, new FrameworkRule(
                Set.of("java"),
                List.of("Selenide", "SelenideElement", "com.codeborne.selenide"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.SELENIUM, new FrameworkRule(
                Set.of("java"),
                List.of("WebDriver", "WebElement", "org.openqa.selenium"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.REST_ASSURED, new FrameworkRule(
                Set.of("java"),
                List.of("RestAssured", "RequestSpecification", "Response", "io.restassured"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.ROBOT_FRAMEWORK, new FrameworkRule(
                Set.of("robot", "resource"),
                List.of("*** Test Cases ***", "*** Keywords ***", "Resource", "Library"),
                List.of()
        ));
    }

    public GenerationResult validate(GenerationResult result,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan) {
        if (result == null) throw new GenerationValidationException("result must not be null");
        if (discovery == null) throw new GenerationValidationException("discovery must not be null");
        if (knowledge == null) throw new GenerationValidationException("knowledge must not be null");
        if (plan == null) throw new GenerationValidationException("plan must not be null");
        if (result.status() == null) throw new GenerationValidationException("status ausente");
        if (result.files() == null) throw new GenerationValidationException("files ausente");

        AutomationFramework framework = discovery.getAutomationFramework();
        FrameworkRule rule = FRAMEWORK_RULES.get(framework);
        if (rule == null) {
            throw new GenerationValidationException("Framework não suportado para geração: " + framework);
        }

        Map<String, PlannedFileAction> plannedCreateOrUpdate = new LinkedHashMap<>();
        for (PlannedFileAction action : plan.fileActions()) {
            if (action == null) continue;
            if (action.operation() == FileOperation.CREATE || action.operation() == FileOperation.UPDATE) {
                plannedCreateOrUpdate.put(action.relativePath(), action);
            }
        }

        Set<String> existingComponentPaths = knowledge.components() == null ? Set.of() :
                knowledge.components().stream()
                        .filter(Objects::nonNull)
                        .map(c -> c.relativePath())
                        .collect(java.util.stream.Collectors.toSet());

        Set<String> seenPaths = new LinkedHashSet<>();
        long totalContentLength = 0;

        for (int i = 0; i < result.files().size(); i++) {
            GeneratedFile file = result.files().get(i);
            if (file == null) throw new GenerationValidationException("files[" + i + "] nulo");

            String path = file.relativePath();
            if (path == null || path.isBlank()) throw new GenerationValidationException("files[" + i + "].relativePath ausente");

            if (PATH_TRAVERSAL.matcher(path).find()) throw new GenerationValidationException("Path traversal detectado: " + path);
            if (ABSOLUTE_UNIX.matcher(path).find()) throw new GenerationValidationException("Caminho absoluto Unix detectado: " + path);
            if (ABSOLUTE_WINDOWS.matcher(path).find()) throw new GenerationValidationException("Caminho absoluto Windows detectado: " + path);
            if (ABSOLUTE_UNC.matcher(path).find()) throw new GenerationValidationException("Caminho UNC detectado: " + path);
            if (FILE_URI.matcher(path).find()) throw new GenerationValidationException("File URI detectado: " + path);

            if (!seenPaths.add(path)) throw new GenerationValidationException("relativePath duplicado: " + path);

            if (file.operation() != GeneratedFileOperation.CREATE && file.operation() != GeneratedFileOperation.UPDATE) {
                throw new GenerationValidationException("Operação inválida vinda da IA para " + path + ": " + file.operation());
            }

            PlannedFileAction planned = plannedCreateOrUpdate.get(path);
            if (planned == null) {
                throw new GenerationValidationException("Arquivo não planejado: " + path);
            }
            if (!planned.operation().name().equals(file.operation().name())) {
                throw new GenerationValidationException("Operação divergente do plano para " + path);
            }

            boolean expectedExistingFile = planned.operation() == FileOperation.UPDATE;
            if (file.existingFile() != expectedExistingFile) {
                throw new GenerationValidationException("existingFile incoerente com a operação para " + path);
            }

            String content = file.content();
            if (content == null || content.isBlank()) {
                throw new GenerationValidationException("content vazio para " + path);
            }
            if (content.length() > MAX_CONTENT_LENGTH) {
                throw new GenerationValidationException("content excede o limite por arquivo: " + path);
            }
            totalContentLength += content.length();
            if (totalContentLength > MAX_TOTAL_CONTENT_LENGTH) {
                throw new GenerationValidationException("content total excede o limite permitido");
            }
            if (MARKDOWN_FENCE.matcher(content).find()) {
                throw new GenerationValidationException("Markdown fence detectado no content de " + path);
            }
            if (MULTI_JAVA_PACKAGE.matcher(content).results().count() > 1) {
                throw new GenerationValidationException("Múltiplos arquivos concatenados detectados em " + path);
            }
            if (MULTI_FILE_HEADER.matcher(content).results().count() > 1) {
                throw new GenerationValidationException("Múltiplos arquivos concatenados detectados em " + path);
            }

            validateExtension(path, rule);
            validateFrameworkEvidence(path, content, rule);

            for (String reused : file.reusedComponents()) {
                if (reused != null && !existingComponentPaths.contains(reused)) {
                    throw new GenerationValidationException("reusedComponents referencia componente inexistente: " + reused);
                }
            }
        }

        Set<String> uncovered = new LinkedHashSet<>(plannedCreateOrUpdate.keySet());
        uncovered.removeAll(seenPaths);

        if (!uncovered.isEmpty() && result.status() != GenerationStatus.PARTIAL) {
            throw new GenerationValidationException("Ação planejada omitida sem warning: " + uncovered);
        }
        if (!uncovered.isEmpty() && result.warnings().isEmpty()) {
            throw new GenerationValidationException("Omissão de ação planejada requer warning explicativo: " + uncovered);
        }

        validateStatusCoherence(result, uncovered.isEmpty());

        return result;
    }

    private void validateExtension(String path, FrameworkRule rule) {
        int dot = path.lastIndexOf('.');
        String extension = dot >= 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!rule.extensions().contains(extension)) {
            throw new GenerationValidationException("Extensão incompatível com o framework: " + path);
        }
    }

    private void validateFrameworkEvidence(String path, String content, FrameworkRule rule) {
        int dot = path.lastIndexOf('.');
        String extension = dot >= 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if ("json".equals(extension)) {
            return;
        }
        boolean hasEvidence = rule.requiredAny().stream().anyMatch(content::contains);
        if (!hasEvidence) {
            throw new GenerationValidationException("Nenhuma evidência do framework encontrada em " + path);
        }
        for (String forbidden : rule.forbidden()) {
            if (content.contains(forbidden)) {
                throw new GenerationValidationException("Evidência de framework incompatível detectada em " + path + ": " + forbidden);
            }
        }
    }

    private void validateStatusCoherence(GenerationResult result, boolean allCovered) {
        switch (result.status()) {
            case FAILED -> {
                if (!result.files().isEmpty()) throw new GenerationValidationException("FAILED não deveria conter arquivos");
                if (result.valid()) throw new GenerationValidationException("FAILED deve ter valid=false");
            }
            case COMPLETED -> {
                if (!result.valid()) throw new GenerationValidationException("COMPLETED deve ter valid=true");
                if (!allCovered) throw new GenerationValidationException("COMPLETED deve cobrir todas as ações planejadas");
            }
            case COMPLETED_WITH_WARNINGS -> {
                if (!result.valid()) throw new GenerationValidationException("COMPLETED_WITH_WARNINGS deve ter valid=true");
                if (result.warnings().isEmpty()) throw new GenerationValidationException("COMPLETED_WITH_WARNINGS deve ter warnings");
                if (!allCovered) throw new GenerationValidationException("COMPLETED_WITH_WARNINGS deve cobrir todas as ações planejadas");
            }
            case PARTIAL -> {
                if (result.valid()) throw new GenerationValidationException("PARTIAL deve ter valid=false");
                if (result.warnings().isEmpty()) throw new GenerationValidationException("PARTIAL deve ter warnings explicando a omissão");
            }
        }
    }
}
