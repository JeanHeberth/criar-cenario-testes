package com.br.criarcenariotestes.business.autoqa.navegacao;

import java.util.List;

/**
 * Um nível da árvore de pastas navegáveis, para o seletor da tela do Auto QA.
 *
 * Nunca inclui arquivos: o campo que isto alimenta é o diretório do projeto, e
 * listar arquivos só ampliaria o que a API revela sobre o servidor sem
 * nenhum ganho para o usuário.
 */
public record NavegacaoPastasResponse(
        /** Caminho sendo listado; null quando são as raízes autorizadas. */
        String caminhoAtual,

        /**
         * Para onde o botão "voltar" leva, ou null quando já se está numa raiz
         * autorizada — subir além dela sairia da área permitida.
         */
        String caminhoPai,

        /** True quando o caminho atual pode ser escolhido como projeto. */
        boolean selecionavel,

        List<PastaNavegavel> pastas
) {
    public record PastaNavegavel(String nome, String caminho) {
    }
}
