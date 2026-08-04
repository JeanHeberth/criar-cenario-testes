package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

@Component
public class PackageJsonParser {

    private final ObjectMapper objectMapper;

    public PackageJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public void parse(Path file, String relativePath, ParsedProjectFiles.Builder builder) {
        builder.packageJson(true);
        builder.packageJsonPath(relativePath);
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            collect(root.path("dependencies"), builder);
            collect(root.path("devDependencies"), builder);
            collect(root.path("peerDependencies"), builder);
        } catch (Exception exception) {
            builder.warnings().add("package.json inválido: " + exception.getMessage());
        }
    }

    private void collect(JsonNode node, ParsedProjectFiles.Builder builder) {
        if (!node.isObject()) {
            return;
        }
        node.fieldNames().forEachRemaining(dep ->
                builder.nodeDependencies().add(dep.toLowerCase(Locale.ROOT)));
    }
}
