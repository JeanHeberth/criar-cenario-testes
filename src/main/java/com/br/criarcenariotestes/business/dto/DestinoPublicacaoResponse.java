package com.br.criarcenariotestes.business.dto;

/**
 * Para onde a publicação vai, resolvido a partir do que o usuário informou —
 * sem gerar nada.
 *
 * Existe para que a tela mostre o destino ANTES de disparar a geração: um
 * erro de referência descoberto depois custa uma rodada inteira de chamadas de
 * IA, e um caso de teste publicado no lugar errado não tem desfazer barato
 * (pasta no Zephyr não aceita DELETE).
 *
 * Também devolve o identificador já normalizado, para o front não ter que
 * reimplementar o parsing de URL que vive no backend.
 */
public record DestinoPublicacaoResponse(
        String provedor,
        String identificador,
        String projectKey,
        String pastaRaiz,
        boolean valido,
        String motivo
) {
    public static DestinoPublicacaoResponse invalido(String motivo) {
        return new DestinoPublicacaoResponse(null, null, null, null, false, motivo);
    }
}
