package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AutoQaExecutionSnapshotRepository - Testes de Contrato (mockado)")
class AutoQaExecutionSnapshotRepositoryTest {

    private final AutoQaExecutionSnapshotRepository repository = mock(AutoQaExecutionSnapshotRepository.class);

    @Test
    @DisplayName("Deve estender MongoRepository<AutoQaExecutionSnapshot, String>")
    void deveEstenderMongoRepository() {
        assertThat(MongoRepository.class).isAssignableFrom(AutoQaExecutionSnapshotRepository.class);
    }

    @Test
    @DisplayName("findByExecutionId deve retornar Optional presente quando encontrado")
    void findByExecutionIdDeveRetornarOptionalPresente() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(executionId, Instant.now());
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.of(snapshot));

        assertThat(repository.findByExecutionId(executionId)).isPresent();
    }

    @Test
    @DisplayName("findByExecutionId deve retornar Optional vazio quando não encontrado")
    void findByExecutionIdDeveRetornarOptionalVazio() {
        UUID executionId = UUID.randomUUID();
        when(repository.findByExecutionId(executionId)).thenReturn(Optional.empty());

        assertThat(repository.findByExecutionId(executionId)).isEmpty();
    }
}
