package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaMetadataParser implements SourceMetadataParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*import\\s+([\\w.*]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("(?m)^\\s*(?:public\\s+)?(?:abstract\\s+)?(class|interface|record|enum)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?m)^\\s*(?:public|protected)\\s+(?:static\\s+)?(?:final\\s+)?(?:<[^>]+>\\s+)?[\\w.$<>\\[\\], ?]+\\s+([A-Za-z_$][\\w$]*)\\s*\\(");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(?m)^\\s*@([A-Za-z_$][\\w$.]*)");
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\bextends\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\bimplements\\s+([A-Za-z_$][\\w$]*(?:\\s*,\\s*[A-Za-z_$][\\w$]*)*)");

    @Override
    public boolean supports(KnowledgeScanResult.KnowledgeFile file) {
        return ".java".equals(file.extension());
    }

    @Override
    public SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file) {
        String content = file.content();
        List<String> packages = matches(PACKAGE_PATTERN, content);
        List<String> imports = matches(IMPORT_PATTERN, content);
        List<String> types = new ArrayList<>();
        List<String> declaredClasses = new ArrayList<>();
        Matcher typeMatcher = TYPE_PATTERN.matcher(content);
        while (typeMatcher.find()) {
            declaredClasses.add(typeMatcher.group(2));
            types.add(typeMatcher.group(1));
        }
        List<String> methods = matches(METHOD_PATTERN, content);
        List<String> annotations = matches(ANNOTATION_PATTERN, content);
        List<String> hierarchy = new ArrayList<>();
        hierarchy.addAll(matches(EXTENDS_PATTERN, content));
        for (String value : matches(IMPLEMENTS_PATTERN, content)) {
            for (String item : value.split("\\s*,\\s*")) {
                hierarchy.add(item);
            }
        }

        List<String> tags = new ArrayList<>();
        if (content.contains("@Test") || file.name().endsWith("Test.java")) {
            tags.add("TEST");
        }
        if (content.contains("@BeforeEach") || content.contains("@AfterEach")) {
            tags.add("HOOK");
        }
        if (content.contains("RestAssured")) {
            tags.add("API_CLIENT_EVIDENCE");
        }
        if (content.contains("SelenideElement") || content.contains("WebElement") || file.name().endsWith("Page.java")) {
            tags.add("PAGE_OBJECT_EVIDENCE");
        }

        boolean testComponent = tags.contains("TEST");
        String packageName = packages.isEmpty() ? null : packages.getFirst();
        String name = declaredClasses.isEmpty() ? file.name().replaceFirst("\\.[^.]+$", "") : declaredClasses.getFirst();
        return new SourceMetadata(
                file.relativePath(),
                name,
                SourceLanguage.JAVA,
                packageName,
                declaredClasses,
                methods,
                imports,
                annotations,
                hierarchy,
                tags,
                testComponent,
                List.of()
        );
    }

    private List<String> matches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(matcher.group(1).trim());
        }
        return values.stream().distinct().toList();
    }
}
