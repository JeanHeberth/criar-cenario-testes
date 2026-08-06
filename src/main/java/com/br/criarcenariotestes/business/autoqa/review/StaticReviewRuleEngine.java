package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewCategory;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validações determinísticas executadas antes da IA. Stateless, sem I/O, sem chamada
 * a provider de IA e sem correção de conteúdo — apenas produz achados (ReviewIssue).
 * Algumas regras (duplicidade, seletor frágil, método vazio) usam heurísticas de texto
 * em vez de AST completa, por limitação deliberada (ver relatório final da Fase 7).
 */
@Component
public class StaticReviewRuleEngine {

    static final int FILE_TOO_LARGE_THRESHOLD = 8_000;
    static final int DUPLICATED_LINE_THRESHOLD = 3;
    private static final int MIN_DUPLICATE_LINE_LENGTH = 12;

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");
    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```");
    private static final Pattern SECRET_KEY_VALUE = Pattern.compile(
            "(?i)\\b(password|senha|secret|apikey|api_key|private_key)\\b\\s*[:=]\\s*[\"']?[^\\s,;\"'`]{3,}");
    private static final Pattern CREDENTIAL_BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._-]{8,}");
    private static final Pattern URL_WITH_CREDENTIALS = Pattern.compile(
            "(?i)[a-z][a-z0-9+.-]*://[^\\s/?#:@]+:[^\\s/?#@]+@");
    private static final Pattern HARDCODED_URL = Pattern.compile("(?i)https?://[\\w.-]+");
    private static final Pattern THREAD_SLEEP = Pattern.compile("Thread\\.sleep\\s*\\(");
    private static final Pattern CYPRESS_FIXED_WAIT = Pattern.compile("cy\\.wait\\s*\\(\\s*\\d+");
    private static final Pattern PLAYWRIGHT_FIXED_WAIT = Pattern.compile("waitForTimeout\\s*\\(");
    private static final Pattern ROBOT_SLEEP = Pattern.compile("(?m)^\\s*Sleep\\s+\\S+");
    private static final Pattern WILDCARD_IMPORT = Pattern.compile("(?m)^\\s*import\\s+[\\w.]+\\.\\*;");
    private static final Pattern GENERIC_EXCEPTION = Pattern.compile("catch\\s*\\(\\s*(Exception|Throwable|Error)\\s+\\w+\\s*\\)");
    private static final Pattern EMPTY_BLOCK = Pattern.compile("\\{\\s*\\}");
    private static final Pattern FRAGILE_SELECTOR = Pattern.compile(":nth-child|(>\\s*[\\w.#\\[\\]]+\\s*){3,}");
    private static final Pattern ASSERTION_EVIDENCE = Pattern.compile(
            "expect\\(|assertThat|Assertions\\.|\\.should\\(|assert |Should Be|Should Contain");

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

    public List<ReviewIssue> review(AutomationFramework framework,
                                     AutomationLanguage language,
                                     TechnicalPlanResult plan,
                                     GenerationResult generation,
                                     List<GeneratedArtifactReader.ReadArtifact> artifacts) {
        List<ReviewIssue> issues = new ArrayList<>();
        issues.addAll(reviewPlanAdherence(plan, generation));
        for (GeneratedArtifactReader.ReadArtifact artifact : artifacts) {
            issues.addAll(reviewFile(framework, language, artifact));
        }
        return List.copyOf(issues);
    }

    private List<ReviewIssue> reviewPlanAdherence(TechnicalPlanResult plan, GenerationResult generation) {
        List<ReviewIssue> issues = new ArrayList<>();

        Map<String, PlannedFileAction> plannedCreateOrUpdate = new LinkedHashMap<>();
        for (PlannedFileAction action : plan.fileActions()) {
            if (action == null) continue;
            if (action.operation() == FileOperation.CREATE || action.operation() == FileOperation.UPDATE) {
                plannedCreateOrUpdate.put(action.relativePath(), action);
            }
        }

        Map<String, GeneratedFile> generatedCreateOrUpdate = new LinkedHashMap<>();
        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() == GeneratedFileOperation.CREATE || file.operation() == GeneratedFileOperation.UPDATE) {
                generatedCreateOrUpdate.put(file.relativePath(), file);
            }
        }

        for (String plannedPath : plannedCreateOrUpdate.keySet()) {
            if (!generatedCreateOrUpdate.containsKey(plannedPath)) {
                issues.add(issue(ReviewRule.PLAN_FILE_MISSING, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        plannedPath, null, "Arquivo planejado não foi gerado", null,
                        "Gerar o arquivo planejado ou justificar formalmente a omissão"));
            }
        }

        for (Map.Entry<String, GeneratedFile> entry : generatedCreateOrUpdate.entrySet()) {
            PlannedFileAction planned = plannedCreateOrUpdate.get(entry.getKey());
            if (planned == null) {
                issues.add(issue(ReviewRule.PLAN_FILE_EXTRA, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        entry.getKey(), null, "Arquivo gerado não estava no plano técnico", null,
                        "Remover o arquivo ou atualizar o plano técnico"));
                continue;
            }
            if (!planned.operation().name().equals(entry.getValue().operation().name())) {
                issues.add(issue(ReviewRule.PLAN_OPERATION_MISMATCH, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        entry.getKey(), null, "Operação divergente do plano técnico",
                        "plano=" + planned.operation() + " gerado=" + entry.getValue().operation(),
                        "Ajustar a operação para corresponder ao plano"));
            }
            if (planned.componentType() != entry.getValue().componentType()) {
                issues.add(issue(ReviewRule.PLAN_COMPONENT_MISMATCH, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.MEDIUM,
                        entry.getKey(), null, "Tipo de componente divergente do plano técnico",
                        "plano=" + planned.componentType() + " gerado=" + entry.getValue().componentType(),
                        "Ajustar o componentType para corresponder ao plano"));
            }
        }

        return issues;
    }

    private List<ReviewIssue> reviewFile(AutomationFramework framework, AutomationLanguage language,
                                          GeneratedArtifactReader.ReadArtifact artifact) {
        List<ReviewIssue> issues = new ArrayList<>();
        String path = artifact.relativePath();
        String content = artifact.content();
        String extension = extensionOf(path);

        if (PATH_TRAVERSAL.matcher(path).find()) {
            issues.add(critical(ReviewRule.PATH_TRAVERSAL, ReviewCategory.SECURITY, path, "Path traversal detectado no caminho gerado"));
        }
        if (ABSOLUTE_UNIX.matcher(path).find() || ABSOLUTE_WINDOWS.matcher(path).find()
                || ABSOLUTE_UNC.matcher(path).find() || FILE_URI.matcher(path).find()) {
            issues.add(critical(ReviewRule.ABSOLUTE_PATH, ReviewCategory.SECURITY, path, "Caminho absoluto detectado"));
        }

        FrameworkRule rule = FRAMEWORK_RULES.get(framework);
        if (rule != null) {
            if (!rule.extensions().contains(extension)) {
                issues.add(issue(ReviewRule.INVALID_EXTENSION, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.MEDIUM,
                        path, null, "Extensão incompatível com o framework " + framework, extension,
                        "Usar uma das extensões esperadas: " + rule.extensions()));
            } else if (!"json".equals(extension)) {
                boolean hasEvidence = rule.requiredAny().stream().anyMatch(content::contains);
                if (!hasEvidence) {
                    issues.add(issue(ReviewRule.FRAMEWORK_MISMATCH, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.HIGH,
                            path, null, "Nenhuma evidência do framework " + framework + " encontrada", null,
                            "Confirmar se o arquivo pertence ao framework correto"));
                }
                for (String forbidden : rule.forbidden()) {
                    if (content.contains(forbidden)) {
                        issues.add(issue(ReviewRule.FRAMEWORK_MISMATCH, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.HIGH,
                                path, null, "Evidência de framework incompatível detectada", forbidden,
                                "Remover código de framework incompatível"));
                    }
                }
            }
        }

        if (!isExtensionCompatibleWithLanguage(language, extension)) {
            issues.add(issue(ReviewRule.LANGUAGE_MISMATCH, ReviewCategory.LANGUAGE_COMPATIBILITY, ReviewSeverity.HIGH,
                    path, null, "Extensão incompatível com a linguagem " + language, extension,
                    "Ajustar a extensão conforme a linguagem do projeto"));
        }

        if (MARKDOWN_FENCE.matcher(content).find()) {
            issues.add(issue(ReviewRule.MARKDOWN_FENCE, ReviewCategory.CODE_QUALITY, ReviewSeverity.HIGH,
                    path, null, "Bloco Markdown encontrado no conteúdo gerado", "```", "Remover marcação Markdown do arquivo"));
        }

        if (SECRET_KEY_VALUE.matcher(content).find()) {
            issues.add(critical(ReviewRule.HARDCODED_SECRET, ReviewCategory.SECURITY, path, "Possível segredo hardcoded detectado"));
        }
        if (CREDENTIAL_BEARER.matcher(content).find() || URL_WITH_CREDENTIALS.matcher(content).find()) {
            issues.add(critical(ReviewRule.HARDCODED_CREDENTIAL, ReviewCategory.SECURITY, path, "Possível credencial hardcoded detectada"));
        }
        if (HARDCODED_URL.matcher(content).find() && !URL_WITH_CREDENTIALS.matcher(content).find()) {
            issues.add(issue(ReviewRule.HARDCODED_URL, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "URL hardcoded encontrada", null, "Externalizar a URL para configuração"));
        }

        if (THREAD_SLEEP.matcher(content).find()) {
            issues.add(issue(ReviewRule.THREAD_SLEEP, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.HIGH,
                    path, null, "Uso de Thread.sleep detectado", "Thread.sleep(", "Substituir por espera explícita/condicional"));
        }
        if (CYPRESS_FIXED_WAIT.matcher(content).find() || PLAYWRIGHT_FIXED_WAIT.matcher(content).find()) {
            issues.add(issue(ReviewRule.FIXED_WAIT, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.MEDIUM,
                    path, null, "Espera fixa detectada", null, "Preferir espera baseada em condição"));
        }
        if (ROBOT_SLEEP.matcher(content).find()) {
            issues.add(issue(ReviewRule.SLEEP_USAGE, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.MEDIUM,
                    path, null, "Uso de Sleep fixo detectado", null, "Preferir 'Wait Until Keyword Succeeds'"));
        }

        if (FRAGILE_SELECTOR.matcher(content).find()) {
            issues.add(issue(ReviewRule.FRAGILE_SELECTOR, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "Seletor potencialmente frágil detectado", null, "Preferir seletor semântico ou atributo de teste"));
        }

        if (artifact.componentType() == PlanComponentType.TEST && !ASSERTION_EVIDENCE.matcher(content).find()) {
            issues.add(issue(ReviewRule.MISSING_ASSERTION, ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                    path, null, "Arquivo de teste sem assertion aparente", null, "Adicionar assertion ao teste"));
        }

        if (EMPTY_BLOCK.matcher(content).find()) {
            ReviewRule emptyRule = artifact.componentType() == PlanComponentType.TEST ? ReviewRule.EMPTY_TEST : ReviewRule.EMPTY_METHOD;
            issues.add(issue(emptyRule, ReviewCategory.TEST_DESIGN, ReviewSeverity.MEDIUM,
                    path, null, "Bloco de código vazio detectado", null, "Implementar o corpo do método/teste"));
        }

        if (WILDCARD_IMPORT.matcher(content).find()) {
            issues.add(issue(ReviewRule.WILDCARD_IMPORT, ReviewCategory.IMPORT, ReviewSeverity.LOW,
                    path, null, "Import wildcard detectado", null, "Importar somente os símbolos necessários"));
        }

        if (GENERIC_EXCEPTION.matcher(content).find()) {
            issues.add(issue(ReviewRule.GENERIC_EXCEPTION, ReviewCategory.ERROR_HANDLING, ReviewSeverity.MEDIUM,
                    path, null, "Captura de exceção genérica detectada", null, "Capturar exceções específicas"));
        }

        if (hasDuplicatedLines(content)) {
            issues.add(issue(ReviewRule.DUPLICATED_CODE, ReviewCategory.DUPLICATION, ReviewSeverity.LOW,
                    path, null, "Linhas duplicadas detectadas", null, "Extrair trecho repetido para um helper"));
        }

        if (content.length() > FILE_TOO_LARGE_THRESHOLD) {
            issues.add(issue(ReviewRule.FILE_TOO_LARGE, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "Arquivo gerado muito grande", null, "Considerar dividir em arquivos menores"));
        }

        if (!matchesNamingConvention(framework, artifact.componentType(), path)) {
            issues.add(issue(ReviewRule.NAMING_CONVENTION_MISMATCH, ReviewCategory.NAMING, ReviewSeverity.LOW,
                    path, null, "Nome de arquivo fora do padrão esperado", null, "Ajustar nome do arquivo à convenção do framework"));
        }

        return issues;
    }

    private boolean isExtensionCompatibleWithLanguage(AutomationLanguage language, String extension) {
        if (language == null) return true;
        return switch (language) {
            case TYPESCRIPT -> Set.of("ts", "js", "json").contains(extension);
            case JAVASCRIPT -> Set.of("js", "json").contains(extension);
            case JAVA -> Set.of("java").contains(extension);
            case PYTHON -> Set.of("py").contains(extension);
            case ROBOT -> Set.of("robot", "resource").contains(extension);
            case CSHARP -> Set.of("cs").contains(extension);
            case UNKNOWN -> true;
        };
    }

    private boolean matchesNamingConvention(AutomationFramework framework, PlanComponentType componentType, String path) {
        if (componentType != PlanComponentType.TEST) return true;
        String fileName = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return switch (framework) {
            case PLAYWRIGHT, CYPRESS -> fileName.contains("spec") || fileName.contains("test") || fileName.contains("cy");
            case SELENIDE, SELENIUM, REST_ASSURED -> fileName.endsWith("test.java") || fileName.endsWith("tests.java") || fileName.endsWith("it.java");
            case ROBOT_FRAMEWORK -> true;
            default -> true;
        };
    }

    private boolean hasDuplicatedLines(String content) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.length() < MIN_DUPLICATE_LINE_LENGTH) continue;
            counts.merge(trimmed, 1, Integer::sum);
        }
        return counts.values().stream().anyMatch(count -> count >= DUPLICATED_LINE_THRESHOLD);
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private ReviewIssue issue(ReviewRule rule, ReviewCategory category, ReviewSeverity severity, String relativePath,
                               Integer line, String message, String evidence, String recommendation) {
        return new ReviewIssue(rule.name(), category, severity, relativePath, line, message, evidence, recommendation,
                severity == ReviewSeverity.CRITICAL);
    }

    private ReviewIssue critical(ReviewRule rule, ReviewCategory category, String relativePath, String message) {
        return issue(rule, category, ReviewSeverity.CRITICAL, relativePath, null, message, null,
                "Revisar imediatamente - risco de segurança");
    }
}
