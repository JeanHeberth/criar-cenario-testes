package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaExecutionRepository - Testes de Contrato (mockado)")
class AutoQaExecutionRepositoryTest {

    private final AutoQaExecutionRepository repository = mock(AutoQaExecutionRepository.class);

    @Test
    @DisplayName("Deve estender MongoRepository<AutoQaExecutionDocument, String>")
    void deveEstenderMongoRepository() {
        assertThat(MongoRepository.class).isAssignableFrom(AutoQaExecutionRepository.class);
    }

    @Test
    @DisplayName("findByExecutionId deve retornar Optional presente quando encontrado")
    void findByExecutionIdDeveRetornarOptionalPresente() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "c", "/p", Instant.now());
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.of(document));

        Optional<AutoQaExecutionDocument> result = repository.findByExecutionId(executionId);

        assertThat(result).isPresent();
        assertThat(result.get().getExecutionId()).isEqualTo(executionId);
    }

    @Test
    @DisplayName("findByExecutionId deve retornar Optional vazio quando não encontrado")
    void findByExecutionIdDeveRetornarOptionalVazio() {
        UUID executionId = UUID.randomUUID();
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.empty());

        assertThat(repository.findByExecutionId(executionId)).isEmpty();
    }

    @Test
    @DisplayName("findAll(Pageable) deve suportar paginação (herdado de MongoRepository)")
    void findAllPageableDeveSuportarPaginacao() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/p", Instant.now());
        Page<AutoQaExecutionDocument> page = new PageImpl<>(List.of(document));
        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        Page<AutoQaExecutionDocument> result = repository.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("save deve persistir o documento (herdado de MongoRepository)")
    void saveDevePersistirDocumento() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/p", Instant.now());
        when(repository.save(document)).thenReturn(document);

        AutoQaExecutionDocument saved = repository.save(document);

        assertThat(saved).isSameAs(document);
    }
}
