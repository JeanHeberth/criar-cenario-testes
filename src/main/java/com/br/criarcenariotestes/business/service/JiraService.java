package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.JiraAttachmentResponse;
import com.br.criarcenariotestes.business.dto.JiraIssueAttachmentsResponse;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class JiraService {

    private final JiraClient jiraClient;

    public JiraIssueAttachmentsResponse listarAnexos(String taskKey) {
        JsonNode issue = jiraClient.buscarIssueComAnexos(taskKey);
        List<JiraAttachmentResponse> attachments = mapearAnexos(taskKey, issue);
        return new JiraIssueAttachmentsResponse(taskKey, attachments);
    }

    public DownloadedAttachment baixarAnexo(String taskKey, String attachmentId) {
        JsonNode issue = jiraClient.buscarIssueComAnexos(taskKey);
        JsonNode attachment = buscarAnexoPorId(issue, attachmentId);

        String fileName = attachment.path("filename").asText("arquivo.bin");
        String mimeType = attachment.path("mimeType").asText("application/octet-stream");
        String contentUrl = attachment.path("content").asText();

        if (contentUrl.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "URL de conteudo do anexo nao encontrada no Jira");
        }

        byte[] fileBytes = jiraClient.baixarAnexo(contentUrl);
        return new DownloadedAttachment(fileName, mimeType, fileBytes);
    }

    private List<JiraAttachmentResponse> mapearAnexos(String taskKey, JsonNode issue) {
        JsonNode attachmentsNode = issue.path("fields").path("attachment");

        if (!attachmentsNode.isArray()) {
            return List.of();
        }

        List<JiraAttachmentResponse> mapped = new ArrayList<>();
        for (JsonNode attachment : attachmentsNode) {
            String id = attachment.path("id").asText();
            mapped.add(new JiraAttachmentResponse(
                    id,
                    attachment.path("filename").asText(),
                    attachment.path("mimeType").asText(),
                    attachment.path("size").asLong(0L),
                    "/jira/tasks/" + taskKey + "/attachments/" + id + "/download"
            ));
        }

        mapped.sort(Comparator.comparing(JiraAttachmentResponse::fileName, String.CASE_INSENSITIVE_ORDER));
        return mapped;
    }

    private JsonNode buscarAnexoPorId(JsonNode issue, String attachmentId) {
        JsonNode attachmentsNode = issue.path("fields").path("attachment");

        if (!attachmentsNode.isArray()) {
            throw new ResponseStatusException(NOT_FOUND, "Task sem anexos no Jira");
        }

        for (JsonNode attachment : attachmentsNode) {
            if (attachmentId.equals(attachment.path("id").asText())) {
                return attachment;
            }
        }

        throw new ResponseStatusException(NOT_FOUND, "Anexo nao encontrado na task informada");
    }

    public record DownloadedAttachment(
            String fileName,
            String mimeType,
            byte[] content
    ) {
    }
}

