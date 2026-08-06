package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface AutoQaExecutionRepository extends MongoRepository<AutoQaExecutionDocument, String> {

    Optional<AutoQaExecutionDocument> findByExecutionId(UUID executionId);
}
