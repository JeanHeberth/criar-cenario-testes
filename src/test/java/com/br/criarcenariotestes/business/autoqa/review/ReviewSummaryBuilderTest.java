package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewSummaryBuilder - Testes Unitários")
class ReviewSummaryBuilderTest {

    private ReviewSummaryBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ReviewSummaryBuilder();
    }

    @Test
    @DisplayName("Deve derivar APPROVED quando não há issues")
    void deveDerivarApproved() {
        assertThat(builder.deriveFileStatus(List.of())).isEqualTo(FileReviewStatus.APPROVED);
        assertThat(builder.deriveGlobalStatus(List.of(), List.of())).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve derivar APPROVED_WITH_WARNINGS com issue LOW/MEDIUM")
    void deveDerivarApprovedWithWarnings() {
        ReviewIssue low = issue(ReviewSeverity.LOW);
        assertThat(builder.deriveFileStatus(List.of(low))).isEqualTo(FileReviewStatus.APPROVED_WITH_WARNINGS);

        ReviewIssue medium = issue(ReviewSeverity.MEDIUM);
        assertThat(builder.deriveGlobalStatus(List.of(), List.of(medium))).isEqualTo(ReviewStatus.APPROVED_WITH_WARNINGS);
    }

    @Test
    @DisplayName("Deve derivar CHANGES_REQUIRED com issue HIGH")
    void deveDerivarChangesRequired() {
        ReviewIssue high = issue(ReviewSeverity.HIGH);
        assertThat(builder.deriveFileStatus(List.of(high))).isEqualTo(FileReviewStatus.CHANGES_REQUIRED);
        assertThat(builder.deriveGlobalStatus(List.of(), List.of(high))).isEqualTo(ReviewStatus.CHANGES_REQUIRED);
    }

    @Test
    @DisplayName("Deve derivar BLOCKED com issue CRITICAL")
    void deveDerivarBlocked() {
        ReviewIssue critical = issue(ReviewSeverity.CRITICAL);
        assertThat(builder.deriveFileStatus(List.of(critical))).isEqualTo(FileReviewStatus.BLOCKED);
        assertThat(builder.deriveGlobalStatus(List.of(), List.of(critical))).isEqualTo(ReviewStatus.BLOCKED);
    }

    @Test
    @DisplayName("CRITICAL deve prevalecer sobre HIGH/MEDIUM/LOW simultâneos")
    void criticalDevePrevalecerSobreOutrasSeveridades() {
        List<ReviewIssue> issues = List.of(issue(ReviewSeverity.LOW), issue(ReviewSeverity.MEDIUM),
                issue(ReviewSeverity.HIGH), issue(ReviewSeverity.CRITICAL));
        assertThat(builder.deriveFileStatus(issues)).isEqualTo(FileReviewStatus.BLOCKED);
    }

    @Test
    @DisplayName("Deve considerar issues de arquivos ao derivar status global")
    void deveConsiderarIssuesDeArquivosNoStatusGlobal() {
        FileReviewResult file = new FileReviewResult("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.BLOCKED, List.of(issue(ReviewSeverity.CRITICAL)), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true);

        assertThat(builder.deriveGlobalStatus(List.of(file), List.of())).isEqualTo(ReviewStatus.BLOCKED);
    }

    @Test
    @DisplayName("Deve derivar humanReviewRequired=true quando há CRITICAL")
    void deveDerivarHumanReviewRequiredComCritical() {
        assertThat(builder.deriveHumanReviewRequired(List.of(), List.of(issue(ReviewSeverity.CRITICAL)))).isTrue();
    }

    @Test
    @DisplayName("Deve derivar humanReviewRequired=false sem CRITICAL")
    void deveDerivarHumanReviewRequiredFalseSemCritical() {
        assertThat(builder.deriveHumanReviewRequired(List.of(), List.of(issue(ReviewSeverity.HIGH)))).isFalse();
    }

    @Test
    @DisplayName("Deve derivar valid=true para todos os status exceto INVALID")
    void deveDerivarValidTrueExcetoInvalid() {
        assertThat(builder.deriveValid(ReviewStatus.APPROVED)).isTrue();
        assertThat(builder.deriveValid(ReviewStatus.APPROVED_WITH_WARNINGS)).isTrue();
        assertThat(builder.deriveValid(ReviewStatus.CHANGES_REQUIRED)).isTrue();
        assertThat(builder.deriveValid(ReviewStatus.BLOCKED)).isTrue();
        assertThat(builder.deriveValid(ReviewStatus.INVALID)).isFalse();
    }

    @Test
    @DisplayName("Deve construir resumo técnico para o agente")
    void deveConstruirResumoTecnico() {
        UUID executionId = UUID.randomUUID();
        FileReviewResult file = new FileReviewResult("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                FileReviewStatus.APPROVED_WITH_WARNINGS, List.of(issue(ReviewSeverity.LOW)), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true);
        CodeReviewResult result = new CodeReviewResult(executionId, List.of(file), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewConfidence.HIGH, false, true);

        String summary = builder.buildAgentSummary(result);

        assertThat(summary).contains("APPROVED_WITH_WARNINGS").contains("1 arquivos").contains("1 issues");
    }

    private ReviewIssue issue(ReviewSeverity severity) {
        return new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, severity, "tests/login.spec.ts", null, "msg", null, "rec", severity == ReviewSeverity.CRITICAL);
    }
}
