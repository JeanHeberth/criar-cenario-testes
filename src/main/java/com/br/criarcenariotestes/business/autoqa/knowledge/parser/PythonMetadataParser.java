package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PythonMetadataParser implements SourceMetadataParser {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*(?:from\\s+([\\w.]+)\\s+)?import\\s+([\\w.*, ]+)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^\\s*class\\s+([A-Za-z_][\\w]*)");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*def\\s+([A-Za-z_][\\w]*)\\s*\\(");
    private static final Pattern DECORATOR_PATTERN = Pattern.compile("(?m)^\\s*@([A-Za-z_][\\w.]*)");

    @Override
    public boolean supports(KnowledgeScanResult.KnowledgeFile file) {
        return ".py".equals(file.extension());
    }

    @Override
    public SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file) {
        String content = file.content();
        List<String> imports = matches(IMPORT_PATTERN, content);
        List<String> classes = matches(CLASS_PATTERN, content);
        List<String> functions = matches(FUNCTION_PATTERN, content);
        List<String> decorators = matches(DECORATOR_PATTERN, content);

        List<String> tags = new ArrayList<>();
        if (file.name().startsWith("test_") || content.contains("pytest")) {
            tags.add("TEST");
        }
        if (content.contains("@pytest.fixture")) {
            tags.add("FIXTURE");
        }
        if (!decorators.isEmpty()) {
            tags.add("DECORATOR");
        }
        if (file.relativePath().contains("/helpers/") || file.relativePath().contains("/utils/")) {
            tags.add("HELPER_EVIDENCE");
        }

        boolean testComponent = file.name().startsWith("test_") || tags.contains("TEST");
        String name = classes.isEmpty() ? firstNonEmpty(functions, file.name().replaceFirst("\\.[^.]+$", "")) : classes.getFirst();
        return new SourceMetadata(
                file.relativePath(),
                name,
                SourceLanguage.PYTHON,
                null,
                classes,
                functions,
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
            if (matcher.groupCount() >= 2) {
                if (matcher.group(1) != null && !matcher.group(1).isBlank()) {
                    values.add(matcher.group(1));
                }
                if (matcher.group(2) != null && !matcher.group(2).isBlank()) {
                    values.add(matcher.group(2).trim());
                }
            } else {
                values.add(matcher.group(1));
            }
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
    }

    private String firstNonEmpty(List<String> values, String fallback) {
        return values.stream().findFirst().orElse(fallback);
    }
}
