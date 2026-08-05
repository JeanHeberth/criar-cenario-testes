package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TypeScriptMetadataParser implements SourceMetadataParser {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+(?:.+?\\s+from\\s+)?['\"]([^'\"]+)['\"];?");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^\\s*(?:export\\s+)?class\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern EXPORTED_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*export\\s+(?:async\\s+)?function\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern EXPORTED_CONST_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*export\\s+const\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s*)?(?:\\([^\\)]*\\)|[A-Za-z_$][\\w$]*)\\s*=>");
    private static final Pattern DECORATOR_PATTERN = Pattern.compile("(?m)^\\s*@([A-Za-z_$][\\w$.]*)");

    @Override
    public boolean supports(KnowledgeScanResult.KnowledgeFile file) {
        String extension = file.extension();
        return ".ts".equals(extension) || ".tsx".equals(extension) || ".js".equals(extension) || ".jsx".equals(extension);
    }

    @Override
    public SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file) {
        String content = file.content();
        List<String> imports = matches(IMPORT_PATTERN, content);
        List<String> classes = matches(CLASS_PATTERN, content);
        List<String> functions = new ArrayList<>(matches(EXPORTED_FUNCTION_PATTERN, content));
        functions.addAll(matches(EXPORTED_CONST_FUNCTION_PATTERN, content));
        List<String> methods = extractMethods(content);
        List<String> declaredMethods = new ArrayList<>(functions);
        declaredMethods.addAll(methods);
        List<String> decorators = matches(DECORATOR_PATTERN, content);

        List<String> tags = new ArrayList<>();
        if (containsAny(content, "test(", "describe(", "it(")) {
            tags.add("TEST");
        }
        if (content.contains("test.extend")) {
            tags.add("FIXTURE");
        }
        if (!decorators.isEmpty()) {
            tags.add("DECORATOR");
        }
        if (file.name().endsWith("Page.ts") || file.name().endsWith("Page.tsx") || file.relativePath().contains("/pages/") || file.relativePath().contains("/pageObjects/")) {
            tags.add("PAGE_OBJECT_EVIDENCE");
        }
        if (file.relativePath().contains("/helpers/") || file.relativePath().contains("/utils/")) {
            tags.add("HELPER_EVIDENCE");
        }
        if (file.relativePath().contains("/api/") || file.relativePath().contains("/client/")) {
            tags.add("API_CLIENT_EVIDENCE");
        }

        boolean testComponent = file.name().matches("(?i).*(spec|test|cy)\\.(ts|tsx|js|jsx)$") || tags.contains("TEST");
        String name = classes.isEmpty() ? firstNonEmpty(functions, file.name().replaceFirst("\\.[^.]+$", "")) : classes.getFirst();
        return new SourceMetadata(
                file.relativePath(),
                name,
                file.extension().endsWith("js") || file.extension().endsWith("jsx") ? SourceLanguage.JAVASCRIPT : SourceLanguage.TYPESCRIPT,
                null,
                classes,
                declaredMethods,
                imports,
                decorators,
                List.of(),
                tags,
                testComponent,
                List.of()
        );
    }

    private List<String> matches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values.stream().distinct().toList();
    }

    private boolean containsAny(String content, String... terms) {
        for (String term : terms) {
            if (content.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractMethods(String content) {
        List<String> methods = new ArrayList<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.contains("(") || !trimmed.contains(")")) {
                continue;
            }
            if (trimmed.startsWith("import ")
                    || trimmed.startsWith("export class ")
                    || trimmed.startsWith("class ")
                    || trimmed.startsWith("return ")
                    || trimmed.startsWith("if ")
                    || trimmed.startsWith("for ")
                    || trimmed.startsWith("while ")
                    || trimmed.startsWith("switch ")
                    || trimmed.startsWith("catch ")) {
                continue;
            }
            String candidate = trimmed.substring(0, trimmed.indexOf('(')).trim();
            while (candidate.startsWith("public ")
                    || candidate.startsWith("private ")
                    || candidate.startsWith("protected ")
                    || candidate.startsWith("readonly ")
                    || candidate.startsWith("static ")
                    || candidate.startsWith("async ")
                    || candidate.startsWith("override ")) {
                candidate = candidate.substring(candidate.indexOf(' ') + 1).trim();
            }
            if (candidate.equals("constructor")) {
                continue;
            }
            String methodName = candidate.substring(candidate.lastIndexOf(' ') + 1).trim();
            if (methodName.matches("[A-Za-z_$][\\w$]*")) {
                methods.add(methodName);
            }
        }
        return methods.stream().distinct().toList();
    }

    private String firstNonEmpty(List<String> values, String fallback) {
        return values.stream().findFirst().orElse(fallback);
    }
}
