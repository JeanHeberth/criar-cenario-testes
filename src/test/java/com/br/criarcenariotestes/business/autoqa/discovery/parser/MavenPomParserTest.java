package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MavenPomParser - Testes Unitários")
class MavenPomParserTest {

    private final MavenPomParser parser = new MavenPomParser();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve extrair dependências do pom.xml")
    void deveExtrairDependenciasDoPom() throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <dependencies>
                    <dependency>
                      <groupId>org.seleniumhq.selenium</groupId>
                      <artifactId>selenium-java</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """);

        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        parser.parse(pom, "pom.xml", builder);

        assertThat(builder.mavenDependencies()).contains("org.seleniumhq.selenium:selenium-java");
    }

    @Test
    @DisplayName("Deve bloquear DOCTYPE e registrar warning")
    void deveBloquearDoctypeERegistrarWarning() throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, """
                <!DOCTYPE project [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <project>
                  <dependencies>
                    <dependency>
                      <groupId>&xxe;</groupId>
                      <artifactId>artifact</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """);

        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        parser.parse(pom, "pom.xml", builder);

        assertThat(builder.mavenDependencies()).isEmpty();
        assertThat(builder.warnings()).singleElement().asString().contains("pom.xml inválido");
    }
}
