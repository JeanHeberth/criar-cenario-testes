package com.br.criarcenariotestes.business.autoqa.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para validação rápida do caminho do projeto
 * antes de iniciar o workflow completo.
 */
public record ProjectValidationRequest(

        @NotBlank(message = "O caminho do projeto é obrigatório")
        String projectPath

) {}
