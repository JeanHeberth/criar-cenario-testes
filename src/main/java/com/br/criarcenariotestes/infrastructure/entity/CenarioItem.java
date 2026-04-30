package com.br.criarcenariotestes.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenarioItem {

    private String nome;
    private String objetivo;
    private String precondicao;
    private String scriptTeste;
    private String resultadoEsperado;
    private String variaveis;
    private String componente;
    private String rotulos;
    private String proposito;
    private String pasta;
    private String proprietario;
    private String cobertura;
    private String status;

}