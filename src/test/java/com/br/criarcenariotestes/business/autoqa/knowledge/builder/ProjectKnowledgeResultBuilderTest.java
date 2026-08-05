package com.br.criarcenariotestes.business.autoqa.knowledge.builder;

import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ProjectStructureResolver;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectKnowledgeResultBuilder - Testes Unitários")
class ProjectKnowledgeResultBuilderTest {

    private final ProjectKnowledgeResultBuilder builder = new ProjectKnowledgeResultBuilder();

    @Test
    @DisplayName("Deve deduplicar componentes")
    void deveDeduplicarComponentes() {
        ProjectComponent component = KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT);
        ProjectKnowledgeResult result = builder.build(
                Path.of("/tmp/project"),
                List.of(component, component),
                List.of(new ReuseCandidate("src/pages/LoginPage.ts", ComponentType.PAGE_OBJECT, "match", ReuseConfidence.HIGH, List.of("login"))),
                new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                new ProjectStructureResolver.ProjectStructure(List.of("tests"), List.of("src"), List.of()),
                List.of(),
                KnowledgeStatus.COMPLETE,
                true
        );

        assertThat(result.components()).hasSize(1);
    }

    @Test
    @DisplayName("Deve manter subsets coerentes")
    void deveManterSubsetsCoerentes() {
        ProjectComponent component = KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT);
        ProjectKnowledgeResult result = builder.build(
                Path.of("/tmp/project"),
                List.of(component),
                List.of(),
                new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                new ProjectStructureResolver.ProjectStructure(List.of("tests"), List.of("src"), List.of()),
                List.of(),
                KnowledgeStatus.COMPLETE,
                true
        );

        assertThat(result.pageObjects()).hasSize(1);
        assertThat(result.tests()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar listas imutáveis")
    void deveRetornarListasImutaveis() {
        ProjectComponent component = KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT);
        ProjectKnowledgeResult result = builder.build(
                Path.of("/tmp/project"),
                List.of(component),
                List.of(),
                new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                new ProjectStructureResolver.ProjectStructure(List.of("tests"), List.of("src"), List.of()),
                List.of(),
                KnowledgeStatus.COMPLETE,
                true
        );

        assertThatThrownBy(() -> result.components().add(component))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
