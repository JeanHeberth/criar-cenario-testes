package com.br.criarcenariotestes.business.autoqa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lançada quando o caminho do projeto de automação é inválido,
 * inexistente, sem permissão ou representa um diretório proibido.
 */
public class InvalidProjectPathException extends ResponseStatusException {

    public InvalidProjectPathException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }

    public static InvalidProjectPathException empty() {
        return new InvalidProjectPathException("O caminho do projeto não pode ser vazio");
    }

    public static InvalidProjectPathException notFound(String path) {
        return new InvalidProjectPathException(
                "O caminho não existe ou não é acessível: " + path
        );
    }

    public static InvalidProjectPathException notDirectory(String path) {
        return new InvalidProjectPathException(
                "O caminho informado não é um diretório: " + path
        );
    }

    public static InvalidProjectPathException noReadPermission(String path) {
        return new InvalidProjectPathException(
                "Sem permissão de leitura no diretório: " + path
        );
    }

    public static InvalidProjectPathException noWritePermission(String path) {
        return new InvalidProjectPathException(
                "Sem permissão de escrita no diretório (necessária para aplicar arquivos): " + path
        );
    }

    public static InvalidProjectPathException forbiddenRoot(String path) {
        return new InvalidProjectPathException(
                "O caminho informado é um diretório raiz ou pessoal protegido e não pode ser utilizado: " + path
        );
    }

    public static InvalidProjectPathException pathTraversal() {
        return new InvalidProjectPathException(
                "Tentativa de path traversal detectada. Utilize um caminho absoluto válido"
        );
    }

    public static InvalidProjectPathException outsideAllowedRoots(String path) {
        return new InvalidProjectPathException(
                "O caminho está fora das raízes permitidas configuradas: " + path
        );
    }
}
