package com.br.criarcenariotestes.business.tracker;

/**
 * Lançada quando veio texto no campo de referência da tarefa, mas ele não é
 * reconhecível como URL ou chave de nenhum rastreador suportado. Falhar aqui
 * é deliberado: seguir em silêncio publicaria os casos de teste sem o vínculo
 * que o usuário pediu, e ninguém perceberia até auditar o board.
 */
public class ReferenciaTarefaInvalidaException extends RuntimeException {

    public ReferenciaTarefaInvalidaException(String message) {
        super(message);
    }
}
