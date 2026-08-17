package com.br.criarcenariotestes.infrastructure.zephyr;

/**
 * O caminho de pasta pedido não existe no projeto e a criação automática está
 * desabilitada (zephyr.allow-folder-creation=false).
 *
 * É uma exceção própria, e não uma falha genérica, porque o tratamento é
 * diferente: nas demais falhas de pasta (rede, instabilidade) vale publicar o
 * caso solto na raiz e seguir, mas aqui publicar solto seria justamente o
 * lixo que a configuração existe para impedir. Ver ZephyrPublisherAgent.
 */
public class PastaInexistenteException extends RuntimeException {

    public PastaInexistenteException(String message) {
        super(message);
    }
}
