package com.br.criarcenariotestes.business.validation;

/**
 * FASE15-BUG-003A: sinaliza que a geração/revisão/persistência de cenários
 * falhou uma validação estrutural determinística (não é uma falha de
 * infraestrutura/rede/provider). Este tipo NUNCA deve ser mascarado por um
 * fallback local — precisa propagar como falha real, para o produto nunca
 * retornar "falso sucesso" quando o conteúdo persistível é inválido.
 */
public class ValidacaoEstruturalException extends RuntimeException {

    public ValidacaoEstruturalException(String message) {
        super(message);
    }
}
