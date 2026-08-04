package com.br.criarcenariotestes.business.autoqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lançada quando ocorre falha durante a análise do projeto de automação.
 */
public class ProjectAnalysisException extends ResponseStatusException {

    public ProjectAnalysisException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }

    public ProjectAnalysisException(String reason, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
    }

    public static ProjectAnalysisException scanFailed(String path, Throwable cause) {
        return new ProjectAnalysisException(
                "Falha ao escanear o projeto em: " + path, cause
        );
    }

    public static ProjectAnalysisException tooManyFiles(int count, int limit) {
        return new ProjectAnalysisException(
                "O projeto excede o limite de arquivos permitidos: " + count + " / " + limit
        );
    }

    public static ProjectAnalysisException fileTooLarge(String relativePath, long sizeKb, long limitKb) {
        return new ProjectAnalysisException(
                "Arquivo excede o limite de tamanho (" + sizeKb + " KB / " + limitKb + " KB): " + relativePath
        );
    }

    public static ProjectAnalysisException totalContentTooLarge(long totalKb, long limitKb) {
        return new ProjectAnalysisException(
                "Volume total de conteúdo excede o limite (" + totalKb + " KB / " + limitKb + " KB)"
        );
    }
}
