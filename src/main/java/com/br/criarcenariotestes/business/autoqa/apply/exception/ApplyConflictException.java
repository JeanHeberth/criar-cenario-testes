package com.br.criarcenariotestes.business.autoqa.apply.exception;

/**
 * Sinaliza um conflito bloqueante detectado para um arquivo específico
 * (ex.: violação de segurança de path). É sempre capturada internamente por
 * FileApplicationService e convertida em ApplyConflict — nunca escapa para o
 * ApplyAgent.
 */
public class ApplyConflictException extends ApplyException {

    private final String relativePath;
    private final String conflictType;

    public ApplyConflictException(String relativePath, String conflictType, String message) {
        super(message);
        this.relativePath = relativePath;
        this.conflictType = conflictType;
    }

    public String relativePath() {
        return relativePath;
    }

    public String conflictType() {
        return conflictType;
    }
}
