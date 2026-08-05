package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ResourceMetadataParser implements SourceMetadataParser {

    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("\"([A-Za-z0-9_\\-]+)\"\\s*:");
    private static final Pattern YAML_KEY_PATTERN = Pattern.compile("(?m)^([A-Za-z0-9_\\-]+)\\s*:");
    private static final Pattern PROPERTIES_KEY_PATTERN = Pattern.compile("(?m)^([A-Za-z0-9_.\\-]+)\\s*=");

    @Override
    public boolean supports(KnowledgeScanResult.KnowledgeFile file) {
        return ".json".equals(file.extension())
                || ".yaml".equals(file.extension())
                || ".yml".equals(file.extension())
                || ".properties".equals(file.extension());
    }

    @Override
    public SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file) {
        String content = file.content();
        List<String> keys = new ArrayList<>();
        if (".json".equals(file.extension())) {
            keys.addAll(matches(JSON_KEY_PATTERN, content));
        } else if (".yaml".equals(file.extension()) || ".yml".equals(file.extension())) {
            keys.addAll(matches(YAML_KEY_PATTERN, content));
        } else {
            keys.addAll(matches(PROPERTIES_KEY_PATTERN, content));
        }
        List<String> tags = new ArrayList<>();
        if (file.relativePath().contains("/fixtures/")) {
            tags.add("FIXTURE");
        }
        if (file.relativePath().contains("/test-data/") || file.relativePath().contains("/testdata/")) {
            tags.add("TEST_DATA");
        }
        if (file.relativePath().contains("/config/") || file.relativePath().endsWith(".properties")) {
            tags.add("CONFIGURATION");
        }
        return new SourceMetadata(
                file.relativePath(),
                file.name().replaceFirst("\\.[^.]+$", ""),
                SourceLanguage.UNKNOWN,
                null,
                List.of(),
                keys,
                List.of(),
                List.of(),
                List.of(),
                tags,
                false,
                List.of()
        );
    }

    private List<String> matches(Pattern pattern, String content) {
        var matcher = pattern.matcher(content);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values.stream().distinct().toList();
    }
}
