package com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SanitizedProjectKnowledgeSnapshot - Testes Unitários")
class SanitizedProjectKnowledgeSnapshotTest {

    @Test
    @DisplayName("from() nunca deve capturar o normalizedProjectPath")
    void fromNuncaCapturaProjectPath() {
        ProjectKnowledgeResult original = knowledgeComPath("/projeto/sensivel/secreto");

        SanitizedProjectKnowledgeSnapshot snapshot = SanitizedProjectKnowledgeSnapshot.from(original);

        assertThat(snapshot.toString()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("toResult() deve reconstruir um ProjectKnowledgeResult equivalente, usando o path informado externamente")
    void toResultDeveReconstruirComPathExterno() {
        ProjectKnowledgeResult original = knowledgeComPath("/projeto/original");
        SanitizedProjectKnowledgeSnapshot snapshot = SanitizedProjectKnowledgeSnapshot.from(original);

        ProjectKnowledgeResult reconstructed = snapshot.toResult(Path.of("/projeto/reidratado"));

        assertThat(reconstructed.normalizedProjectPath()).isEqualTo(Path.of("/projeto/reidratado"));
        assertThat(reconstructed.components()).isEqualTo(original.components());
        assertThat(reconstructed.reuseCandidates()).isEqualTo(original.reuseCandidates());
        assertThat(reconstructed.namingConvention()).isEqualTo(original.namingConvention());
        assertThat(reconstructed.testDirectories()).isEqualTo(original.testDirectories());
        assertThat(reconstructed.sourceDirectories()).isEqualTo(original.sourceDirectories());
        assertThat(reconstructed.status()).isEqualTo(original.status());
        assertThat(reconstructed.valid()).isEqualTo(original.valid());
    }

    private ProjectKnowledgeResult knowledgeComPath(String path) {
        ProjectComponent component = new ProjectComponent("tests/support/loginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT,
                SourceLanguage.TYPESCRIPT, null, List.of(), List.of("login"), List.of(), List.of(), List.of(), false, true, List.of());
        ReuseCandidate candidate = new ReuseCandidate("tests/support/loginPage.ts", ComponentType.PAGE_OBJECT, "compatível",
                ReuseConfidence.HIGH, List.of("login"));
        NamingConvention naming = new NamingConvention("*.spec.ts", "*Page.ts", "PascalCase", "camelCase", "tests", List.of(), ReuseConfidence.HIGH);
        return new ProjectKnowledgeResult(Path.of(path), List.of(component), List.of(), List.of(component), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(candidate), naming, List.of("tests"), List.of("src"),
                List.of("node_modules"), List.of(), KnowledgeStatus.COMPLETE, true);
    }
}
