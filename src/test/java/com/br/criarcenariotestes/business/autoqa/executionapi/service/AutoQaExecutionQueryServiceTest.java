package com.br.criarcenariotestes.business.autoqa.executionapi.service;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaExecutionNotFoundException;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaExecutionQueryService - Testes Unitários")
class AutoQaExecutionQueryServiceTest {

    private AutoQaExecutionRepository repository;
    private AutoQaExecutionResponseMapper mapper;
    private AutoQaExecutionQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AutoQaExecutionRepository.class);
        mapper = new AutoQaExecutionResponseMapper();
        service = new AutoQaExecutionQueryService(repository, mapper);
    }

    @Test
    @DisplayName("get deve retornar o DTO público quando a execução existir")
    void getDeveRetornarDtoQuandoExistir() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "cenário", "/projeto", Instant.now());
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.of(document));

        AutoQaExecutionResponse response = service.get(executionId);

        assertThat(response.executionId()).isEqualTo(executionId);
    }

    @Test
    @DisplayName("get deve lançar AutoQaExecutionNotFoundException quando não existir")
    void getDeveLancarNotFoundQuandoNaoExistir() {
        UUID executionId = UUID.randomUUID();
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(executionId)).isInstanceOf(AutoQaExecutionNotFoundException.class);
    }

    @Test
    @DisplayName("list deve retornar página mapeada")
    void listDeveRetornarPaginaMapeada() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        var page = new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1);
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        AutoQaExecutionListResponse result = service.list(PageRequest.of(0, 20));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> service.get(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar pageable nulo")
    void deveRejeitarPageableNulo() {
        assertThatThrownBy(() -> service.list(null)).isInstanceOf(NullPointerException.class);
    }
}
