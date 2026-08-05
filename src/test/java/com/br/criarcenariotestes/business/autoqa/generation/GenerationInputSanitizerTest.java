package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerationInputSanitizer - Testes Unitários")
class GenerationInputSanitizerTest {

    private GenerationInputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new GenerationInputSanitizer();
    }

    @Test
    @DisplayName("Deve nunca incluir projectPath no output")
    void deveNuncaIncluirProjectPath() {
        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        result.reusableComponents().forEach(c -> assertThat(c.relativePath()).isInstanceOf(String.class));
        assertThat(result.toString()).doesNotContain("/project");
    }

    @Test
    @DisplayName("Deve nunca incluir conteúdo completo de arquivos existentes")
    void deveNuncaIncluirConteudoCompleto() {
        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        result.reusableComponents().forEach(c -> {
            assertThat(c.declaredMethods()).allSatisfy(m -> assertThat(m).doesNotContain("{"));
        });
    }

    @Test
    @DisplayName("Deve manter assinaturas permitidas (métodos e imports)")
    void deveManterAssinaturasPermitidas() {
        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.reusableComponents()).isNotEmpty();
        assertThat(result.reusableComponents().get(0).declaredMethods()).contains("login", "open");
    }

    @Test
    @DisplayName("Deve limitar arquivos planejados a MAX_FILE_ACTIONS")
    void deveLimitarArquivos() {
        var actions = IntStream.rangeClosed(1, 40)
                .mapToObj(i -> GenerationTestData.createAction("tests/test" + String.format("%02d", i) + ".spec.ts", PlanComponentType.TEST))
                .collect(Collectors.toList());
        TechnicalPlanResult plan = GenerationTestData.readyPlan(actions.toArray(new com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction[0]));

        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), GenerationTestData.emptyKnowledge(), plan
        );

        assertThat(result.fileActions()).hasSize(GenerationInputSanitizer.MAX_FILE_ACTIONS);
    }

    @Test
    @DisplayName("Deve limitar componentes reutilizáveis a MAX_COMPONENTS")
    void deveLimitarComponentes() {
        String[] paths = IntStream.rangeClosed(1, 30)
                .mapToObj(i -> "pages/Page" + String.format("%02d", i) + ".ts")
                .toArray(String[]::new);
        ProjectKnowledgeResult knowledge = GenerationTestData.completeKnowledge(paths);

        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), knowledge,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.reusableComponents()).hasSize(GenerationInputSanitizer.MAX_COMPONENTS);
    }

    @Test
    @DisplayName("Deve limitar métodos por componente a MAX_METHODS_PER_COMPONENT")
    void deveLimitarMetodos() {
        List<String> methods = IntStream.rangeClosed(1, 20).mapToObj(i -> "method" + i).toList();
        ProjectComponent component = new ProjectComponent("pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT,
                SourceLanguage.TYPESCRIPT, null, List.of(), methods, List.of(), List.of(), List.of(), false, true, List.of());
        ProjectKnowledgeResult knowledge = knowledgeWith(List.of(component));

        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), knowledge,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.reusableComponents().get(0).declaredMethods()).hasSize(GenerationInputSanitizer.MAX_METHODS_PER_COMPONENT);
    }

    @Test
    @DisplayName("Deve limitar imports por componente a MAX_IMPORTS_PER_COMPONENT")
    void deveLimitarImports() {
        List<String> imports = IntStream.rangeClosed(1, 20).mapToObj(i -> "import x" + i).toList();
        ProjectComponent component = new ProjectComponent("pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT,
                SourceLanguage.TYPESCRIPT, null, List.of(), List.of(), imports, List.of(), List.of(), false, true, List.of());
        ProjectKnowledgeResult knowledge = knowledgeWith(List.of(component));

        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), knowledge,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.reusableComponents().get(0).imports()).hasSize(GenerationInputSanitizer.MAX_IMPORTS_PER_COMPONENT);
    }

    @Test
    @DisplayName("Deve limitar steps a MAX_STEPS")
    void deveLimitarSteps() {
        List<ScenarioStep> steps = IntStream.rangeClosed(1, 30)
                .mapToObj(i -> new ScenarioStep(i, "Ação " + i, "Resultado " + i, List.of()))
                .toList();
        ScenarioAnalysisResult scenario = new ScenarioAnalysisResult(
                "Cenário", "Objetivo", List.of(), steps, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType.WEB_UI,
                com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus.VALID, List.of(), true
        );

        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), scenario, GenerationTestData.emptyKnowledge(),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.steps()).hasSize(GenerationInputSanitizer.MAX_STEPS);
    }

    @Test
    @DisplayName("Deve ser determinístico com as mesmas entradas")
    void deveSerDeterministico() {
        var discovery = GenerationTestData.playwrightDiscovery();
        var scenario = GenerationTestData.validScenario();
        var knowledge = GenerationTestData.completeKnowledge("pages/LoginPage.ts");
        var plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));

        SanitizedGenerationInput r1 = sanitizer.sanitize(discovery, scenario, knowledge, plan);
        SanitizedGenerationInput r2 = sanitizer.sanitize(discovery, scenario, knowledge, plan);

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    @DisplayName("Deve não modificar objetos originais")
    void deveNaoModificarObjetosOriginais() {
        ProjectKnowledgeResult knowledge = GenerationTestData.completeKnowledge("pages/LoginPage.ts");
        int originalSize = knowledge.components().size();
        TechnicalPlanResult plan = GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
        int originalActions = plan.fileActions().size();

        sanitizer.sanitize(GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), knowledge, plan);

        assertThat(knowledge.components()).hasSize(originalSize);
        assertThat(plan.fileActions()).hasSize(originalActions);
    }

    @Test
    @DisplayName("Deve respeitar limite total razoável")
    void deveRespeitarLimiteTotal() {
        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"),
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.fileActions().size()).isLessThanOrEqualTo(GenerationInputSanitizer.MAX_FILE_ACTIONS);
        assertThat(result.reusableComponents().size()).isLessThanOrEqualTo(GenerationInputSanitizer.MAX_COMPONENTS);
        assertThat(result.steps().size()).isLessThanOrEqualTo(GenerationInputSanitizer.MAX_STEPS);
    }

    @Test
    @DisplayName("Deve incluir framework, linguagem e plano no output")
    void deveIncluirFrameworkLinguagemEPlano() {
        SanitizedGenerationInput result = sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(),
                GenerationTestData.completeKnowledge(), GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST))
        );

        assertThat(result.framework()).isNotNull();
        assertThat(result.language()).isNotNull();
        assertThat(result.planTitle()).isNotNull();
        assertThat(result.fileActions()).isNotEmpty();
    }

    // --- helper ---

    private ProjectKnowledgeResult knowledgeWith(List<ProjectComponent> components) {
        return new ProjectKnowledgeResult(
                Path.of("/project"), components, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), new NamingConvention(null, null, null, null, null, List.of(), ReuseConfidence.UNKNOWN),
                List.of(), List.of(), List.of(), List.of(), KnowledgeStatus.COMPLETE, true
        );
    }
}
