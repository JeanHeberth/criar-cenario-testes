package com.br.criarcenariotestes.business.dto;

import java.util.List;

public record JiraIssueAttachmentsResponse(
        String taskKey,
        List<JiraAttachmentResponse> attachments
) {
}

