package com.br.criarcenariotestes.infrastructure.repository;

import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CenarioRepository extends MongoRepository<Cenario, String> {
}
