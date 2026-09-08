package com.br.criarcenariotestes.business.autoqa.review;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec: a revisão pede teto de saída próprio, como a geração.
 *
 * <p>Observado em produção: revisar 5 arquivos produziu 29.458 caracteres e
 * truncou em MAX_TOKENS com o teto de 8000. A resposta veio cortada no meio do
 * JSON e a revisão falhou nos dois provedores — com as duas chamadas já pagas.
 *
 * <p>A saída do review cresce com o número de arquivos revisados: um teto
 * dimensionado para uma resposta pequena reprova justamente as execuções
 * maiores, que são as que mais precisam de revisão.
 */
class LimiteDeSaidaDoReviewTest {

    @Test
    void deveDeclararTetoDeSaidaCompativelComRevisaoDeVariosArquivos() throws Exception {
        Field campo = CodeReviewService.class.getDeclaredField("MAX_TOKENS_REVIEW");
        campo.setAccessible(true);
        int teto = (int) campo.get(null);

        assertThat(teto)
                .as("8000 truncou uma revisão de 5 arquivos (29.458 caracteres)")
                .isGreaterThan(8_000);
    }
}
