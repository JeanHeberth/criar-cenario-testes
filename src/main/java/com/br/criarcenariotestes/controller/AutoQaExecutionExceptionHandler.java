package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.executionapi.exception.*;
import com.br.criarcenariotestes.business.dto.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

/**
 * Handler dedicado, escopado apenas a AutoQaExecutionController — nunca
 * altera o ApiExceptionHandler genérico existente. Qualquer exceção não
 * tratada aqui continua caindo no handler genérico (500 sanitizado). Nunca
 * retorna stacktrace, nome de classe interna ou projectPath absoluto.
 * @Order(HIGHEST_PRECEDENCE) garante que este handler mais específico seja
 * resolvido antes do ApiExceptionHandler genérico (ambos concorreriam em pé
 * de igualdade sem isso, já que nenhum dos dois declarava @Order antes).
 */
@RestControllerAdvice(assignableTypes = AutoQaExecutionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AutoQaExecutionExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request inválido", request);
    }

    @ExceptionHandler(AutoQaExecutionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AutoQaExecutionNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({AutoQaExecutionDisabledException.class, AutoQaSensitiveActionDisabledException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(AutoQaExecutionApiException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler({AutoQaInvalidTransitionException.class, AutoQaExecutionConflictException.class, AutoQaOptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConflict(AutoQaExecutionApiException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(AutoQaSnapshotException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessable(AutoQaSnapshotException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Estado da execução inconsistente para esta operação", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        String path = extractPath(request);
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path));
    }

    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.isBlank()) {
            return "";
        }
        return description.startsWith("uri=") ? description.substring(4) : description;
    }
}
