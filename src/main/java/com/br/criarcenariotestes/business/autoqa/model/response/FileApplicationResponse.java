package com.br.criarcenariotestes.business.autoqa.model.response;

public record FileApplicationResponse(
        boolean success,
        String backupId,
        int appliedCount,
        String message
) {
}
