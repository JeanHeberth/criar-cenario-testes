package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PackageJsonParser - Testes Unitários")
class PackageJsonParserTest {

    private final PackageJsonParser parser = new PackageJsonParser(new ObjectMapper());

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve ler dependencies, devDependencies e peerDependencies")
    void deveLerDependencias() throws Exception {
        Path packageJson = tempDir.resolve("package.json");
        Files.writeString(packageJson, """
                {
                  "dependencies": {"cypress":"13.0.0"},
                  "devDependencies": {"@playwright/test":"1.45.0"},
                  "peerDependencies": {"selenium-webdriver":"4.0.0"}
                }
                """);

        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        parser.parse(packageJson, "package.json", builder);

        assertThat(builder.nodeDependencies())
                .contains("cypress", "@playwright/test", "selenium-webdriver");
    }

    @Test
    @DisplayName("Deve ignorar scripts e texto genérico")
    void deveIgnorarScriptsETextoGenerico() throws Exception {
        Path packageJson = tempDir.resolve("package.json");
        Files.writeString(packageJson, """
                {
                  "scripts": {"test":"cypress run"},
                  "description":"contains @playwright/test text only"
                }
                """);

        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        parser.parse(packageJson, "package.json", builder);

        assertThat(builder.nodeDependencies()).isEmpty();
    }

    @Test
    @DisplayName("Deve registrar warning para JSON inválido")
    void deveRegistrarWarningParaJsonInvalido() throws Exception {
        Path packageJson = tempDir.resolve("package.json");
        Files.writeString(packageJson, "{ invalid }");

        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        parser.parse(packageJson, "package.json", builder);

        assertThat(builder.warnings()).singleElement().asString().contains("package.json inválido");
    }
}
