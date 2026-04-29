package com.br.criarcenariotestes.infrastructure.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document("cenario")
public class Cenario {

    @Id
    private String id;

    private String titulo;
    private String regraDeNegocio;
    private String criteriosAceitacao;
    private List<CenarioItem> cenarios;
}