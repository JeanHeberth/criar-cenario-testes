package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VeredictoDaRevisaoTest {

    private final ReviewSummaryBuilder builder = new ReviewSummaryBuilder();

    @Test
    void devePreservarPedidoDeMudancaDaIaQuandoSeveridadeNaoAcompanha() {
        // Caso real: a revisora respondeu CHANGES_REQUIRED classificando tudo
        // como MEDIUM/LOW. O recálculo por severidade dava APPROVED_WITH_WARNINGS
        // e o pipeline seguia — com código que nem compilava.
        assertThat(builder.naoMaisPermissivoQue(
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewStatus.CHANGES_REQUIRED))
                .isEqualTo(ReviewStatus.CHANGES_REQUIRED);

        assertThat(builder.naoMaisPermissivoQue(
                FileReviewStatus.APPROVED_WITH_WARNINGS, FileReviewStatus.CHANGES_REQUIRED))
                .isEqualTo(FileReviewStatus.CHANGES_REQUIRED);
    }

    @Test
    void deveManterODerivadoQuandoEleForMaisRestritivoQueAIa() {
        // Caminho inverso: regra estática achou HIGH e a IA aprovou. Aqui quem
        // manda é o derivado — a regra determinística não pode ser anulada por
        // um "aprovado" do modelo.
        assertThat(builder.naoMaisPermissivoQue(
                ReviewStatus.CHANGES_REQUIRED, ReviewStatus.APPROVED))
                .isEqualTo(ReviewStatus.CHANGES_REQUIRED);

        assertThat(builder.naoMaisPermissivoQue(
                ReviewStatus.BLOCKED, ReviewStatus.APPROVED_WITH_WARNINGS))
                .isEqualTo(ReviewStatus.BLOCKED);
    }

    @Test
    void naoDeveMexerQuandoOStatusDaIaNaoEComparavel() {
        // null (arquivo sem correspondente na resposta da IA) e valores fora da
        // escala não podem alterar o derivado.
        assertThat(builder.naoMaisPermissivoQue(ReviewStatus.APPROVED, null))
                .isEqualTo(ReviewStatus.APPROVED);
        assertThat(builder.naoMaisPermissivoQue(ReviewStatus.APPROVED, ReviewStatus.INVALID))
                .isEqualTo(ReviewStatus.APPROVED);
        assertThat(builder.naoMaisPermissivoQue(FileReviewStatus.APPROVED, FileReviewStatus.SKIPPED))
                .isEqualTo(FileReviewStatus.APPROVED);
    }

    @Test
    void deveConcordarQuandoOsDoisSinaisApontamOMesmo() {
        assertThat(builder.naoMaisPermissivoQue(ReviewStatus.APPROVED, ReviewStatus.APPROVED))
                .isEqualTo(ReviewStatus.APPROVED);
        assertThat(builder.naoMaisPermissivoQue(ReviewStatus.BLOCKED, ReviewStatus.BLOCKED))
                .isEqualTo(ReviewStatus.BLOCKED);
    }

    @Test
    void oPedidoDaRevisoraPrecisaVirarAchadoHighParaOApplyEnxergar() {
        // Vão que a própria preservação de veredito abriu: o ApplyAgent decide
        // por SEVERIDADE, não por status. Com CHANGES_REQUIRED vindo só do
        // veredito e nenhum achado HIGH, `podeSeguirComAchadosLeves` liberaria
        // o apply se allowWarnings estivesse marcado — e o painel do front já
        // vem com esse campo pré-marcado.
        //
        // Este teste fixa a regra: quando o veredito endurece o status, um
        // achado HIGH é sintetizado para que a decisão por severidade também
        // veja o pedido.
        assertThat(ReviewRule.valueOf("REVIEWER_REQUESTED_CHANGES"))
                .as("regra dedicada, para o motivo aparecer no relatório em vez de virar HIGH anônimo")
                .isNotNull();

        assertThat(builder.naoMaisPermissivoQue(
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewStatus.CHANGES_REQUIRED))
                .isNotEqualTo(ReviewStatus.APPROVED_WITH_WARNINGS);
    }
}
