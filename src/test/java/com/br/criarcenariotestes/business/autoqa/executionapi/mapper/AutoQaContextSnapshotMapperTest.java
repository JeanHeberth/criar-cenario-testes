package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaSnapshotException;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshot;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.learning.LearningTestData;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutoQaContextSnapshotMapper - Testes Unitários")
class AutoQaContextSnapshotMapperTest {

    private final AutoQaContextSnapshotMapper mapper = new AutoQaContextSnapshotMapper();

    @Test
    @DisplayName("toContext de snapshot vazio deve retornar contexto sem nenhuma fase registrada")
    void toContextDeSnapshotVazioNaoRegistraNada() {
        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(UUID.randomUUID(), Instant.now());

        AutoQaContext context = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(context.getProjectDiscoveryResult()).isNull();
        assertThat(context.getScenarioAnalysisResult()).isNull();
        assertThat(context.getFailureAnalysisResult()).isNull();
    }

    @Test
    @DisplayName("toSnapshot com contexto vazio não deve preencher nenhuma fase")
    void toSnapshotDeContextoVazioNaoPreencheFases() {
        AutoQaContext context = AutoQaContext.create("cenário", "/projeto");
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(context.getExecutionId(), Instant.now());

        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(context, target, null, Instant.now());

        assertThat(snapshot.getDiscovery()).isNull();
        assertThat(snapshot.getScenarioAnalysis()).isNull();
    }

    @Test
    @DisplayName("Round-trip até Discovery deve preservar os dados")
    void roundTripAteDiscovery() {
        AutoQaContext original = AutoQaContext.create("cenário", "/projeto");
        original.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.DISCOVERY, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getProjectDiscoveryResult()).isEqualTo(original.getProjectDiscoveryResult());
        assertThat(reconstructed.getScenarioAnalysisResult()).isNull();
    }

    @Test
    @DisplayName("Round-trip até ScenarioAnalysis deve preservar os dados")
    void roundTripAteScenarioAnalysis() {
        AutoQaContext original = AutoQaContext.create("cenário", "/projeto");
        original.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        original.registerScenarioAnalysis(GenerationTestData.validScenario());

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.SCENARIO_ANALYSIS, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getScenarioAnalysisResult()).isEqualTo(original.getScenarioAnalysisResult());
        assertThat(reconstructed.getProjectKnowledgeResult()).isNull();
    }

    @Test
    @DisplayName("Round-trip até ProjectKnowledge deve preservar os dados")
    void roundTripAteProjectKnowledge() {
        AutoQaContext original = AutoQaContext.create("cenário", "/projeto");
        original.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        original.registerScenarioAnalysis(GenerationTestData.validScenario());
        original.registerProjectKnowledge(GenerationTestData.completeKnowledge("tests/support/loginPage.ts"));

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.PROJECT_KNOWLEDGE, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getProjectKnowledgeResult()).isEqualTo(original.getProjectKnowledgeResult());
        assertThat(reconstructed.getTechnicalPlanResult()).isNull();
    }

    @Test
    @DisplayName("Round-trip até Planning deve preservar os dados")
    void roundTripAtePlanning() {
        AutoQaContext original = contextoAtePlanning();

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.PLANNING, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getTechnicalPlanResult()).isEqualTo(original.getTechnicalPlanResult());
        assertThat(reconstructed.getGenerationResult()).isNull();
        assertThat(snapshot.getLastCompletedStage()).isEqualTo(AutoQaStage.PLANNING);
    }

    @Test
    @DisplayName("Round-trip até Generation deve preservar tudo exceto o conteúdo dos arquivos")
    void roundTripAteGenerationSemConteudo() {
        AutoQaContext original = contextoAtePlanning();
        var file = GenerationTestData.generatedFile("tests/login.spec.ts",
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.CREATE,
                com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT,
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.GENERATED, false);
        var generation = new com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult(
                original.getExecutionId(), "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);
        original.registerGeneration(generation);

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.GENERATION, Instant.now());

        assertThat(snapshot.getGeneration().files().get(0).content()).isNull();
        assertThat(snapshot.getGeneration().files().get(0).relativePath()).isEqualTo("tests/login.spec.ts");

        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");
        assertThat(reconstructed.getGenerationResult().files().get(0).content()).isNull();
        assertThat(reconstructed.getGenerationResult().files().get(0).relativePath()).isEqualTo("tests/login.spec.ts");
    }

    @Test
    @DisplayName("Round-trip até Review deve preservar os dados")
    void roundTripAteReview() {
        AutoQaContext original = contextoAteReview();

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.REVIEW, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getCodeReviewResult()).isEqualTo(original.getCodeReviewResult());
        assertThat(reconstructed.getApplyResult()).isNull();
    }

    @Test
    @DisplayName("Round-trip até Apply (incluindo ApplyApproval) deve preservar os dados")
    void roundTripAteApply() {
        AutoQaContext original = contextoAteApply();

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.APPLY, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getApplyApproval()).isEqualTo(original.getApplyApproval());
        assertThat(reconstructed.getApplyResult()).isEqualTo(original.getApplyResult());
        assertThat(reconstructed.getExecutionApproval()).isNull();
        assertThat(reconstructed.getExecutionResult()).isNull();
    }

    @Test
    @DisplayName("Round-trip incluindo ExecutionApproval deve preservar os dados (fronteira máxima do snapshot)")
    void roundTripAteExecutionApproval() {
        AutoQaContext original = contextoAteApply();
        ExecutionApproval executionApproval = new ExecutionApproval(true, "qa.lead", LocalDateTime.now(),
                Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
        original.registerExecutionApproval(executionApproval);

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.APPLY, Instant.now());
        AutoQaContext reconstructed = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(reconstructed.getExecutionApproval()).isEqualTo(executionApproval);
        assertThat(reconstructed.getExecutionResult()).isNull();
    }

    @Test
    @DisplayName("Snapshot com ordem impossível (planning sem discovery) deve lançar AutoQaSnapshotException")
    void snapshotComOrdemImpossivelDeveLancarExcecao() {
        AutoQaContext original = AutoQaContext.create("cenário", "/projeto");
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshotComGap = mapper.toSnapshot(original, target, null, Instant.now());
        snapshotComGap.setTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST)));

        assertThatThrownBy(() -> mapper.toContext(snapshotComGap, "cenário", "/projeto"))
                .isInstanceOf(AutoQaSnapshotException.class);
    }

    @Test
    @DisplayName("Snapshot inconsistente não deve propagar a IllegalStateException original do AutoQaContext")
    void snapshotInconsistenteNaoPropagaExcecaoOriginal() {
        AutoQaContext original = AutoQaContext.create("cenário", "/projeto");
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshotComGap = mapper.toSnapshot(original, target, null, Instant.now());
        snapshotComGap.setApply(LearningTestData.completedApply(UUID.randomUUID(), "tests/login.spec.ts"));

        assertThatThrownBy(() -> mapper.toContext(snapshotComGap, "cenário", "/projeto"))
                .isInstanceOf(AutoQaSnapshotException.class)
                .cause().isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("toSnapshot não deve modificar o AutoQaContext original")
    void toSnapshotNaoModificaContextoOriginal() {
        AutoQaContext original = contextoAtePlanning();
        var discoveryAntes = original.getProjectDiscoveryResult();

        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        mapper.toSnapshot(original, target, AutoQaStage.PLANNING, Instant.now());

        assertThat(original.getProjectDiscoveryResult()).isSameAs(discoveryAntes);
    }

    @Test
    @DisplayName("toContext deve produzir uma nova instância de AutoQaContext a cada chamada")
    void toContextProduzNovaInstanciaSempre() {
        AutoQaContext original = contextoAtePlanning();
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.PLANNING, Instant.now());

        AutoQaContext first = mapper.toContext(snapshot, "cenário", "/project");
        AutoQaContext second = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(first).isNotSameAs(second);
        assertThat(first.getExecutionId()).isNotEqualTo(second.getExecutionId());
    }

    @Test
    @DisplayName("Reidratação é determinística: mesmos dados de fase reconstroem o mesmo resultado")
    void reidratacaoEhDeterministica() {
        AutoQaContext original = contextoAtePlanning();
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(original.getExecutionId(), Instant.now());
        AutoQaExecutionSnapshot snapshot = mapper.toSnapshot(original, target, AutoQaStage.PLANNING, Instant.now());

        AutoQaContext first = mapper.toContext(snapshot, "cenário", "/project");
        AutoQaContext second = mapper.toContext(snapshot, "cenário", "/project");

        assertThat(first.getProjectDiscoveryResult()).isEqualTo(second.getProjectDiscoveryResult());
        assertThat(first.getTechnicalPlanResult()).isEqualTo(second.getTechnicalPlanResult());
    }

    @Test
    @DisplayName("Deve rejeitar contexto nulo em toSnapshot")
    void deveRejeitarContextoNuloEmToSnapshot() {
        AutoQaExecutionSnapshot target = AutoQaExecutionSnapshot.createNew(UUID.randomUUID(), Instant.now());
        assertThatThrownBy(() -> mapper.toSnapshot(null, target, null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar snapshot alvo nulo em toSnapshot")
    void deveRejeitarSnapshotAlvoNuloEmToSnapshot() {
        AutoQaContext context = AutoQaContext.create("cenário", "/projeto");
        assertThatThrownBy(() -> mapper.toSnapshot(context, null, null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar snapshot nulo em toContext")
    void deveRejeitarSnapshotNuloEmToContext() {
        assertThatThrownBy(() -> mapper.toContext(null, "cenário", "/projeto"))
                .isInstanceOf(NullPointerException.class);
    }

    // --- helpers ---

    private AutoQaContext contextoAtePlanning() {
        AutoQaContext context = AutoQaContext.create("cenário", "/projeto");
        context.registerProjectDiscovery(GenerationTestData.playwrightDiscovery());
        context.registerScenarioAnalysis(GenerationTestData.validScenario());
        context.registerProjectKnowledge(GenerationTestData.completeKnowledge("tests/support/loginPage.ts"));
        context.registerTechnicalPlan(GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/login.spec.ts", com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST)));
        return context;
    }

    private AutoQaContext contextoAteReview() {
        AutoQaContext context = contextoAtePlanning();
        var file = GenerationTestData.generatedFile("tests/login.spec.ts",
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.CREATE,
                com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT,
                com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.GENERATED, false);
        context.registerGeneration(new com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult(
                context.getExecutionId(), "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true));
        context.registerCodeReview(new com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult(
                context.getExecutionId(), List.of(), List.of(), List.of(), List.of("no-hardcoded-wait"), List.of(), List.of(),
                com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus.APPROVED,
                com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence.HIGH, false, true));
        return context;
    }

    private AutoQaContext contextoAteApply() {
        AutoQaContext context = contextoAteReview();
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(), List.of(ApplyOperation.CREATE), true, true);
        context.registerApplyApproval(approval);
        context.registerApplyResult(LearningTestData.completedApply(context.getExecutionId(), "tests/login.spec.ts"));
        return context;
    }
}
