package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyValidationException;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApplyPreconditionValidator - Testes Unitários")
class ApplyPreconditionValidatorTest {

    private final ApplyPreconditionValidator validator = new ApplyPreconditionValidator();

    @Test
    @DisplayName("Deve aceitar plano READY, geração COMPLETED, review APPROVED e aprovação válida")
    void deveAceitarCenarioFeliz() {
        assertThatCode(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar plano READY_WITH_WARNINGS")
    void deveAceitarPlanoReadyWithWarnings() {
        TechnicalPlanResult plan = new TechnicalPlanResult("t", "s", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus.READY_WITH_WARNINGS,
                com.br.criarcenariotestes.business.autoqa.model.planning.PlanningConfidence.MEDIUM, true);

        assertThatCode(() -> validator.validate(plan, generation(GenerationStatus.COMPLETED_WITH_WARNINGS),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar plano BLOCKED")
    void deveRejeitarPlanoBlocked() {
        assertThatThrownBy(() -> validator.validate(GenerationTestData.blockedPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    @DisplayName("Deve rejeitar plano INVALID")
    void deveRejeitarPlanoInvalid() {
        assertThatThrownBy(() -> validator.validate(GenerationTestData.invalidPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar geração PARTIAL")
    void deveRejeitarGeracaoPartial() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.PARTIAL),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("PARTIAL");
    }

    @Test
    @DisplayName("Deve rejeitar geração FAILED")
    void deveRejeitarGeracaoFailed() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.FAILED),
                review(ReviewStatus.APPROVED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar review CHANGES_REQUIRED")
    void deveRejeitarReviewChangesRequired() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.CHANGES_REQUIRED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar review BLOCKED")
    void deveRejeitarReviewBlocked() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.BLOCKED), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar review INVALID")
    void deveRejeitarReviewInvalid() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.INVALID), approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar aprovação nula")
    void deveRejeitarAprovacaoNula() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED), null))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("ApplyApproval");
    }

    @Test
    @DisplayName("Deve rejeitar aprovação com approved=false")
    void deveRejeitarAprovacaoNaoAprovada() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED), approval(false, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("approved");
    }

    @Test
    @DisplayName("Deve rejeitar review APPROVED_WITH_WARNINGS sem allowWarnings")
    void deveRejeitarReviewComWarningsSemAllowWarnings() {
        assertThatThrownBy(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), approval(true, false, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("allowWarnings");
    }

    @Test
    @DisplayName("Deve aceitar review APPROVED_WITH_WARNINGS com allowWarnings=true")
    void deveAceitarReviewComWarningsComAllowWarnings() {
        assertThatCode(() -> validator.validate(readyPlan(), generation(GenerationStatus.COMPLETED),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), approval(true, true, ApplyOperation.CREATE)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve rejeitar quando há arquivo CREATE mas aprovação não cobre CREATE")
    void deveRejeitarQuandoCreateNaoAprovado() {
        assertThatThrownBy(() -> validator.validate(readyPlan(),
                generationWithFile(GeneratedFileOperation.CREATE), review(ReviewStatus.APPROVED),
                approval(true, true, ApplyOperation.UPDATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("CREATE");
    }

    @Test
    @DisplayName("Deve rejeitar quando há arquivo UPDATE mas aprovação não cobre UPDATE")
    void deveRejeitarQuandoUpdateNaoAprovado() {
        assertThatThrownBy(() -> validator.validate(readyPlan(),
                generationWithFile(GeneratedFileOperation.UPDATE), review(ReviewStatus.APPROVED),
                approval(true, true, ApplyOperation.CREATE)))
                .isInstanceOf(ApplyValidationException.class)
                .hasMessageContaining("UPDATE");
    }

    @Test
    @DisplayName("Deve rejeitar quando UPDATE está na lista aprovada mas allowFileUpdate=false")
    void deveRejeitarQuandoAllowFileUpdateFalso() {
        ApplyApproval approval = new ApplyApproval(true, "qa.lead", LocalDateTime.now(),
                List.of(ApplyOperation.UPDATE), false, true);

        assertThatThrownBy(() -> validator.validate(readyPlan(),
                generationWithFile(GeneratedFileOperation.UPDATE), review(ReviewStatus.APPROVED), approval))
                .isInstanceOf(ApplyValidationException.class);
    }

    @Test
    @DisplayName("Deve aceitar quando CREATE e UPDATE estão ambos cobertos pela aprovação")
    void deveAceitarQuandoCreateEUpdateCobertos() {
        GenerationResult generation = new GenerationResult(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT",
                List.of(generatedFile(GeneratedFileOperation.CREATE), generatedFile(GeneratedFileOperation.UPDATE)),
                List.of(), List.of(), null, null, GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        assertThatCode(() -> validator.validate(readyPlan(), generation, review(ReviewStatus.APPROVED),
                approval(true, true, ApplyOperation.CREATE, ApplyOperation.UPDATE)))
                .doesNotThrowAnyException();
    }

    private TechnicalPlanResult readyPlan() {
        return GenerationTestData.readyPlan();
    }

    private GenerationResult generation(GenerationStatus status) {
        return new GenerationResult(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", List.of(), List.of(), List.of(),
                null, null, status, GenerationConfidence.HIGH, status == GenerationStatus.COMPLETED || status == GenerationStatus.COMPLETED_WITH_WARNINGS);
    }

    private GenerationResult generationWithFile(GeneratedFileOperation operation) {
        return new GenerationResult(UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", List.of(generatedFile(operation)),
                List.of(), List.of(), null, null, GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    private GeneratedFile generatedFile(GeneratedFileOperation operation) {
        return new GeneratedFile("src/Foo.spec.ts", operation, PlanComponentType.TEST, "conteudo", "UTF-8",
                "hash", GeneratedFileStatus.GENERATED, operation == GeneratedFileOperation.UPDATE, List.of(), List.of(), List.of());
    }

    private CodeReviewResult review(ReviewStatus status) {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                status, ReviewConfidence.HIGH, false, status == ReviewStatus.APPROVED || status == ReviewStatus.APPROVED_WITH_WARNINGS);
    }

    private ApplyApproval approval(boolean approved, boolean allowWarnings, ApplyOperation... operations) {
        return new ApplyApproval(approved, "qa.lead", LocalDateTime.now(), List.of(operations), true, allowWarnings);
    }
}
