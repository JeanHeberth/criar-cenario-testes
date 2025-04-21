package com.br.criarcenariotestes.infrastructure.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cenario")
@Getter
@Setter
@Data
public class Cenario {

    @Id
    private String id;
    private String titulo;
    private String regraDeNegocio;
    private String cenarioGerado;
}
