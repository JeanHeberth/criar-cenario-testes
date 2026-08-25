package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.*;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CodeReviewValidator - Testes Unitários")
class CodeReviewValidatorTest {

    private CodeReviewValidator validator;
    private UUID executionId;

    @BeforeEach
    void setUp() {
        validator = new CodeReviewValidator();
        executionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve validar resposta APPROVED sem issues")
    void deveValidarRespostaAprovada() {
        var response = CodeReviewTestData.approvedResponse("tests/login.spec.ts");
        var result = validate(response, "tests/login.spec.ts");
        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo revisado inexistente na GenerationResult")
    void deveRejeitarArquivoInexistente() {
        var response = CodeReviewTestData.approvedResponse("tests/outro.spec.ts");
        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar arquivo REUSE/NONE sendo revisado (não estava entre CREATE/UPDATE)")
    void deveRejeitarReuseSendoRevisado() {
        UUID id = UUID.randomUUID();
        var generation = new com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult(
                id, "PLAYWRIGHT", "TYPESCRIPT",
                List.of(new com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile(
                        "pages/LoginPage.ts", com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation.REUSE,
                        com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType.PAGE_OBJECT, null, "UTF-8", null,
                        com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus.SKIPPED, true, List.of(), List.of(), List.of())),
                List.of(), List.of(), "root", "manifest.json",
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus.COMPLETED,
                com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence.HIGH, true);

        var response = CodeReviewTestData.approvedResponse("pages/LoginPage.ts");

        assertThatThrownBy(() -> validator.validate(response, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.completeKnowledge(),
                CodeReviewTestData.plan(), generation))
                .isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve normalizar line inválida e PRESERVAR o achado")
    void deveNormalizarLineInvalida() {
        // Caso real: um achado com line inválida derrubava a revisão inteira,
        // levando junto quatro erros de compilação legítimos. A linha é
        // acessório; o defeito apontado pode ser verdadeiro.
        var issue = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "tests/login.spec.ts", 0, "msg", null, "rec", false);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.APPROVED_WITH_WARNINGS, ReviewStatus.APPROVED_WITH_WARNINGS, false, true);

        var saneada = validate(response, "tests/login.spec.ts");

        assertThat(saneada.files().get(0).issues())
                .singleElement()
                .satisfies(i -> {
                    assertThat(i.code()).isEqualTo("CODE");
                    assertThat(i.line()).as("line inválida vira nula, o achado permanece").isNull();
                });
    }

    @Test
    @DisplayName("Deve aceitar CRITICAL sem BLOCKED no arquivo (warn, não throw)")
    void deveAceitarCriticalSemBlocked() {
        var issue = new ReviewIssue("HARDCODED_SECRET", ReviewCategory.SECURITY, ReviewSeverity.CRITICAL,
                "tests/login.spec.ts", null, "segredo", null, "remover", true);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.APPROVED_WITH_WARNINGS, ReviewStatus.BLOCKED, true, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar HIGH com APPROVED no arquivo (warn, não throw)")
    void deveAceitarHighComApproved() {
        var issue = new ReviewIssue("MISSING_ASSERTION", ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                "tests/login.spec.ts", null, "sem assertion", null, "adicionar", false);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.APPROVED, ReviewStatus.CHANGES_REQUIRED, false, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar status global APPROVED com issue HIGH (warn, não throw)")
    void deveAceitarApprovedGlobalComHigh() {
        var issue = new ReviewIssue("MISSING_ASSERTION", ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                "tests/login.spec.ts", null, "sem assertion", null, "adicionar", false);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.CHANGES_REQUIRED, ReviewStatus.APPROVED, false, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar status global APPROVED com issue CRITICAL (warn, não throw)")
    void deveAceitarApprovedGlobalComCritical() {
        var issue = new ReviewIssue("HARDCODED_SECRET", ReviewCategory.SECURITY, ReviewSeverity.CRITICAL,
                "tests/login.spec.ts", null, "segredo", null, "remover", true);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.BLOCKED, ReviewStatus.APPROVED, true, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar BLOCKED sem issue CRITICAL (warn, não throw)")
    void deveAceitarBlockedSemCritical() {
        var issue = new ReviewIssue("MISSING_ASSERTION", ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                "tests/login.spec.ts", null, "sem assertion", null, "adicionar", false);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.CHANGES_REQUIRED, ReviewStatus.BLOCKED, true, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar status global incoerente (warn, não throw)")
    void deveAceitarStatusGlobalIncoerente() {
        var response = new CodeReviewAiResponse(
                List.of(new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.APPROVED,
                        List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true)),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.CHANGES_REQUIRED, ReviewConfidence.HIGH, false, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve aceitar BLOCKED sem humanReviewRequired (warn, não throw)")
    void deveAceitarBlockedSemHumanReview() {
        var issue = new ReviewIssue("HARDCODED_SECRET", ReviewCategory.SECURITY, ReviewSeverity.CRITICAL,
                "tests/login.spec.ts", null, "segredo", null, "remover", true);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.BLOCKED, ReviewStatus.BLOCKED, false, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve rejeitar status INVALID com valid=true")
    void deveRejeitarInvalidComValidTrue() {
        var response = new CodeReviewAiResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.INVALID, ReviewConfidence.UNKNOWN, true, true);

        assertThatThrownBy(() -> validator.validate(response, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.completeKnowledge(),
                CodeReviewTestData.plan(), CodeReviewTestData.generation(executionId)))
                .isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar issue estática (issue) duplicada com severidade inconsistente")
    void deveRejeitarIssueDuplicadaInconsistente() {
        var issue1 = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "tests/login.spec.ts", null, "msg", null, "rec", false);
        var issue2 = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.HIGH,
                "tests/login.spec.ts", null, "msg2", null, "rec2", false);
        var response = new CodeReviewAiResponse(List.of(), List.of(issue1, issue2), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.CHANGES_REQUIRED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar sugestão contendo código completo (Markdown fence)")
    void deveRejeitarSugestaoComCodigo() {
        var suggestion = new ReviewSuggestion("tests/login.spec.ts", "```ts\ncode\n```", ReviewSeverity.LOW, "rationale", true, List.of());
        var response = new CodeReviewAiResponse(
                List.of(new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.APPROVED,
                        List.of(), List.of(suggestion), List.of(), List.of(), ReviewConfidence.HIGH, true)),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar sugestão contendo diff")
    void deveRejeitarSugestaoComDiff() {
        var suggestion = new ReviewSuggestion("tests/login.spec.ts", "--- a\n+++ b\n@@ -1 +1 @@", ReviewSeverity.LOW, "rationale", true, List.of());
        var response = new CodeReviewAiResponse(
                List.of(new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.APPROVED,
                        List.of(), List.of(suggestion), List.of(), List.of(), ReviewConfidence.HIGH, true)),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto em issue")
    void deveRejeitarPathAbsolutoEmIssue() {
        var issue = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "/etc/passwd", null, "msg", null, "rec", false);
        var response = new CodeReviewAiResponse(List.of(), List.of(issue), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar path absoluto em sugestão")
    void deveRejeitarPathAbsolutoEmSugestao() {
        var suggestion = new ReviewSuggestion("/etc/passwd", "desc", ReviewSeverity.LOW, "rationale", false, List.of());
        var response = new CodeReviewAiResponse(List.of(), List.of(), List.of(suggestion), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar segredo aparente no message da issue")
    void deveRejeitarSegredoEmIssue() {
        var issue = new ReviewIssue("CODE", ReviewCategory.SECURITY, ReviewSeverity.LOW,
                "tests/login.spec.ts", null, "password: 'abc12345'", null, "rec", false);
        var response = new CodeReviewAiResponse(List.of(), List.of(issue), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar elemento nulo em files")
    void deveRejeitarElementoNuloEmFiles() {
        List<CodeReviewAiResponse.AiFileReview> files = new java.util.ArrayList<>();
        files.add(null);
        var response = new CodeReviewAiResponse(files, List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve descartar issue nula sem derrubar a revisão")
    void deveDescartarIssueNula() {
        List<ReviewIssue> issues = new java.util.ArrayList<>();
        issues.add(null);
        var response = new CodeReviewAiResponse(List.of(), issues, List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewConfidence.HIGH, false, true);

        var saneada = validate(response, "tests/login.spec.ts");

        assertThat(saneada.globalIssues()).as("o achado inutilizável some, a revisão sobrevive").isEmpty();
    }

    @Test
    @DisplayName("Deve retornar a mesma instância recebida quando válida")
    void deveRetornarMesmaInstancia() {
        var response = CodeReviewTestData.approvedResponse("tests/login.spec.ts");
        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Não deve modificar o resultado recebido")
    void deveNaoModificarResultado() {
        var response = CodeReviewTestData.approvedResponse("tests/login.spec.ts");
        validate(response, "tests/login.spec.ts");
        assertThat(response.status()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(response.files()).hasSize(1);
    }

    @Test
    @DisplayName("Deve validar review Playwright válido")
    void deveValidarReviewPlaywright() {
        var response = CodeReviewTestData.approvedResponse("tests/login.spec.ts");
        assertThat(validate(response, "tests/login.spec.ts")).isNotNull();
    }

    @Test
    @DisplayName("Deve validar warning não bloqueante sem lançar exceção")
    void deveValidarWarningNaoBloqueante() {
        var response = new CodeReviewAiResponse(
                List.of(new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.APPROVED,
                        List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true)),
                List.of(), List.of(), List.of(), List.of(),
                List.of(new ReviewWarning("LOW_CONFIDENCE", "confiança baixa", false)),
                ReviewStatus.APPROVED, ReviewConfidence.LOW, false, true);

        assertThat(validate(response, "tests/login.spec.ts")).isSameAs(response);
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarRespostaNula() {
        assertThatThrownBy(() -> validator.validate(null, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.completeKnowledge(),
                CodeReviewTestData.plan(), CodeReviewTestData.generation(executionId)))
                .isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status ausente")
    void deveRejeitarStatusAusente() {
        var response = new CodeReviewAiResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, ReviewConfidence.HIGH, false, true);
        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath ausente em file review")
    void deveRejeitarRelativePathAusente() {
        var response = new CodeReviewAiResponse(
                List.of(new CodeReviewAiResponse.AiFileReview(null, FileReviewStatus.APPROVED,
                        List.of(), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true)),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);

        assertThatThrownBy(() -> validate(response, "tests/login.spec.ts")).isInstanceOf(CodeReviewValidationException.class);
    }

    @Test
    @DisplayName("Deve descartar issue com relativePath divergente, mantendo o resto")
    void deveDescartarIssueComRelativePathDivergente() {
        var issue = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "tests/outro.spec.ts", null, "msg", null, "rec", false);
        var response = CodeReviewTestData.responseWithIssue("tests/login.spec.ts", issue,
                FileReviewStatus.APPROVED_WITH_WARNINGS, ReviewStatus.APPROVED_WITH_WARNINGS, false, true);

        var saneada = validate(response, "tests/login.spec.ts");

        assertThat(saneada.files().get(0).issues())
                .as("achado que aponta outro arquivo é descartado, não derruba a revisão")
                .isEmpty();
    }

    // --- helper ---

    private CodeReviewAiResponse validate(CodeReviewAiResponse response, String... generatedPaths) {
        return validator.validate(response, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData.completeKnowledge(),
                CodeReviewTestData.plan(generatedPaths), CodeReviewTestData.generation(executionId, generatedPaths));
    }
}
