package com.br.criarcenariotestes.business.autoqa.context;

import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningConfidence;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.planning.PlanningTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutoQaContext - Testes Unitários")
class AutoQaContextTest {

    @Test
    @DisplayName("Deve criar contexto com status CREATED")
    void deveCriarContextoComStatusCreated() {
        AutoQaContext context = AutoQaContext.create("Cenário válido", "/projeto");

        assertThat(context.getExecutionId()).isNotNull();
        assertThat(context.getScenario()).isEqualTo("Cenário válido");
        assertThat(context.getProjectPath()).isEqualTo("/projeto");
        assertThat(context.getStartedAt()).isNotNull();
        assertThat(context.getFinishedAt()).isNull();
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.CREATED);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getAgentExecutions()).isEmpty();
        assertThat(context.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar cenário nulo")
    void deveRejeitarCenarioNulo() {
        assertThatThrownBy(() -> AutoQaContext.create(null, "/projeto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar cenário vazio")
    void deveRejeitarCenarioVazio() {
        assertThatThrownBy(() -> AutoQaContext.create("   ", "/projeto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar caminho nulo")
    void deveRejeitarCaminhoNulo() {
        assertThatThrownBy(() -> AutoQaContext.create("Cenário", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath");
    }

    @Test
    @DisplayName("Deve rejeitar caminho vazio")
    void deveRejeitarCaminhoVazio() {
        assertThatThrownBy(() -> AutoQaContext.create("Cenário", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath");
    }

    @Test
    @DisplayName("Deve remover espaços do cenário e do caminho")
    void deveRemoverEspacosDoCenarioEDoCaminho() {
        AutoQaContext context = AutoQaContext.create("  Cenário  ", "  /projeto  ");

        assertThat(context.getScenario()).isEqualTo("Cenário");
        assertThat(context.getProjectPath()).isEqualTo("/projeto");
    }

    @Test
    @DisplayName("Deve registrar execução do agente")
    void deveRegistrarExecucaoDoAgente() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AgentExecutionResult result = AgentExecutionResult.success("Executado");

        context.addAgentExecution(result);

        assertThat(context.getAgentExecutions()).containsExactly(result);
    }

    @Test
    @DisplayName("Deve rejeitar resultado nulo")
    void deveRejeitarResultadoNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.addAgentExecution(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve finalizar com sucesso")
    void deveFinalizarComSucesso() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startWorkflow();
        context.finishSuccessfully();

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getFinishedAt()).isNotNull();
        assertThat(context.getFinishedAt()).isAfterOrEqualTo(context.getStartedAt());
    }

    @Test
    @DisplayName("Deve finalizar com erro")
    void deveFinalizarComErro() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startWorkflow();
        context.finishWithError("Falha");

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getErrors()).containsExactly("Falha");
        assertThat(context.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar listas imutáveis")
    void deveRetornarListasImutaveis() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.getAgentExecutions().add(AgentExecutionResult.success("X")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.getErrors().add("erro"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve registrar agente atual")
    void deveRegistrarAgenteAtual() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startAgent("  Agente 1  ");

        assertThat(context.getCurrentAgent()).isEqualTo("Agente 1");
    }

    @Test
    @DisplayName("Deve limpar agente atual ao finalizar")
    void deveLimparAgenteAtualAoFinalizar() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startAgent("Agente 1");
        context.finishSuccessfully();

        assertThat(context.getCurrentAgent()).isNull();
    }

    @Test
    @DisplayName("Deve registrar resultado da descoberta")
    void deveRegistrarResultadoDaDescoberta() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        ProjectDiscoveryResult result = new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(AutomationFramework.PLAYWRIGHT),
                List.of("PLAYWRIGHT"),
                "playwright.config.ts",
                List.of("playwright.config.ts"),
                List.of(),
                DiscoveryConfidence.HIGH,
                true
        );

        context.registerProjectDiscovery(result);

        assertThat(context.getProjectDiscoveryResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve rejeitar resultado da descoberta nulo")
    void deveRejeitarResultadoDaDescobertaNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.registerProjectDiscovery(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar segunda descoberta na mesma execução")
    void deveRejeitarSegundaDescobertaNaMesmaExecucao() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        ProjectDiscoveryResult result = new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.UNKNOWN,
                AutomationLanguage.UNKNOWN,
                PackageManager.UNKNOWN,
                BuildTool.UNKNOWN,
                Set.of(),
                Set.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                DiscoveryConfidence.UNKNOWN,
                true
        );

        context.registerProjectDiscovery(result);

        assertThatThrownBy(() -> context.registerProjectDiscovery(result))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve registrar resultado da análise do cenário")
    void deveRegistrarResultadoDaAnaliseDoCenario() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        ScenarioAnalysisResult analysis = sampleAnalysis();

        context.registerScenarioAnalysis(analysis);

        assertThat(context.getScenarioAnalysisResult()).isEqualTo(analysis);
    }

    @Test
    @DisplayName("Deve rejeitar resultado da análise nulo")
    void deveRejeitarResultadoDaAnaliseNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());

        assertThatThrownBy(() -> context.registerScenarioAnalysis(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir descoberta antes da análise")
    void deveExigirDescobertaAntesDaAnalise() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.registerScenarioAnalysis(sampleAnalysis()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve rejeitar segunda análise na mesma execução")
    void deveRejeitarSegundaAnaliseNaMesmaExecucao() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        ScenarioAnalysisResult analysis = sampleAnalysis();

        context.registerScenarioAnalysis(analysis);

        assertThatThrownBy(() -> context.registerScenarioAnalysis(analysis))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("analysis");
    }

    @Test
    @DisplayName("Deve registrar ProjectKnowledge")
    void deveRegistrarProjectKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        ProjectKnowledgeResult knowledge = sampleKnowledge();

        context.registerProjectKnowledge(knowledge);

        assertThat(context.getProjectKnowledgeResult()).isEqualTo(knowledge);
    }

    @Test
    @DisplayName("Deve rejeitar ProjectKnowledge nulo")
    void deveRejeitarProjectKnowledgeNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());

        assertThatThrownBy(() -> context.registerProjectKnowledge(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir descoberta antes do knowledge")
    void deveExigirDiscoveryAntesDoKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.registerProjectKnowledge(sampleKnowledge()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve exigir scenario analysis antes do knowledge")
    void deveExigirScenarioAnalysisAntesDoKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());

        assertThatThrownBy(() -> context.registerProjectKnowledge(sampleKnowledge()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Scenario analysis");
    }

    @Test
    @DisplayName("Deve rejeitar segundo ProjectKnowledge")
    void deveRejeitarSegundoProjectKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        ProjectKnowledgeResult knowledge = sampleKnowledge();

        context.registerProjectKnowledge(knowledge);

        assertThatThrownBy(() -> context.registerProjectKnowledge(knowledge))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("knowledge");
    }

    private ProjectDiscoveryResult sampleDiscovery() {
        return new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(AutomationFramework.PLAYWRIGHT),
                List.of("PLAYWRIGHT"),
                "playwright.config.ts",
                List.of("playwright.config.ts"),
                List.of(),
                DiscoveryConfidence.HIGH,
                true
        );
    }

    private ScenarioAnalysisResult sampleAnalysis() {
        return new ScenarioAnalysisResult(
                "Login válido",
                "Validar acesso",
                List.of("Usuário cadastrado"),
                List.of(new ScenarioStep(1, "Acessar a tela de login", "A tela é exibida", List.of())),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Usuário"),
                List.of(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                List.of(),
                true
        );
    }

    private ProjectKnowledgeResult sampleKnowledge() {
        ProjectComponent component = new ProjectComponent(
                "src/test/java/com/example/LoginPage.java",
                "LoginPage",
                ComponentType.PAGE_OBJECT,
                SourceLanguage.JAVA,
                "com.example",
                List.of("LoginPage"),
                List.of("open"),
                List.of("import org.openqa.selenium.By"),
                List.of("@Page"),
                List.of("PAGE_OBJECT"),
                false,
                true,
                List.of()
        );
        return new ProjectKnowledgeResult(
                Path.of("/projeto"),
                List.of(component),
                List.of(),
                List.of(component),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ReuseCandidate("src/test/java/com/example/LoginPage.java", ComponentType.PAGE_OBJECT, "login", ReuseConfidence.HIGH, List.of("login"))),
                new NamingConvention("*.java", "*Page.java", "PascalCase", "camelCase", "src/test/java", List.of("src/test/java/com/example/LoginPage.java"), ReuseConfidence.HIGH),
                List.of("src/test/java"),
                List.of("src/main/java"),
                List.of("node_modules"),
                List.of(),
                KnowledgeStatus.COMPLETE,
                true
        );
    }

    // ---- TechnicalPlan tests ----

    @Test
    @DisplayName("Deve registrar technical plan após knowledge")
    void deveRegistrarTechnicalPlan() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        context.registerProjectKnowledge(sampleKnowledge());
        context.registerTechnicalPlan(PlanningTestData.readyPlan());
        assertThat(context.getTechnicalPlanResult()).isNotNull();
    }

    @Test
    @DisplayName("Deve rejeitar technical plan nulo")
    void deveRejeitarTechnicalPlanNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        context.registerProjectKnowledge(sampleKnowledge());
        assertThatThrownBy(() -> context.registerTechnicalPlan(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar technical plan sem knowledge")
    void deveRejeitarTechnicalPlanSemKnowledge() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        assertThatThrownBy(() -> context.registerTechnicalPlan(PlanningTestData.readyPlan()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Deve rejeitar technical plan sem discovery")
    void deveRejeitarTechnicalPlanSemDiscovery() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThatThrownBy(() -> context.registerTechnicalPlan(PlanningTestData.readyPlan()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve rejeitar technical plan duplicado")
    void deveRejeitarTechnicalPlanDuplicado() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        context.registerProjectKnowledge(sampleKnowledge());
        context.registerTechnicalPlan(PlanningTestData.readyPlan());
        assertThatThrownBy(() -> context.registerTechnicalPlan(PlanningTestData.readyPlan()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Deve retornar null para technical plan não registrado")
    void deveRetornarNullParaTechnicalPlanNaoRegistrado() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThat(context.getTechnicalPlanResult()).isNull();
    }

    // ---- Generation tests ----

    @Test
    @DisplayName("Deve registrar generation result após technical plan")
    void deveRegistrarGenerationResult() {
        AutoQaContext context = contextComTechnicalPlan();
        GenerationResult generationResult = sampleGenerationResult();

        context.registerGeneration(generationResult);

        assertThat(context.getGenerationResult()).isEqualTo(generationResult);
    }

    @Test
    @DisplayName("Deve rejeitar generation result nulo")
    void deveRejeitarGenerationResultNulo() {
        AutoQaContext context = contextComTechnicalPlan();

        assertThatThrownBy(() -> context.registerGeneration(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve exigir discovery antes da generation")
    void deveExigirDiscoveryAntesDaGeneration() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.registerGeneration(sampleGenerationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve exigir scenario antes da generation")
    void deveExigirScenarioAntesDaGeneration() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());

        assertThatThrownBy(() -> context.registerGeneration(sampleGenerationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Scenario analysis");
    }

    @Test
    @DisplayName("Deve exigir knowledge antes da generation")
    void deveExigirKnowledgeAntesDaGeneration() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());

        assertThatThrownBy(() -> context.registerGeneration(sampleGenerationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Deve exigir planning antes da generation")
    void deveExigirPlanningAntesDaGeneration() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        context.registerProjectKnowledge(sampleKnowledge());

        assertThatThrownBy(() -> context.registerGeneration(sampleGenerationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("plan");
    }

    @Test
    @DisplayName("Deve rejeitar segundo generation result")
    void deveRejeitarSegundoGenerationResult() {
        AutoQaContext context = contextComTechnicalPlan();
        context.registerGeneration(sampleGenerationResult());

        assertThatThrownBy(() -> context.registerGeneration(sampleGenerationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Deve retornar null para generation não registrada")
    void deveRetornarNullParaGenerationNaoRegistrada() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        assertThat(context.getGenerationResult()).isNull();
    }

    private AutoQaContext contextComTechnicalPlan() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        context.registerProjectDiscovery(sampleDiscovery());
        context.registerScenarioAnalysis(sampleAnalysis());
        context.registerProjectKnowledge(sampleKnowledge());
        context.registerTechnicalPlan(PlanningTestData.readyPlan());
        return context;
    }

    private GenerationResult sampleGenerationResult() {
        UUID executionId = UUID.randomUUID();
        return new GenerationResult(
                executionId, "PLAYWRIGHT", "TYPESCRIPT",
                List.of(), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true
        );
    }
}
