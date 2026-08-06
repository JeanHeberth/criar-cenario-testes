package com.br.criarcenariotestes.business.autoqa.executionapi.service;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaExecutionNotFoundException;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/** Camada de leitura: get/list, sempre retornando DTO público já sanitizado. */
@Service
public class AutoQaExecutionQueryService {

    private final AutoQaExecutionRepository repository;
    private final AutoQaExecutionResponseMapper mapper;

    public AutoQaExecutionQueryService(AutoQaExecutionRepository repository, AutoQaExecutionResponseMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    public AutoQaExecutionResponse get(UUID executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        AutoQaExecutionDocument document = repository.findByExecutionId(executionId)
                .orElseThrow(() -> new AutoQaExecutionNotFoundException("Execução não encontrada: " + executionId));
        return mapper.toResponse(document);
    }

    public AutoQaExecutionListResponse list(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        return mapper.toListResponse(repository.findAll(pageable));
    }
}
