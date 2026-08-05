package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PlanningValidator - Testes Unitários")
class PlanningValidatorTest {

    private PlanningValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlanningValidator();
    }

    @Test
    @DisplayName("Deve aceitar plano READY válido")
    void deveAceitarPlanoReadyValido() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar plano BLOCKED válido")
    void deveAceitarPlanoBloqueado() {
        TechnicalPlanResult result = PlanningTestData.blockedPlan();
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar plano INVALID")
    void deveAceitarPlanoInvalido() {
        TechnicalPlanResult result = PlanningTestData.invalidPlan();
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar result nulo")
    void deveRejeitarResultNulo() {
        assertThatThrownBy(() -> validator.validate(null,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("null");
    }

    @Test
    @DisplayName("Deve rejeitar title nulo")
    void deveRejeitarTitleNulo() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            null, "strategy", List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("title");
    }

    @Test
    @DisplayName("Deve rejeitar title em branco")
    void deveRejeitarTitleEmBranco() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "  ", "strategy", List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar strategy nula")
    void deveRejeitarStrategyNula() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", null, List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("strategy");
    }

    @Test
    @DisplayName("Deve rejeitar path traversal em relativePath")
    void deveRejeitarPathTraversal() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "../etc/passwd", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("traversal");
    }

    @Test
    @DisplayName("Deve rejeitar caminho absoluto Unix")
    void deveRejeitarCaminhoAbsolutoUnix() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "/etc/passwd", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("absoluto");
    }

    @Test
    @DisplayName("Deve rejeitar file URI")
    void deveRejeitarFileUri() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "file:///etc/passwd", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("URI");
    }

    @Test
    @DisplayName("Deve rejeitar relativePath duplicado")
    void deveRejeitarRelativePathDuplicado() {
        PlannedFileAction a1 = PlanningTestData.createAction("tests/same.ts");
        PlannedFileAction a2 = PlanningTestData.createAction("tests/same.ts");
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(a1, a2), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), null, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("duplicado");
    }

    @Test
    @DisplayName("Deve rejeitar REUSE para arquivo inexistente")
    void deveRejeitarReuseParaArquivoInexistente() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "tests/nonexistent.ts", FileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
            "Reutilizar", true, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("inexistente");
    }

    @Test
    @DisplayName("Deve aceitar REUSE para arquivo existente")
    void deveAceitarReuseParaArquivoExistente() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        TechnicalPlanResult result = planWithAction(PlanningTestData.reuseAction("pages/LoginPage.ts"));
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar UPDATE sem approvalRequirement")
    void deveRejeitarUpdateSemApproval() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        PlannedFileAction updateNoApproval = new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
            "Atualizar", true, true, ApprovalRequirement.NONE, List.of(), List.of()
        );
        TechnicalPlanResult result = planWithAction(updateNoApproval);
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("NONE");
    }

    @Test
    @DisplayName("Deve aceitar UPDATE com approvalRequirement")
    void deveAceitarUpdateComApproval() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        TechnicalPlanResult result = planWithAction(PlanningTestData.updateAction("pages/LoginPage.ts"));
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar CREATE para arquivo existente")
    void deveRejeitarCreateParaArquivoExistente() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.CREATE, PlanComponentType.PAGE_OBJECT,
            "Criar", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("existente");
    }

    @Test
    @DisplayName("Deve rejeitar código na reason")
    void deveRejeitarCodigoNaReason() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "public class Login {}", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("Código");
    }

    @Test
    @DisplayName("Deve rejeitar comando na reason")
    void deveRejeitarComandoNaReason() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "executar npm install", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("Comando");
    }

    @Test
    @DisplayName("Deve rejeitar INVALID com valid=true")
    void deveRejeitarInvalidComValidTrue() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.INVALID, PlanningConfidence.UNKNOWN, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("INVALID");
    }

    @Test
    @DisplayName("Deve rejeitar READY com valid=false")
    void deveRejeitarReadyComValidFalse() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, false
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("READY");
    }

    @Test
    @DisplayName("Deve rejeitar READY sem ações")
    void deveRejeitarReadySemAcoes() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("ação");
    }

    @Test
    @DisplayName("Deve rejeitar READY_WITH_WARNINGS sem warnings")
    void deveRejeitarReadyWithWarningsSemWarnings() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(),
            PlanningStatus.READY_WITH_WARNINGS, PlanningConfidence.MEDIUM, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("READY_WITH_WARNINGS");
    }

    @Test
    @DisplayName("Deve rejeitar BLOCKED sem warning nem risk bloqueante")
    void deveRejeitarBlockedSemWarningNemRisk() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), PlanningStatus.BLOCKED, PlanningConfidence.LOW, false
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("BLOCKED");
    }

    @Test
    @DisplayName("Deve rejeitar knowledge PARTIAL sem warnings no plano")
    void deveRejeitarKnowledgePartialSemWarnings() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(),
            PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        ProjectKnowledgeResult partialKnowledge = PlanningTestData.partialKnowledge();
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), partialKnowledge))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("warnings");
    }

    @Test
    @DisplayName("Deve rejeitar knowledge EMPTY sem warnings no plano")
    void deveRejeitarKnowledgeEmptySemWarnings() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(),
            PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.emptyKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("warnings");
    }

    @Test
    @DisplayName("Deve rejeitar ReuseDecision com reuse=true e path inexistente")
    void deveRejeitarReuseDecisionComPathInexistente() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(),
            List.of(PlanningTestData.reuseDecision("pages/NonExistent.ts", true)),
            List.of(), List.of(), List.of(), List.of(), List.of(),
            null, PlanningConfidence.HIGH, true
        );
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Deve aceitar ReuseDecision com reuse=false e path inexistente")
    void deveAceitarReuseDecisionComReuseFalse() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(),
            List.of(PlanningTestData.reuseDecision("pages/NonExistent.ts", false)),
            List.of(), List.of(), List.of(), List.of(), List.of(),
            PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar status null sem erro de coerência")
    void deveAceitarStatusNullSemErro() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), null, PlanningConfidence.HIGH, true
        );
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar BLOCKED com risk bloqueante")
    void deveAceitarBlockedComRiskBloqueante() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(), List.of(),
            List.of(new PlanningRisk("Risco bloqueante", "Alto", "Mitigação", true)),
            List.of(), List.of(), List.of(), List.of(),
            PlanningStatus.BLOCKED, PlanningConfidence.LOW, false
        );
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar READY com reuseDecision reuse=true como ação")
    void deveAceitarReadyComReuseDecision() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy", List.of(), List.of(),
            List.of(PlanningTestData.reuseDecision("pages/LoginPage.ts", true)),
            List.of(), List.of(), List.of(), List.of(), List.of(),
            PlanningStatus.READY, PlanningConfidence.HIGH, true
        );
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar REUSE com existingFile=false")
    void deveRejeitarReuseComExistingFileFalso() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
            "Reutilizar", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("existingFile");
    }

    @Test
    @DisplayName("Deve rejeitar CREATE com existingFile=true")
    void deveRejeitarCreateComExistingFileTrue() {
        TechnicalPlanResult result = planWithAction(new PlannedFileAction(
            "tests/novo.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Criar", true, true, ApprovalRequirement.NONE, List.of(), List.of()
        ));
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge()))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("existingFile");
    }

    // ============================================================
    // ACEITE: Ítem 2 — UPDATE: aprovação FILE_UPDATE_REQUIRED ou mais restritiva
    // ============================================================

    @Test
    @DisplayName("Deve rejeitar UPDATE com REVIEW_REQUIRED (menos restritivo que FILE_UPDATE_REQUIRED)")
    void deveRejeitarUpdateComReviewRequired() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        PlannedFileAction updateReview = new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
            "Atualizar com revisão simples", true, true,
            ApprovalRequirement.REVIEW_REQUIRED, List.of(), List.of()
        );
        TechnicalPlanResult result = planWithAction(updateReview);
        assertThatThrownBy(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .isInstanceOf(PlanningValidationException.class)
            .hasMessageContaining("FILE_UPDATE_REQUIRED");
    }

    @Test
    @DisplayName("Deve aceitar UPDATE com MANUAL_DECISION_REQUIRED (mais restritivo que FILE_UPDATE_REQUIRED)")
    void deveAceitarUpdateComManualDecisionRequired() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        PlannedFileAction updateManual = new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
            "Atualizar com decisão manual", true, true,
            ApprovalRequirement.MANUAL_DECISION_REQUIRED, List.of(), List.of()
        );
        TechnicalPlanResult result = planWithAction(updateManual);
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar UPDATE com DEPENDENCY_CHANGE_REQUIRED")
    void deveAceitarUpdateComDependencyChangeRequired() {
        ProjectKnowledgeResult knowledge = PlanningTestData.completeKnowledge("pages/LoginPage.ts");
        PlannedFileAction updateDep = new PlannedFileAction(
            "pages/LoginPage.ts", FileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
            "Atualizar alterando dependência", true, true,
            ApprovalRequirement.DEPENDENCY_CHANGE_REQUIRED, List.of(), List.of()
        );
        TechnicalPlanResult result = planWithAction(updateDep);
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), knowledge))
            .doesNotThrowAnyException();
    }

    // ============================================================
    // ACEITE: Item 4 — PARTIAL/EMPTY com warnings → deve aceitar
    // ============================================================

    @Test
    @DisplayName("Deve aceitar knowledge PARTIAL quando o plano contém warnings")
    void deveAceitarKnowledgePartialComWarnings() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(),
            List.of(new PlanningWarning("PARTIAL_KNOWLEDGE", "Conhecimento parcial disponível", false)),
            List.of(), List.of(), List.of(),
            PlanningStatus.READY_WITH_WARNINGS, PlanningConfidence.MEDIUM, true
        );
        ProjectKnowledgeResult partialKnowledge = PlanningTestData.partialKnowledge();
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), partialKnowledge))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar knowledge EMPTY quando o plano contém warnings")
    void deveAceitarKnowledgeEmptyComWarnings() {
        TechnicalPlanResult result = new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(PlanningTestData.createAction("tests/x.ts")),
            List.of(), List.of(), List.of(),
            List.of(new PlanningWarning("EMPTY_KNOWLEDGE", "Nenhum componente catalogado", true)),
            List.of(), List.of(), List.of(),
            PlanningStatus.READY_WITH_WARNINGS, PlanningConfidence.LOW, true
        );
        assertThatCode(() -> validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.emptyKnowledge()))
            .doesNotThrowAnyException();
    }

    // ============================================================
    // ACEITE: Item 5 — PlanningValidator não modifica o plano
    // ============================================================

    @Test
    @DisplayName("Deve retornar a mesma instância recebida sem criar cópia")
    void deveRetornarMesmaInstancia() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        TechnicalPlanResult returned = validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge());
        assertThat(returned == result).isTrue();
    }

    @Test
    @DisplayName("Deve não alterar status do plano recebido")
    void deveNaoAlterarStatus() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        PlanningStatus statusAntes = result.status();
        validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge());
        assertThat(result.status()).isEqualTo(statusAntes);
    }

    @Test
    @DisplayName("Deve não adicionar warnings ao plano recebido")
    void deveNaoAdicionarWarnings() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        int warningsAntes = result.warnings().size();
        validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge());
        assertThat(result.warnings()).hasSize(warningsAntes);
    }

    @Test
    @DisplayName("Deve não alterar confidence do plano recebido")
    void deveNaoAlterarConfidence() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        PlanningConfidence confidenceAntes = result.confidence();
        validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge());
        assertThat(result.confidence()).isEqualTo(confidenceAntes);
    }

    @Test
    @DisplayName("Deve não alterar a lista de fileActions do plano recebido")
    void deveNaoAlterarFileActions() {
        TechnicalPlanResult result = PlanningTestData.readyPlan();
        List<PlannedFileAction> actionsAntes = new ArrayList<>(result.fileActions());
        validator.validate(result,
            PlanningTestData.discovery(), PlanningTestData.validScenario(), PlanningTestData.completeKnowledge());
        assertThat(result.fileActions()).isEqualTo(actionsAntes);
    }

    // --- helper ---

    private TechnicalPlanResult planWithAction(PlannedFileAction action) {
        return new TechnicalPlanResult(
            "titulo", "strategy",
            List.of(action),
            List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(),
            null, PlanningConfidence.HIGH, true
        );
    }
}
