package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Deriva deterministicamente o status (por arquivo e global), humanReviewRequired e valid
 * a partir do CONJUNTO FINAL de issues (estáticas + IA já mescladas pelo CodeReviewService).
 * O "status" auto-reportado pela IA nunca é usado como fonte da verdade aqui.
 */
@Component
public class ReviewSummaryBuilder {

    public FileReviewStatus deriveFileStatus(List<ReviewIssue> issues) {
        Objects.requireNonNull(issues, "issues must not be null");
        if (hasSeverity(issues, ReviewSeverity.CRITICAL)) return FileReviewStatus.BLOCKED;
        if (hasSeverity(issues, ReviewSeverity.HIGH)) return FileReviewStatus.CHANGES_REQUIRED;
        if (hasSeverity(issues, ReviewSeverity.MEDIUM) || hasSeverity(issues, ReviewSeverity.LOW)) return FileReviewStatus.APPROVED_WITH_WARNINGS;
        return FileReviewStatus.APPROVED;
    }

    public ReviewStatus deriveGlobalStatus(List<FileReviewResult> files, List<ReviewIssue> globalIssues) {
        Objects.requireNonNull(files, "files must not be null");
        Objects.requireNonNull(globalIssues, "globalIssues must not be null");
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.CRITICAL)) return ReviewStatus.BLOCKED;
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.HIGH)) return ReviewStatus.CHANGES_REQUIRED;
        if (anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.MEDIUM)
                || anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.LOW)) {
            return ReviewStatus.APPROVED_WITH_WARNINGS;
        }
        return ReviewStatus.APPROVED;
    }

    public boolean deriveHumanReviewRequired(List<FileReviewResult> files, List<ReviewIssue> globalIssues) {
        return anyFileOrGlobalHasSeverity(files, globalIssues, ReviewSeverity.CRITICAL);
    }

    public boolean deriveValid(ReviewStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return status != ReviewStatus.INVALID;
    }

    public String buildAgentSummary(CodeReviewResult result) {
        Objects.requireNonNull(result, "result must not be null");
        long totalIssues = result.globalIssues().size()
                + result.files().stream().mapToLong(f -> f.issues().size()).sum();
        return "Revisão concluída: " + result.status() + " / " + result.files().size() + " arquivos / " + totalIssues + " issues";
    }

    private boolean anyFileOrGlobalHasSeverity(List<FileReviewResult> files, List<ReviewIssue> globalIssues, ReviewSeverity severity) {
        return hasSeverity(globalIssues, severity)
                || files.stream().anyMatch(f -> hasSeverity(f.issues(), severity));
    }

    private boolean hasSeverity(List<ReviewIssue> issues, ReviewSeverity severity) {
        return issues.stream().anyMatch(i -> i != null && i.severity() == severity);
    }

    /**
     * Ordem de permissividade, do mais frouxo ao mais rígido. INVALID fica de
     * fora porque é tratado antes, como resposta inutilizável.
     */
    private static final List<ReviewStatus> RIGIDEZ_GLOBAL = List.of(
            ReviewStatus.APPROVED,
            ReviewStatus.APPROVED_WITH_WARNINGS,
            ReviewStatus.CHANGES_REQUIRED,
            ReviewStatus.BLOCKED);

    private static final List<FileReviewStatus> RIGIDEZ_ARQUIVO = List.of(
            FileReviewStatus.APPROVED,
            FileReviewStatus.APPROVED_WITH_WARNINGS,
            FileReviewStatus.CHANGES_REQUIRED,
            FileReviewStatus.BLOCKED);

    /**
     * O status final nunca é MAIS PERMISSIVO que o veredito declarado pela IA.
     *
     * <p>Observado em produção: a IA revisora respondeu CHANGES_REQUIRED, mas
     * classificou seus achados como MEDIUM/LOW. Como o status era recalculado
     * só pelas severidades, o veredito virava APPROVED_WITH_WARNINGS e o
     * pipeline seguia — a revisora pediu para parar e o sistema entendeu
     * "siga". Código que sequer compilava passou por aí.
     *
     * <p>Entre dois sinais discordantes, o mais restritivo é o que preserva a
     * intenção: severidade mal classificada é falha de calibragem do modelo,
     * não permissão para aplicar. O caminho inverso continua valendo — se as
     * regras estáticas acham HIGH e a IA disse APPROVED, prevalece o derivado.
     */
    public ReviewStatus naoMaisPermissivoQue(ReviewStatus derivado, ReviewStatus declaradoPelaIa) {
        // List.of(...).indexOf(null) lança NPE. O null é real: arquivo sem
        // correspondente na resposta da IA.
        if (declaradoPelaIa == null) {
            return derivado;
        }
        int posDerivado = RIGIDEZ_GLOBAL.indexOf(derivado);
        int posDeclarado = RIGIDEZ_GLOBAL.indexOf(declaradoPelaIa);
        if (posDerivado < 0 || posDeclarado < 0) {
            return derivado;
        }
        return posDeclarado > posDerivado ? declaradoPelaIa : derivado;
    }

    public FileReviewStatus naoMaisPermissivoQue(FileReviewStatus derivado, FileReviewStatus declaradoPelaIa) {
        if (declaradoPelaIa == null) {
            return derivado;
        }
        int posDerivado = RIGIDEZ_ARQUIVO.indexOf(derivado);
        int posDeclarado = RIGIDEZ_ARQUIVO.indexOf(declaradoPelaIa);
        if (posDerivado < 0 || posDeclarado < 0) {
            return derivado;
        }
        return posDeclarado > posDerivado ? declaradoPelaIa : derivado;
    }
}
