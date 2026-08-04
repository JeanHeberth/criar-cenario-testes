package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.ClassInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.MethodInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.MethodParameter;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalogEntry;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Agente de análise textual estruturada do projeto de automação.
 * Extrai classes, métodos, testes e padrões sem uso de IA.
 * Organizado para permitir futura migração para análise AST.
 */
@Component
public class ProjectAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisAgent.class);

    // ─── Padrões de classes ───────────────────────────────────────────────────

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:export\\s+)?(?:default\\s+)?(?:abstract\\s+)?(class|interface)\\s+(\\w+)",
            Pattern.MULTILINE
    );

    // ─── Padrões de métodos ───────────────────────────────────────────────────

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s{1,8}(async\\s+)?(?:public\\s+|private\\s+|protected\\s+)?(async\\s+)?" +
            "(\\w+)\\s*\\(([^)]*)\\)\\s*(?::\\s*([^{;\\n]+?))?\\s*[{;]",
            Pattern.MULTILINE
    );

    // ─── Padrões de teste ─────────────────────────────────────────────────────

    private static final Pattern DESCRIBE_PATTERN = Pattern.compile(
            "describe\\s*\\(\\s*['\"`]([^'\"`]+)['\"`]",
            Pattern.MULTILINE
    );

    private static final Pattern PARAM_PATTERN = Pattern.compile(
            "(\\w+)\\s*(?::\\s*([\\w<>\\[\\]|&, .]+?))?(?=\\s*,|\\s*$)"
    );

    private static final List<String> IGNORED_METHOD_NAMES = List.of(
            "constructor", "beforeAll", "beforeEach", "afterAll", "afterEach",
            "describe", "test", "it", "expect", "import", "export", "return", "if", "for"
    );

    private static final List<String> SPEC_EXTENSIONS = List.of(
            ".spec.ts", ".spec.js", ".cy.ts", ".cy.js", ".test.ts", ".test.js"
    );

    private static final List<String> FIXTURE_DIRS = List.of("fixtures", "fixture");
    private static final List<String> HELPER_DIRS = List.of("helpers", "utils", "support", "commands");

    public ProjectAnalysisResult analyze(ProjectCatalog catalog, AutomationFramework framework) {
        if (catalog == null || catalog.isEmpty()) {
            return emptyResult();
        }

        log.info("Analisando catálogo. arquivos={}, framework={}", catalog.getEntries().size(), framework);

        List<ClassInfo> allClasses = new ArrayList<>();
        List<String> testFiles = new ArrayList<>();
        List<String> fixtureFiles = new ArrayList<>();
        List<String> helperFiles = new ArrayList<>();
        List<String> customCommands = new ArrayList<>();
        List<String> describeBlocks = new ArrayList<>();
        List<String> testCases = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (ProjectCatalogEntry entry : catalog.entriesWithContent()) {
            String path = entry.relativePath();
            String content = entry.content();

            if (isTestFile(path)) {
                testFiles.add(path);
            }
            if (isInDirectory(path, FIXTURE_DIRS)) {
                fixtureFiles.add(path);
            }
            if (isInDirectory(path, HELPER_DIRS)) {
                helperFiles.add(path);
            }

            if (entry.isTypeScript() || entry.isJavaScript()) {
                allClasses.addAll(parseClasses(content, path));
                describeBlocks.addAll(parseDescribeBlocks(content));
                testCases.addAll(parseTestCases(content));
                if (framework == AutomationFramework.CYPRESS) {
                    customCommands.addAll(parseCypressCommands(content));
                }
            }
        }

        List<ClassInfo> pageObjects = allClasses.stream()
                .filter(ClassInfo::isPageObject)
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalClasses", allClasses.size());
        metadata.put("totalPageObjects", pageObjects.size());
        metadata.put("totalTestFiles", testFiles.size());
        metadata.put("framework", framework != null ? framework.name() : "UNKNOWN");

        log.info("Análise concluída. classes={}, pageObjects={}, testFiles={}",
                allClasses.size(), pageObjects.size(), testFiles.size());

        return ProjectAnalysisResult.builder()
                .classes(allClasses)
                .pageObjects(pageObjects)
                .testFiles(testFiles)
                .fixtureFiles(fixtureFiles)
                .helperFiles(helperFiles)
                .customCommands(customCommands)
                .describeBlocks(describeBlocks)
                .testCases(testCases)
                .conventions(List.of())
                .gaps(List.of())
                .warnings(warnings)
                .metadata(metadata)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    // ─── Parser de classes ────────────────────────────────────────────────────

    private List<ClassInfo> parseClasses(String content, String sourceFile) {
        List<ClassInfo> result = new ArrayList<>();
        Matcher classMatcher = CLASS_PATTERN.matcher(content);

        while (classMatcher.find()) {
            String type = classMatcher.group(1);
            String name = classMatcher.group(2);

            if (name == null || name.isBlank()) continue;

            // Extrai os métodos dentro do bloco da classe
            int classStart = classMatcher.end();
            String classBody = extractBlock(content, classStart);

            List<MethodInfo> methods = parseMethods(classBody, sourceFile);
            List<String> imports = parseImports(content);

            result.add(ClassInfo.builder()
                    .name(name)
                    .type(type)
                    .methods(methods)
                    .importStatements(imports)
                    .sourceFile(sourceFile)
                    .build());
        }
        return result;
    }

    // ─── Parser de métodos ────────────────────────────────────────────────────

    private List<MethodInfo> parseMethods(String classBody, String sourceFile) {
        if (classBody == null || classBody.isBlank()) return List.of();

        List<MethodInfo> result = new ArrayList<>();
        String[] lines = classBody.split("\n");
        Matcher methodMatcher = METHOD_PATTERN.matcher(classBody);

        while (methodMatcher.find()) {
            String async1 = methodMatcher.group(1);
            String async2 = methodMatcher.group(2);
            String name = methodMatcher.group(3);
            String rawParams = methodMatcher.group(4);
            String returnType = methodMatcher.group(5);

            if (name == null || IGNORED_METHOD_NAMES.contains(name)) continue;
            if (name.equals("get") || name.equals("set")) continue;

            boolean isAsync = (async1 != null && !async1.isBlank())
                    || (async2 != null && !async2.isBlank());

            List<MethodParameter> params = parseParameters(rawParams);

            int line = countLines(classBody, methodMatcher.start());

            result.add(new MethodInfo(
                    name,
                    params,
                    returnType != null ? returnType.trim() : null,
                    isAsync,
                    "public",
                    line,
                    sourceFile
            ));
        }
        return result;
    }

    // ─── Parser de parâmetros ─────────────────────────────────────────────────

    private List<MethodParameter> parseParameters(String rawParams) {
        if (rawParams == null || rawParams.isBlank()) return List.of();

        List<MethodParameter> result = new ArrayList<>();
        String[] parts = rawParams.split(",");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) continue;

            // Remove modifiers como "readonly", "private", "public"
            trimmed = trimmed.replaceAll("^(?:readonly|private|public|protected)\\s+", "");

            // Remove valores default: "param: string = 'value'" → "param: string"
            int equalIdx = trimmed.indexOf('=');
            if (equalIdx >= 0) trimmed = trimmed.substring(0, equalIdx).trim();

            int colon = trimmed.indexOf(':');
            if (colon >= 0) {
                String paramName = trimmed.substring(0, colon).trim().replaceAll("\\?$", "");
                String paramType = trimmed.substring(colon + 1).trim();
                if (!paramName.isBlank()) {
                    result.add(new MethodParameter(paramName, paramType));
                }
            } else if (!trimmed.isBlank()) {
                result.add(new MethodParameter(trimmed.replaceAll("\\?$", ""), null));
            }
        }
        return result;
    }

    // ─── Parser de imports ────────────────────────────────────────────────────

    private List<String> parseImports(String content) {
        List<String> imports = new ArrayList<>();
        Pattern importPattern = Pattern.compile("^import\\s+.+", Pattern.MULTILINE);
        Matcher m = importPattern.matcher(content);
        while (m.find()) {
            imports.add(m.group().trim());
        }
        return imports;
    }

    // ─── Parser de describe blocks ────────────────────────────────────────────

    private List<String> parseDescribeBlocks(String content) {
        List<String> result = new ArrayList<>();
        Matcher m = DESCRIBE_PATTERN.matcher(content);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    // ─── Parser de test/it blocks ─────────────────────────────────────────────

    private List<String> parseTestCases(String content) {
        List<String> result = new ArrayList<>();
        Pattern testPattern = Pattern.compile(
                "(?:test|it)\\s*\\(\\s*['\"`]([^'\"`]+)['\"`]", Pattern.MULTILINE
        );
        Matcher m = testPattern.matcher(content);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    // ─── Parser de Cypress commands ───────────────────────────────────────────

    private List<String> parseCypressCommands(String content) {
        List<String> result = new ArrayList<>();
        Pattern cmdPattern = Pattern.compile(
                "Cypress\\.Commands\\.add\\s*\\(\\s*['\"`](\\w+)['\"`]", Pattern.MULTILINE
        );
        Matcher m = cmdPattern.matcher(content);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    // ─── Utilitários ─────────────────────────────────────────────────────────

    private String extractBlock(String content, int startPos) {
        int depth = 0;
        int start = content.indexOf('{', startPos);
        if (start < 0) return "";

        StringBuilder block = new StringBuilder();
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    block.append(c);
                    break;
                }
            }
            block.append(c);
        }
        return block.toString();
    }

    private boolean isTestFile(String relativePath) {
        String lower = relativePath.toLowerCase();
        return SPEC_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private boolean isInDirectory(String relativePath, List<String> dirs) {
        String lower = relativePath.replace("\\", "/").toLowerCase();
        return dirs.stream().anyMatch(dir ->
                lower.startsWith(dir + "/") || lower.contains("/" + dir + "/")
        );
    }

    private int countLines(String text, int pos) {
        int count = 1;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }

    private ProjectAnalysisResult emptyResult() {
        return ProjectAnalysisResult.builder()
                .classes(List.of())
                .pageObjects(List.of())
                .testFiles(List.of())
                .fixtureFiles(List.of())
                .helperFiles(List.of())
                .customCommands(List.of())
                .describeBlocks(List.of())
                .testCases(List.of())
                .conventions(List.of())
                .gaps(List.of())
                .warnings(List.of())
                .metadata(Map.of())
                .analyzedAt(LocalDateTime.now())
                .build();
    }
}
