package com.br.criarcenariotestes.business.dto;

public record JiraAttachmentResponse(
        String id,
        String fileName,
        String mimeType,
        long size,
        String downloadUrl
) {
}

