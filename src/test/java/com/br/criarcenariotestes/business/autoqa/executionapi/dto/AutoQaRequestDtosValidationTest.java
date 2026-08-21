package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTOs de request - Testes de Bean Validation")
class AutoQaRequestDtosValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("scenario em branco não é mais violação de bean validation - a origem pode ser um cenarioId")
    void scenarioEmBrancoNaoEhMaisViolacaoDeBeanValidation() {
        // A obrigatoriedade virou "scenario OU cenarioId", que @NotBlank por
        // campo não expressa. Quem valida isso é o CenarioSalvoResolver, e o
        // 400 correspondente está coberto em AutoQaExecutionControllerTest.
        Set<ConstraintViolation<AutoQaCreateExecutionRequest>> violations =
                validator.validate(new AutoQaCreateExecutionRequest(" ", "/projeto"));
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AutoQaCreateExecutionRequest deve aceitar cenarioId como origem, sem scenario")
    void createExecutionRequestDeveAceitarCenarioId() {
        Set<ConstraintViolation<AutoQaCreateExecutionRequest>> violations =
                validator.validate(new AutoQaCreateExecutionRequest(null, "68a1f0c2db2c9947d6eae6ab", "/projeto"));
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AutoQaCreateExecutionRequest deve rejeitar projectPath em branco")
    void createExecutionRequestDeveRejeitarProjectPathEmBranco() {
        Set<ConstraintViolation<AutoQaCreateExecutionRequest>> violations =
                validator.validate(new AutoQaCreateExecutionRequest("cenário", ""));
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("AutoQaCreateExecutionRequest válido não deve gerar violação")
    void createExecutionRequestValidoNaoGeraViolacao() {
        Set<ConstraintViolation<AutoQaCreateExecutionRequest>> violations =
                validator.validate(new AutoQaCreateExecutionRequest("cenário", "/projeto"));
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AutoQaApplyApprovalRequest deve rejeitar approvedBy em branco")
    void applyApprovalRequestDeveRejeitarApprovedByEmBranco() {
        Set<ConstraintViolation<AutoQaApplyApprovalRequest>> violations = validator.validate(
                new AutoQaApplyApprovalRequest("", List.of(ApplyOperation.CREATE), true, true));
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("AutoQaApplyApprovalRequest deve rejeitar authorizedOperations vazio")
    void applyApprovalRequestDeveRejeitarOperacoesVazias() {
        Set<ConstraintViolation<AutoQaApplyApprovalRequest>> violations = validator.validate(
                new AutoQaApplyApprovalRequest("qa.lead", List.of(), true, true));
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("AutoQaApplyApprovalRequest.toDomain() deve sempre produzir approved=true")
    void applyApprovalRequestToDomainSempreApproved() {
        var request = new AutoQaApplyApprovalRequest("qa.lead", List.of(ApplyOperation.CREATE), true, true);
        assertThat(request.toDomain().approved()).isTrue();
        assertThat(request.toDomain().approvedBy()).isEqualTo("qa.lead");
    }

    @Test
    @DisplayName("AutoQaExecutionApprovalRequest deve rejeitar allowedCommands vazio")
    void executionApprovalRequestDeveRejeitarComandosVazios() {
        Set<ConstraintViolation<AutoQaExecutionApprovalRequest>> violations = validator.validate(
                new AutoQaExecutionApprovalRequest("qa.lead", Set.of(), true, false, false));
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("AutoQaExecutionApprovalRequest.toDomain() deve sempre produzir approved=true")
    void executionApprovalRequestToDomainSempreApproved() {
        var request = new AutoQaExecutionApprovalRequest("qa.lead", Set.of(ExecutionCommandId.PLAYWRIGHT_TEST), true, false, false);
        assertThat(request.toDomain().approved()).isTrue();
    }

    @Test
    @DisplayName("AutoQaCancelRequest não exige reason (pode ser nulo)")
    void cancelRequestNaoExigeReason() {
        Set<ConstraintViolation<AutoQaCancelRequest>> violations = validator.validate(new AutoQaCancelRequest(null));
        assertThat(violations).isEmpty();
    }
}
