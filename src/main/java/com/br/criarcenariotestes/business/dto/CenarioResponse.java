package com.br.criarcenariotestes.business.dto;

import java.util.List;

public record CenarioResponse(
        String id,
        String titulo,
        String regraDeNegocio,
        String criteriosAceitacao,
        List<String> cenarios
) {}
