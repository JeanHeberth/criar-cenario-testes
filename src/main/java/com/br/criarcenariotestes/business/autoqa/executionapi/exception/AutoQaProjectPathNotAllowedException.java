package com.br.criarcenariotestes.business.autoqa.executionapi.exception;

/**
 * Mapeada para HTTP 403 — projectPath resolvido não está dentro de nenhuma
 * raiz de auto-qa.allowed-roots (política fail-closed de
 * ProjectPathSecurityValidator). Mensagem nunca inclui o path recebido nem
 * as roots configuradas.
 */
public class AutoQaProjectPathNotAllowedException extends AutoQaExecutionApiException {
    public AutoQaProjectPathNotAllowedException(String message) {
        super(message);
    }
}
