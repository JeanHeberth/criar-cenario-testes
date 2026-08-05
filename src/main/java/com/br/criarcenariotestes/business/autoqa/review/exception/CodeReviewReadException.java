package com.br.criarcenariotestes.business.autoqa.review.exception;

/**
 * Falha técnica de leitura dos artefatos gerados (arquivo inexistente, permissão,
 * I/O, encoding, caminho inseguro, symlink, saída da raiz). Nunca usada para
 * divergência de hash, que é tratada como achado estático CRITICAL (ver
 * StaticReviewRuleEngine / ReviewRule.CONTENT_INTEGRITY_MISMATCH).
 */
public class CodeReviewReadException extends CodeReviewException {
    public CodeReviewReadException(String message) { super(message); }
    public CodeReviewReadException(String message, Throwable cause) { super(message, cause); }
}
