package com.br.criarcenariotestes.business.dto;

public record CenarioResponse(
        String id,
        String titulo,
        String regraDeNegocio,
        String cenarioGerado
) {}
