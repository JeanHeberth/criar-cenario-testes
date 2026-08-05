package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectStructureResolver - Testes Unitários")
class ProjectStructureResolverTest {

    private final ProjectStructureResolver resolver = new ProjectStructureResolver();

    @Test
    @DisplayName("Deve resolver diretórios de testes, fontes e ignorados")
    void deveResolverDiretorios() {
        var structure = resolver.resolve(
                new KnowledgeScanResult(Path.of("/tmp/project"), List.of(), List.of("node_modules/a.ts", "build/b.java"), List.of(), false),
                List.of(
                        KnowledgeTestData.component("src/test/java/LoginTest.java", "LoginTest", ComponentType.TEST, SourceLanguage.JAVA),
                        KnowledgeTestData.component("src/main/java/LoginPage.java", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.JAVA)
                )
        );

        assertThat(structure.testDirectories()).contains("src/test/java");
        assertThat(structure.sourceDirectories()).contains("src/main/java");
        assertThat(structure.ignoredDirectories()).contains("node_modules", "build");
    }
}
