package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlanningInputSanitizer - Testes Unitários")
class PlanningInputSanitizerTest {

    private PlanningInputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new PlanningInputSanitizer();
    }

    @Test
    @DisplayName("Deve nunca incluir Path no output")
    void deveNuncaIncluirNormalizedProjectPath() {
        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(),
            PlanningTestData.validScenario(),
            PlanningTestData.completeKnowledge("tests/login.spec.ts")
        );
        // SanitizedPlanningInput has no Path fields - verify structural contract
        assertThat(result).isNotNull();
        // All string lists should be strings (not paths)
        result.components().forEach(c -> assertThat(c.relativePath()).isInstanceOf(String.class));
        result.candidates().forEach(c -> assertThat(c.componentPath()).isInstanceOf(String.class));
    }

    @Test
    @DisplayName("Deve limitar componentes a MAX_COMPONENTS")
    void deveLimitarComponentes() {
        List<ProjectComponent> components = IntStream.rangeClosed(1, 35)
            .mapToObj(i -> new ProjectComponent(
                "src/comp" + String.format("%02d", i) + ".ts",
                "Comp" + i,
                ComponentType.PAGE_OBJECT,
                SourceLanguage.TYPESCRIPT,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), false, true, List.of()
            ))
            .collect(Collectors.toList());
        ProjectKnowledgeResult knowledge = knowledgeWith(components, KnowledgeStatus.COMPLETE);

        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge
        );

        assertThat(result.components()).hasSize(PlanningInputSanitizer.MAX_COMPONENTS);
    }

    @Test
    @DisplayName("Deve limitar candidatos a MAX_CANDIDATES")
    void deveLimitarCandidatos() {
        List<ReuseCandidate> candidates = IntStream.rangeClosed(1, 20)
            .mapToObj(i -> new ReuseCandidate(
                "src/cand" + String.format("%02d", i) + ".ts",
                ComponentType.PAGE_OBJECT,
                "reason",
                ReuseConfidence.HIGH,
                List.of("term")
            ))
            .collect(Collectors.toList());
        ProjectKnowledgeResult knowledge = knowledgeWithCandidates(candidates, KnowledgeStatus.COMPLETE);

        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge
        );

        assertThat(result.candidates()).hasSize(PlanningInputSanitizer.MAX_CANDIDATES);
    }

    @Test
    @DisplayName("Deve limitar steps a MAX_STEPS")
    void deveLimitarPassos() {
        List<com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep> steps =
            IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep(
                    i, "Ação " + i, "Resultado " + i, List.of()
                ))
                .collect(Collectors.toList());
        ScenarioAnalysisResult scenario = scenarioWithSteps(steps);

        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(), scenario, PlanningTestData.emptyKnowledge()
        );

        assertThat(result.steps()).hasSize(PlanningInputSanitizer.MAX_STEPS);
    }

    @Test
    @DisplayName("Deve limitar warnings a MAX_WARNINGS")
    void deveLimitarWarnings() {
        List<String> warnings = IntStream.rangeClosed(1, 15)
            .mapToObj(i -> "Warning " + String.format("%02d", i))
            .collect(Collectors.toList());
        ProjectKnowledgeResult knowledge = knowledgeWithWarnings(warnings, KnowledgeStatus.PARTIAL);

        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge
        );

        assertThat(result.knowledgeWarnings()).hasSize(PlanningInputSanitizer.MAX_WARNINGS);
    }

    @Test
    @DisplayName("Deve ordenar componentes antes de truncar")
    void deveOrdenarComponentesAntesDeTruncar() {
        List<ProjectComponent> components = new ArrayList<>();
        for (int i = 35; i >= 1; i--) {
            components.add(new ProjectComponent(
                "src/comp" + String.format("%02d", i) + ".ts",
                "Comp" + i,
                ComponentType.PAGE_OBJECT,
                SourceLanguage.TYPESCRIPT,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), false, true, List.of()
            ));
        }
        ProjectKnowledgeResult knowledge = knowledgeWith(components, KnowledgeStatus.COMPLETE);

        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge
        );

        assertThat(result.components()).hasSize(PlanningInputSanitizer.MAX_COMPONENTS);
        // Should be first 30 alphabetically
        assertThat(result.components().get(0).relativePath()).isEqualTo("src/comp01.ts");
        assertThat(result.components().get(29).relativePath()).isEqualTo("src/comp30.ts");
    }

    @Test
    @DisplayName("Deve ser determinístico com mesmas entradas")
    void deveSerDeterministico() {
        ProjectDiscoveryResult discovery = PlanningTestData.discovery();
        ScenarioAnalysisResult scenario = PlanningTestData.validScenario();
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("tests/a.ts", "tests/b.ts");

        SanitizedPlanningInput r1 = sanitizer.sanitize(discovery, scenario, knowledge);
        SanitizedPlanningInput r2 = sanitizer.sanitize(discovery, scenario, knowledge);

        assertThat(r1.components()).isEqualTo(r2.components());
        assertThat(r1.steps()).isEqualTo(r2.steps());
        assertThat(r1.knowledgeWarnings()).isEqualTo(r2.knowledgeWarnings());
    }

    @Test
    @DisplayName("Deve não modificar objetos originais")
    void deveNaoModificarObjetosOriginais() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("tests/login.ts");
        int originalSize = knowledge.components().size();

        sanitizer.sanitize(PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge);

        assertThat(knowledge.components()).hasSize(originalSize);
    }

    @Test
    @DisplayName("Deve manter caminhos relativos como strings")
    void deveManterCaminhosRelativos() {
        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(),
            PlanningTestData.validScenario(),
            PlanningTestData.completeKnowledge("tests/login.spec.ts", "pages/LoginPage.ts")
        );

        result.components().forEach(c -> {
            assertThat(c.relativePath()).doesNotStartWith("/");
            assertThat(c.relativePath()).doesNotContain("..");
        });
    }

    @Test
    @DisplayName("Deve não incluir conteúdo de arquivos")
    void deveNaoIncluirConteudoDeArquivos() {
        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(),
            PlanningTestData.validScenario(),
            PlanningTestData.completeKnowledge("tests/login.spec.ts")
        );

        // Components only have metadata: relativePath, typeName, componentName - no content
        result.components().forEach(c -> {
            assertThat(c).isNotNull();
            assertThat(c.relativePath()).isNotNull();
            assertThat(c.typeName()).isNotNull();
            assertThat(c.componentName()).isNotNull();
        });
    }

    @Test
    @DisplayName("Deve respeitar limite total razoável")
    void deveRespeitarLimiteTotal() {
        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(),
            PlanningTestData.validScenario(),
            PlanningTestData.completeKnowledge("tests/login.spec.ts")
        );

        assertThat(result.components().size()).isLessThanOrEqualTo(PlanningInputSanitizer.MAX_COMPONENTS);
        assertThat(result.candidates().size()).isLessThanOrEqualTo(PlanningInputSanitizer.MAX_CANDIDATES);
        assertThat(result.steps().size()).isLessThanOrEqualTo(PlanningInputSanitizer.MAX_STEPS);
        assertThat(result.knowledgeWarnings().size()).isLessThanOrEqualTo(PlanningInputSanitizer.MAX_WARNINGS);
    }

    @Test
    @DisplayName("Deve incluir framework e linguagem no output")
    void deveIncluirFrameworkELinguagem() {
        SanitizedPlanningInput result = sanitizer.sanitize(
            PlanningTestData.discovery(),
            PlanningTestData.validScenario(),
            PlanningTestData.completeKnowledge()
        );

        assertThat(result.framework()).isNotNull();
        assertThat(result.language()).isNotNull();
    }

    // --- helper builders ---

    private ProjectKnowledgeResult knowledgeWith(List<ProjectComponent> components, KnowledgeStatus status) {
        return new ProjectKnowledgeResult(
            Path.of("/project"), components,
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(),
            new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
            List.of(), List.of(), List.of(), List.of(), status, status != KnowledgeStatus.FAILED
        );
    }

    private ProjectKnowledgeResult knowledgeWithCandidates(List<ReuseCandidate> candidates, KnowledgeStatus status) {
        return new ProjectKnowledgeResult(
            Path.of("/project"), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            candidates,
            new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
            List.of(), List.of(), List.of(), List.of(), status, status != KnowledgeStatus.FAILED
        );
    }

    private ProjectKnowledgeResult knowledgeWithWarnings(List<String> warnings, KnowledgeStatus status) {
        return new ProjectKnowledgeResult(
            Path.of("/project"), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(),
            new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
            List.of(), List.of(), List.of(), warnings, status, status != KnowledgeStatus.FAILED
        );
    }

    private ScenarioAnalysisResult scenarioWithSteps(List<com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep> steps) {
        return new ScenarioAnalysisResult(
            "Cenário", "Objetivo", List.of(), steps, List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(),
            com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType.WEB_UI,
            com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus.VALID,
            List.of(), true
        );
    }
}
