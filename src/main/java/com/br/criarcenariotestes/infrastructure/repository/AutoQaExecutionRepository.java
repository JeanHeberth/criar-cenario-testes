package com.br.criarcenariotestes.infrastructure.repository;

import com.br.criarcenariotestes.infrastructure.entity.AutoQaExecutionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AutoQaExecutionRepository
        extends MongoRepository<AutoQaExecutionDocument, String> {

    Optional<AutoQaExecutionDocument> findByExecutionId(String executionId);
}
