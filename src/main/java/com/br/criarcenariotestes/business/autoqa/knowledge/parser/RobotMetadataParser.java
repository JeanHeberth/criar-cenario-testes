package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RobotMetadataParser implements SourceMetadataParser {

    private static final Pattern TEST_CASES_PATTERN = Pattern.compile("(?ms)^\\*\\*\\*\\s*Test Cases\\s*\\*\\*\\*(.*?)(?:^\\*\\*\\*|\\z)");
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile("(?ms)^\\*\\*\\*\\s*Keywords\\s*\\*\\*\\*(.*?)(?:^\\*\\*\\*|\\z)");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(?mi)^Resource\\s{2,}(.+)$");
    private static final Pattern LIBRARY_PATTERN = Pattern.compile("(?mi)^Library\\s{2,}(.+)$");
    private static final Pattern VARIABLES_PATTERN = Pattern.compile("(?mi)^Variables\\s{2,}(.+)$");
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[Tags\\]\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public boolean supports(KnowledgeScanResult.KnowledgeFile file) {
        return ".robot".equals(file.extension()) || ".resource".equals(file.extension());
    }

    @Override
    public SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file) {
        String content = file.content();
        List<String> testCases = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        if (TEST_CASES_PATTERN.matcher(content).find()) {
            testCases.add("TEST_CASES");
        }
        if (KEYWORDS_PATTERN.matcher(content).find()) {
            keywords.add("KEYWORDS");
        }
        List<String> imports = new ArrayList<>();
        imports.addAll(matches(RESOURCE_PATTERN, content, "RESOURCE"));
        imports.addAll(matches(LIBRARY_PATTERN, content, "LIBRARY"));
        imports.addAll(matches(VARIABLES_PATTERN, content, "VARIABLES"));
        List<String> tags = new ArrayList<>();
        for (String value : matches(TAG_PATTERN, content, null)) {
            for (String tag : value.split("\\s+")) {
                if (!tag.isBlank()) {
                    tags.add(tag.trim());
                }
            }
        }
        if (file.extension().equals(".resource")) {
            tags.add("RESOURCE");
        }

        boolean testComponent = file.extension().equals(".robot") && TEST_CASES_PATTERN.matcher(content).find();
        String name = file.name().replaceFirst("\\.[^.]+$", "");
        return new SourceMetadata(
                file.relativePath(),
                name,
                SourceLanguage.ROBOT,
                null,
                List.of(),
                keywords.isEmpty() ? testCases : keywords,
                imports,
                List.of(),
                List.of(),
                tags,
                testComponent,
                List.of()
        );
    }

    private List<String> matches(Pattern pattern, String content, String fallback) {
        var matcher = pattern.matcher(content);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value != null) {
                values.add(value.trim());
            } else if (fallback != null) {
                values.add(fallback);
            }
        }
        return values.stream().distinct().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
