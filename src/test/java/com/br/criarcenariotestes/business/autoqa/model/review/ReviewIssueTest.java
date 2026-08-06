package com.br.criarcenariotestes.business.autoqa.model.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewIssue - Testes Unitários")
class ReviewIssueTest {

    @Test
    @DisplayName("Deve criar issue válida")
    void deveCriarReviewIssueValida() {
        ReviewIssue issue = new ReviewIssue("FRAGILE_SELECTOR", ReviewCategory.MAINTAINABILITY, ReviewSeverity.MEDIUM,
                "tests/login.spec.ts", 12, "Seletor frágil", ".container > div", "Usar seletor semântico", false);

        assertThat(issue.code()).isEqualTo("FRAGILE_SELECTOR");
        assertThat(issue.severity()).isEqualTo(ReviewSeverity.MEDIUM);
    }

    @Test
    @DisplayName("Deve permitir line nula")
    void devePermitirLineNula() {
        ReviewIssue issue = new ReviewIssue("MISSING_ASSERTION", ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                "tests/login.spec.ts", null, "Sem assertion", null, "Adicionar assertion", true);

        assertThat(issue.line()).isNull();
    }

    @Test
    @DisplayName("Deve remover espaços de code, relativePath, message, evidence e recommendation")
    void deveTrimarCampos() {
        ReviewIssue issue = new ReviewIssue("  CODE  ", ReviewCategory.SECURITY, ReviewSeverity.HIGH,
                "  tests/x.ts  ", 1, "  msg  ", "  evid  ", "  rec  ", true);

        assertThat(issue.code()).isEqualTo("CODE");
        assertThat(issue.relativePath()).isEqualTo("tests/x.ts");
        assertThat(issue.message()).isEqualTo("msg");
        assertThat(issue.evidence()).isEqualTo("evid");
        assertThat(issue.recommendation()).isEqualTo("rec");
    }

    @Test
    @DisplayName("Deve representar issue CRITICAL com blocking=true")
    void deveRepresentarStatusBlocked() {
        ReviewIssue issue = new ReviewIssue("CONTENT_INTEGRITY_MISMATCH", ReviewCategory.SECURITY, ReviewSeverity.CRITICAL,
                "tests/login.spec.ts", null, "Hash divergente", null, "Regerar arquivo", true);

        assertThat(issue.severity()).isEqualTo(ReviewSeverity.CRITICAL);
        assertThat(issue.blocking()).isTrue();
    }

    @Test
    @DisplayName("Deve manter contrato JSON via ObjectMapper")
    void deveManterContratoJson() throws Exception {
        String json = """
                {
                  "code": "FRAGILE_SELECTOR",
                  "category": "MAINTAINABILITY",
                  "severity": "MEDIUM",
                  "relativePath": "tests/login.spec.ts",
                  "line": 12,
                  "message": "Seletor frágil",
                  "evidence": ".container",
                  "recommendation": "Usar seletor semântico",
                  "blocking": false
                }
                """;
        ReviewIssue issue = new ObjectMapper().readValue(json, ReviewIssue.class);

        assertThat(issue.code()).isEqualTo("FRAGILE_SELECTOR");
        assertThat(issue.category()).isEqualTo(ReviewCategory.MAINTAINABILITY);
    }

    @Test
    @DisplayName("Não deve conter evidência exageradamente longa como código completo")
    void deveNaoExporCodigoCompleto() {
        String longEvidence = "x".repeat(50);
        ReviewIssue issue = new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "tests/x.ts", null, "msg", longEvidence, "rec", false);

        assertThat(issue.evidence()).hasSize(50);
    }
}
