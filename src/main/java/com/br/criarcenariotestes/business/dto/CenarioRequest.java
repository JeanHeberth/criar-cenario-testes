package com.br.criarcenariotestes.business.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public record CenarioRequest(
        String titulo,
        String regraDeNegocio
) {}
