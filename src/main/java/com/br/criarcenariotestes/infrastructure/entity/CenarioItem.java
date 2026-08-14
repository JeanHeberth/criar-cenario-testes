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

    /**
     * FASE15-BUG-005B: rastreabilidade de evidência. Valores conceituais de
     * evidenceType: DOCUMENTED / DIRECT_INFERENCE / EXPLORATORY.
     * evidenceSources é texto livre (IDs separados por vírgula, ex.:
     * "RN-A-02, RN-B-01", ou "USER" quando a fonte é a regra digitada, ou
     * vazio/"Não se aplica" quando EXPLORATORY). Cenários legados (antes
     * desta sessão) não possuem esses campos — desserializam como null.
     */
    private String evidenceType;
    private String evidenceSources;

}