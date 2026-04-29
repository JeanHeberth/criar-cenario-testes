package com.br.criarcenariotestes.business.dto;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;

import java.util.List;

public record CenarioResponse(
        String id,
        String titulo,
        String regraDeNegocio,
        String criteriosAceitacao,
        List<CenarioItem> cenarios
) {}