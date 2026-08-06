package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.*;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator.AutoQaExecutionOrchestrator;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.service.AutoQaExecutionQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * Expõe o ciclo de vida de uma execução Auto QA. Todo estado sensível
 * (projectPath, prompt, stacktrace) fica só no lado do domínio/persistência
 * — este controller nunca vê nem repassa nada disso, sempre trabalha com
 * AutoQaExecutionResponse já sanitizado.
 */
@RestController
@RequestMapping("/api/auto-qa/executions")
public class AutoQaExecutionController {

    private final AutoQaExecutionOrchestrator orchestrator;
    private final AutoQaExecutionQueryService queryService;
    private final AutoQaExecutionResponseMapper mapper;

    public AutoQaExecutionController(AutoQaExecutionOrchestrator orchestrator,
                                      AutoQaExecutionQueryService queryService,
                                      AutoQaExecutionResponseMapper mapper) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
        this.queryService = Objects.requireNonNull(queryService);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @PostMapping
    public ResponseEntity<AutoQaExecutionResponse> create(@Valid @RequestBody AutoQaCreateExecutionRequest request) {
        AutoQaExecutionResponse response = mapper.toResponse(orchestrator.create(request.scenario(), request.projectPath()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/auto-qa/executions/" + response.executionId()))
                .body(response);
    }

    @GetMapping
    public AutoQaExecutionListResponse list(Pageable pageable) {
        return queryService.list(pageable);
    }

    @GetMapping("/{executionId}")
    public AutoQaExecutionResponse get(@PathVariable UUID executionId) {
        return queryService.get(executionId);
    }

    @PostMapping("/{executionId}/start")
    public ResponseEntity<AutoQaExecutionResponse> start(@PathVariable UUID executionId) {
        return accepted(orchestrator.start(executionId));
    }

    @PostMapping("/{executionId}/continue")
    public ResponseEntity<AutoQaExecutionResponse> continueExecution(@PathVariable UUID executionId) {
        return accepted(orchestrator.continueExecution(executionId));
    }

    @PostMapping("/{executionId}/generate")
    public ResponseEntity<AutoQaExecutionResponse> generate(@PathVariable UUID executionId) {
        return accepted(orchestrator.generate(executionId));
    }

    @PostMapping("/{executionId}/apply-approval")
    public ResponseEntity<AutoQaExecutionResponse> applyApproval(@PathVariable UUID executionId,
                                                                   @Valid @RequestBody AutoQaApplyApprovalRequest request) {
        AutoQaExecutionDocument document = orchestrator.registerApplyApproval(executionId, request.toDomain());
        return ResponseEntity.ok(mapper.toResponse(document));
    }

    @PostMapping("/{executionId}/apply")
    public ResponseEntity<AutoQaExecutionResponse> apply(@PathVariable UUID executionId) {
        return accepted(orchestrator.apply(executionId));
    }

    @PostMapping("/{executionId}/execution-approval")
    public ResponseEntity<AutoQaExecutionResponse> executionApproval(@PathVariable UUID executionId,
                                                                       @Valid @RequestBody AutoQaExecutionApprovalRequest request) {
        AutoQaExecutionDocument document = orchestrator.registerExecutionApproval(executionId, request.toDomain());
        return ResponseEntity.ok(mapper.toResponse(document));
    }

    @PostMapping("/{executionId}/execute")
    public ResponseEntity<AutoQaExecutionResponse> execute(@PathVariable UUID executionId) {
        return accepted(orchestrator.execute(executionId));
    }

    @PostMapping("/{executionId}/cancel")
    public ResponseEntity<AutoQaExecutionResponse> cancel(@PathVariable UUID executionId,
                                                            @RequestBody(required = false) AutoQaCancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(mapper.toResponse(orchestrator.cancel(executionId, reason)));
    }

    private ResponseEntity<AutoQaExecutionResponse> accepted(AutoQaExecutionDocument document) {
        return ResponseEntity.accepted().body(mapper.toResponse(document));
    }
}
