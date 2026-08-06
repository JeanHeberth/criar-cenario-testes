package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import jakarta.validation.constraints.NotBlank;

public record AutoQaCreateExecutionRequest(
        @NotBlank String scenario,
        @NotBlank String projectPath
) {
}
