package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import java.util.List;

public record AutoQaExecutionListResponse(List<AutoQaExecutionResponse> items, int page, int size, long totalElements) {
}
