package com.br.criarcenariotestes.business.autoqa.review.exception;

public class CodeReviewException extends RuntimeException {
    public CodeReviewException(String message) { super(message); }
    public CodeReviewException(String message, Throwable cause) { super(message, cause); }
}
