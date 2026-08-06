package com.br.criarcenariotestes.business.autoqa.apply.exception;

/**
 * Falha estrutural que impede a aplicação de sequer começar: pré-condições de
 * fase (status proibidos) ou aprovação ausente/negada. Nunca é usada para
 * conflitos de conteúdo (manifest, hash, path), que viram ApplyConflict
 * dentro de um ApplyResult com status BLOCKED.
 */
public class ApplyValidationException extends ApplyException {
    public ApplyValidationException(String message) { super(message); }
}
